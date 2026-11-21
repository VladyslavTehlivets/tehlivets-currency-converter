package pl.umcs.auth.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.javalin.router.JavalinDefaultRouting;
import pl.umcs.auth.application.Sessions;
import pl.umcs.auth.application.SignInService;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.NextPath;
import pl.umcs.auth.domain.Session;
import pl.umcs.auth.domain.SignInRefusal;
import pl.umcs.auth.domain.SignInRefusal.Code;
import pl.umcs.auth.domain.StateCookie;
import pl.umcs.auth.google.GoogleDoor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SignInRoutes {

	public record TestDoor(String email, String password) {
		boolean isOpen() {
			return email != null && !email.isBlank() && password != null && !password.isBlank();
		}
	}

	public static final String SESSION_COOKIE = "__Host-session";
	public static final String BINDER_COOKIE = "__Host-sign-in";
	private static final Duration NONCE_LIFETIME = Duration.ofMinutes(5);
	private static final ObjectMapper JSON = new ObjectMapper();

	private final SignInService signIn;
	private final Sessions sessions;
	private final GoogleDoor google;
	private final TestDoor testDoor;
	private final Clock clock;

	public SignInRoutes(SignInService signIn, Sessions sessions, GoogleDoor google, TestDoor testDoor, Clock clock) {
		this.signIn = signIn;
		this.sessions = sessions;
		this.google = google;
		this.testDoor = testDoor;
		this.clock = clock;
	}

	public void register(JavalinDefaultRouting routing) {
		routing.before("/sign-in", this::returnToNextWhenSignedIn);
		routing.post("/auth/google/start", this::startGoogle);
		routing.get("/auth/google/callback", this::finishGoogle);
		routing.get("/auth/test/door", ctx -> ctx.status(testDoor.isOpen() ? 200 : 404).header("Cache-Control", "no-store"));
		routing.post("/auth/test/sign-in", this::signInAtTheTestDoor);
		routing.post("/v1/sessions/nonce", this::issueNonce);
		routing.post("/v1/sessions", this::createSession);
	}

	private void returnToNextWhenSignedIn(Context ctx) {
		if (ctx.method().name().equals("GET") && liveSessionOf(ctx).isPresent()) {
			ctx.redirect(NextPath.of(ctx.queryParam("next")).value(), io.javalin.http.HttpStatus.SEE_OTHER);
		}
	}

	private void startGoogle(Context ctx) {
		NextPath next = NextPath.of(ctx.formParam("next"));
		if (!google.isOpen()) {
			refuseOnThePage(ctx, Code.PROVIDER_UNAVAILABLE, next);
			return;
		}
		StateCookie binder = StateCookie.begin(next, clock.instant());
		ctx.cookie(binderCookie(binder.encode(), (int) StateCookie.LIFETIME.toSeconds()));
		ctx.header("Cache-Control", "no-store");
		ctx.redirect(google.authorizationRequest(binder).toString(), io.javalin.http.HttpStatus.SEE_OTHER);
	}

	private void finishGoogle(Context ctx) {
		Optional<StateCookie> binder = Optional.ofNullable(ctx.cookie(BINDER_COOKIE)).flatMap(StateCookie::decode);
		ctx.cookie(binderCookie("", 0));
		ctx.header("Cache-Control", "no-store");
		NextPath next = binder.map(found -> NextPath.of(found.nextPath())).orElse(NextPath.HOME);
		try {
			StateCookie bound = binder.filter(found -> found.returnsWith(String.valueOf(ctx.queryParam("state")), clock.instant()))
					.orElseThrow(() -> new SignInRefusal(Code.STATE_MISMATCH));
			String error = ctx.queryParam("error");
			if (error != null) {
				throw new SignInRefusal("access_denied".equals(error) ? Code.CANCELLED : Code.PROVIDER_UNAVAILABLE);
			}
			String code = ctx.queryParam("code");
			if (code == null || code.isBlank()) {
				throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
			}
			Session session = signIn.signIn(google.exchange(code, bound));
			ctx.cookie(sessionCookie(session));
			ctx.redirect(next.value(), io.javalin.http.HttpStatus.SEE_OTHER);
		} catch (SignInRefusal refusal) {
			refuseOnThePage(ctx, refusal.code(), next);
		}
	}

	private void signInAtTheTestDoor(Context ctx) {
		NextPath next = NextPath.of(ctx.formParam("next"));
		String presented = Optional.ofNullable(ctx.formParam("password")).orElse("");
		boolean matches = testDoor.isOpen()
				&& MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), testDoor.password().getBytes(StandardCharsets.UTF_8))
				&& testDoor.email().equals(ctx.formParam("email"));
		if (!matches) {
			if (!testDoor.isOpen()) {
				ctx.status(404);
				return;
			}
			refuseOnThePage(ctx, Code.NOT_INVITED, next);
			return;
		}
		ctx.cookie(sessionCookie(signIn.signIn(new Assertion.TestDoorAssertion(testDoor.email()))));
		ctx.redirect(next.value(), io.javalin.http.HttpStatus.SEE_OTHER);
	}

	private void issueNonce(Context ctx) {
		String nonce = StateCookie.randomToken();
		sessions.issueNonce(nonce, clock.instant().plus(NONCE_LIFETIME));
		ctx.status(201).header("Cache-Control", "no-store").json(Map.of("nonce", nonce));
	}

	private void createSession(Context ctx) throws java.io.IOException {
		JsonNode body = JSON.readTree(ctx.bodyInputStream().readNBytes(8192));
		String nonce = body.path("nonce").asText("");
		String idToken = body.path("idToken").asText("");
		try {
			if (!"google".equals(body.path("provider").asText())) {
				throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
			}
			if (!sessions.consumeNonce(nonce, clock.instant())) {
				throw new SignInRefusal(Code.STATE_MISMATCH);
			}
			Session session = signIn.signIn(google.idTokens().verifyFromClient(idToken, nonce));
			ctx.cookie(sessionCookie(session));
			ctx.status(201).header("Cache-Control", "no-store").json(Map.of("session", Map.of("id", session.id().toString(), "expiresAt", session.expiresAt().toString())));
		} catch (SignInRefusal refusal) {
			ctx.status(401).json(Map.of("error", Map.of("code", refusal.code().wire())));
		}
	}

	public Optional<Session> liveSessionOf(Context ctx) {
		try {
			String cookie = ctx.cookie(SESSION_COOKIE);
			return cookie == null ? Optional.empty() : sessions.find(UUID.fromString(cookie)).filter(found -> found.isLiveAt(Instant.now(clock)));
		} catch (IllegalArgumentException malformed) {
			return Optional.empty();
		}
	}

	private static void refuseOnThePage(Context ctx, Code code, NextPath next) {
		String target = "/sign-in?refused=" + code.wire() + "&door=google"
				+ (next.equals(NextPath.HOME) ? "" : "&next=" + URLEncoder.encode(next.value(), StandardCharsets.UTF_8));
		ctx.redirect(target, io.javalin.http.HttpStatus.SEE_OTHER);
	}

	private static Cookie binderCookie(String value, int maxAgeSeconds) {
		Cookie cookie = new Cookie(BINDER_COOKIE, value, "/", maxAgeSeconds, true, 0, true, null, null, SameSite.LAX);
		return cookie;
	}

	private static Cookie sessionCookie(Session session) {
		return new Cookie(SESSION_COOKIE, session.id().toString(), "/", (int) Session.LIFETIME.toSeconds(), true, 0, true, null, null, SameSite.LAX);
	}
}
