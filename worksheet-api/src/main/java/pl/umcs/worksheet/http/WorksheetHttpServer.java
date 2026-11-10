package pl.umcs.worksheet.http;

import io.javalin.Javalin;
import io.javalin.http.ContentTooLargeResponse;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.umcs.worksheet.application.ConvertOnWorksheet;
import pl.umcs.worksheet.application.KeepRate;
import pl.umcs.worksheet.application.ListRates;
import pl.umcs.worksheet.application.OpenWorksheet;
import pl.umcs.worksheet.application.RateMissing;
import pl.umcs.worksheet.application.RemoveRate;
import pl.umcs.worksheet.application.StartWorksheet;
import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.RateId;
import pl.umcs.worksheet.domain.RateValue;
import pl.umcs.worksheet.domain.RatedAt;
import pl.umcs.worksheet.domain.Worksheet;
import pl.umcs.worksheet.domain.WorksheetId;
import pl.umcs.worksheet.domain.WorksheetKey;
import pl.umcs.worksheet.domain.WorksheetRefusal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class WorksheetHttpServer {

	public record UseCases(
			StartWorksheet startWorksheet,
			OpenWorksheet openWorksheet,
			ListRates listRates,
			KeepRate keepRate,
			RemoveRate removeRate,
			ConvertOnWorksheet convertOnWorksheet) {
	}

	private static final Logger LOG = LoggerFactory.getLogger(WorksheetHttpServer.class);
	private static final long MAX_BODY_BYTES = 4096;
	private static final String BEARER = "Bearer ";

	private final UseCases useCases;
	private final String revision;

	private WorksheetHttpServer(UseCases useCases, String revision) {
		this.useCases = useCases;
		this.revision = revision;
	}

	public static Javalin create(UseCases useCases, String revision, Path webExportDir) {
		WorksheetHttpServer server = new WorksheetHttpServer(useCases, revision);
		return Javalin.create(config -> {
			config.showJavalinBanner = false;
			config.http.maxRequestSize = MAX_BODY_BYTES;
			config.router.mount(routing -> {
				server.registerApi(routing);
				if (Files.isDirectory(webExportDir)) {
					WebExport webExport = new WebExport(webExportDir);
					routing.get("/", webExport::serve);
					routing.get("/*", ctx -> {
						if (isApiPath(ctx)) {
							refuse(ctx, 404, "not_found", null);
						} else {
							webExport.serve(ctx);
						}
					});
				}
			});
		});
	}

	private void registerApi(JavalinDefaultRouting routing) {
		routing.before(ctx -> ctx.header("X-Content-Type-Options", "nosniff"));
		routing.get("/v1/health", ctx -> ctx.json(new ResponseBodies.Health("ok", revision)));
		routing.post("/v1/worksheets", this::startWorksheet);
		routing.get("/v1/worksheet/rates", this::listRates);
		routing.post("/v1/worksheet/rates", this::keepRate);
		routing.delete("/v1/worksheet/rates/{id}", this::removeRate);
		routing.post("/v1/worksheet/conversions", this::convert);

		routing.exception(FieldRefusal.class, (refusal, ctx) -> refuse(ctx, 400, refusal.code(), refusal.field()));
		routing.exception(WorksheetRefusal.class, WorksheetHttpServer::refuseByWorksheet);
		routing.exception(OpenWorksheet.UnknownWorksheetKey.class, (unknown, ctx) -> refuse(ctx, 401, "unauthorized", null));
		routing.exception(RateMissing.class, WorksheetHttpServer::refuseMissingRate);
		routing.exception(ContentTooLargeResponse.class, (tooLarge, ctx) -> refuse(ctx, 413, "body_too_large", null));
		routing.exception(Exception.class, (failure, ctx) -> {
			LOG.error("request failed on {} {}", ctx.method(), ctx.path(), failure);
			refuse(ctx, 500, "internal", null);
		});
		routing.error(404, ctx -> {
			if (ctx.resultInputStream() == null) {
				refuse(ctx, 404, "not_found", null);
			}
		});
	}

	private void startWorksheet(Context ctx) {
		WorksheetKey key = useCases.startWorksheet().start();
		ctx.status(201).header("Cache-Control", "no-store").json(new ResponseBodies.StartedWorksheet(key.reveal()));
	}

	private void listRates(Context ctx) {
		WorksheetId worksheet = openedWorksheet(ctx);
		var rates = useCases.listRates().list(worksheet).stream().map(ResponseBodies.Rate::of).toList();
		ctx.json(new ResponseBodies.Rates(rates, Worksheet.RATE_LIMIT));
	}

	private void keepRate(Context ctx) {
		WorksheetId worksheet = openedWorksheet(ctx);
		RequestFields fields = RequestFields.of(ctx.body());
		var from = fields.currency("from");
		var to = fields.currency("to");
		var rate = fields.positiveDecimal("rate", "invalid_rate");
		var ratedAt = fields.moment("ratedAt");
		DatedRate kept = useCases.keepRate().keep(worksheet, new Direction(from, to), new RateValue(rate), new RatedAt(ratedAt));
		ctx.status(201).json(ResponseBodies.Rate.of(kept));
	}

	private void removeRate(Context ctx) {
		WorksheetId worksheet = openedWorksheet(ctx);
		useCases.removeRate().remove(worksheet, rateIdOf(ctx.pathParam("id")));
		ctx.status(204);
	}

	private void convert(Context ctx) {
		WorksheetId worksheet = openedWorksheet(ctx);
		RequestFields fields = RequestFields.of(ctx.body());
		var amount = fields.positiveDecimal("amount", "invalid_amount");
		var from = fields.currency("from");
		var to = fields.currency("to");
		var at = fields.moment("at");
		ctx.json(useCases.convertOnWorksheet().convert(worksheet, amount, new Direction(from, to), at));
	}

	private WorksheetId openedWorksheet(Context ctx) {
		String authorization = ctx.header("Authorization");
		if (authorization == null || !authorization.startsWith(BEARER) || authorization.length() == BEARER.length()) {
			throw new OpenWorksheet.UnknownWorksheetKey();
		}
		return useCases.openWorksheet().open(WorksheetKey.presented(authorization.substring(BEARER.length())));
	}

	private static RateId rateIdOf(String pathParam) {
		try {
			return new RateId(UUID.fromString(pathParam));
		} catch (IllegalArgumentException notARateId) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.RATE_NOT_FOUND);
		}
	}

	private static void refuseByWorksheet(WorksheetRefusal refusal, Context ctx) {
		String code = refusal.reason().name().toLowerCase();
		switch (refusal.reason()) {
			case SAME_CURRENCY -> refuse(ctx, 400, code, "to");
			case INVALID_RATE -> refuse(ctx, 400, code, "rate");
			case INVALID_MOMENT -> refuse(ctx, 400, code, "ratedAt");
			case RATE_EXISTS -> refuse(ctx, 409, code, null);
			case WORKSHEET_FULL -> refuse(ctx, 422, code, null);
			case RATE_NOT_FOUND -> refuse(ctx, 404, code, null);
		}
	}

	private static void refuseMissingRate(RateMissing missing, Context ctx) {
		var direction = new ResponseBodies.MissingDirection(missing.from().getCurrencyCode(), missing.to().getCurrencyCode());
		ctx.status(422).json(new ResponseBodies.MissingRateRefusal(new ResponseBodies.MissingRate("rate_missing", direction, missing.at().toString())));
	}

	private static void refuse(Context ctx, int status, String code, String field) {
		ctx.status(status).json(ResponseBodies.Refusal.of(code, field));
	}

	private static boolean isApiPath(Context ctx) {
		return ctx.path().equals("/v1") || ctx.path().startsWith("/v1/");
	}
}
