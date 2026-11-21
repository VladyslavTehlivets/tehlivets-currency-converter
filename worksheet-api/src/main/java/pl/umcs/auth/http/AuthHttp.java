package pl.umcs.auth.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRouting;
import pl.umcs.auth.AuthSettings;
import pl.umcs.auth.application.SignInNonces;
import pl.umcs.auth.application.SignInService;
import pl.umcs.auth.application.Sessions;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.NextPath;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.Session;
import pl.umcs.auth.domain.SignInRefused;
import pl.umcs.auth.domain.StateCookie;
import pl.umcs.auth.provider.GoogleTokenEndpoint;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public final class AuthHttp {

	public static final String SESSION_COOKIE = "__Host-session";
	public static final String BINDER_COOKIE = "__Host-sign-in-binder";
	public static final String GOOGLE_DOOR = "google";
	public static final String SIGN_IN_PATH = "/sign-in";

	private static final String GOOGLE_AUTHORIZATION = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final long MAX_BODY_BYTES = 8192;

	record Nonce(String nonce) {
	}

	record SessionIssued(String sessionId) {
	}

	record SessionRequest(String nonce, String assertion) {
	}

	record TestDoorRequest(String email, String password) {
	}

	record BinderPayload(String state, String nonce, String verifier, String next, long issuedAt) {
	}

	private final SignInService signIn;
	private final Sessions sessions;
	private final SignInNonces nonces;
	private final GoogleTokenEndpoint tokenEndpoint;
	private final AuthSettings settings;
	private final Clock clock;

	public AuthHttp(SignInService signIn, Sessions sessions, SignInNonces nonces, GoogleTokenEndpoint tokenEndpoint, AuthSettings settings, Clock clock) {
		this.signIn = signIn;
		this.sessions = sessions;
		this.nonces = nonces;
		this.tokenEndpoint = tokenEndpoint;
		this.settings = settings;
		this.clock = clock;
	}

	public void register(JavalinDefaultRouting routing) {
		routing.get("/auth/google/start", this::startGoogle);
		routing.get("/auth/google/callback", this::finishGoogle);
		routing.post("/v1/sessions/nonces", this::issueNonce);
		routing.post("/v1/sessions", this::createSession);
		if (settings.testDoor().isPresent()) {
			routing.post("/auth/test-door", this::signInThroughTestDoor);
		}
		routing.exception(SignInRefused.class, (refused, ctx) -> refuseNative(ctx, refused));
	}

	public boolean hasLiveSession(Context ctx) {
		return cookie(ctx, SESSION_COOKIE).flatMap(AuthHttp::asUuid).flatMap(id -> sessions.memberOf(id, clock.instant())).isPresent();
	}

	public void redirectSignedInVisitor(Context ctx) {
		String next = NextPath.parse(ctx.queryParam("next")).orElse(NextPath.home()).value();
		ctx.redirect(next);
	}

	private void startGoogle(Context ctx) {
		Optional<NextPath> next = ctx.queryParam("next") == null ? Optional.of(NextPath.home()) : NextPath.parse(ctx.queryParam("next"));
		if (next.isEmpty()) {
			ctx.status(400).json(new ErrorBody(new ErrorBody.Error("invalid_next", "next")));
			return;
		}
		if (settings.googleClientIds().isEmpty()) {
			ctx.redirect(refusalLocation(RefusalCode.PROVIDER_UNAVAILABLE));
			return;
		}
		StateCookie binder = new StateCookie(randomToken(), randomToken(), randomToken() + randomToken(), next.get(), clock.instant());
		ctx.res().addHeader("Set-Cookie", binderCookie(encode(binder), StateCookie.LIFETIME.toSeconds()));
		ctx.header("Cache-Control", "no-store");
		ctx.redirect(authorizationUrl(binder));
	}

	private void finishGoogle(Context ctx) {
		Optional<StateCookie> binder = cookie(ctx, BINDER_COOKIE).flatMap(AuthHttp::decode);
		ctx.res().addHeader("Set-Cookie", binderCookie("", 0));
		ctx.header("Cache-Control", "no-store");
		try {
			StateCookie bound = binder.filter(cookie -> cookie.isFresh(clock.instant())).filter(cookie -> cookie.answers(ctx.queryParam("state")))
					.orElseThrow(() -> new SignInRefused(RefusalCode.STATE_MISMATCH));
			if (ctx.queryParam("error") != null) {
				throw new SignInRefused("access_denied".equals(ctx.queryParam("error")) ? RefusalCode.CANCELLED : RefusalCode.PROVIDER_UNAVAILABLE);
			}
			String code = ctx.queryParam("code");
			if (code == null || code.isBlank()) {
				throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
			}
			String idToken = tokenEndpoint.idTokenFor(code, bound.verifier(), settings.callbackUrl());
			Session session = signIn.signIn(new Assertion.GoogleIdToken(idToken, bound.nonce(), Assertion.Origin.TOKEN_ENDPOINT));
			ctx.res().addHeader("Set-Cookie", sessionCookie(session));
			ctx.redirect(bound.next().value());
		} catch (SignInRefused refused) {
			ctx.redirect(refusalLocation(refused.code()));
		}
	}

	private void issueNonce(Context ctx) {
		ctx.status(201).header("Cache-Control", "no-store").json(new Nonce(nonces.issue(clock.instant())));
	}

	private void createSession(Context ctx) {
		SessionRequest request = readBody(ctx, SessionRequest.class);
		if (request.nonce() == null || request.assertion() == null || request.nonce().isBlank() || request.assertion().isBlank()) {
			ctx.status(400).json(ErrorBody.of("invalid_request"));
			return;
		}
		if (!nonces.consume(request.nonce(), clock.instant())) {
			throw new SignInRefused(RefusalCode.STATE_MISMATCH);
		}
		Session session = signIn.signIn(new Assertion.GoogleIdToken(request.assertion(), request.nonce(), Assertion.Origin.CLIENT));
		ctx.res().addHeader("Set-Cookie", sessionCookie(session));
		ctx.status(201).header("Cache-Control", "no-store").json(new SessionIssued(session.id().toString()));
	}

	private void signInThroughTestDoor(Context ctx) {
		TestDoorRequest request = readBody(ctx, TestDoorRequest.class);
		Session session = signIn.signIn(new Assertion.TestPassword(request.email(), request.password()));
		ctx.res().addHeader("Set-Cookie", sessionCookie(session));
		ctx.status(204).header("Cache-Control", "no-store");
	}

	private static void refuseNative(Context ctx, SignInRefused refused) {
		int status = switch (refused.code()) {
			case STATE_MISMATCH, CANCELLED -> 400;
			case PROVIDER_UNAVAILABLE -> 401;
			case TOKEN_FOR_ANOTHER_CLIENT, EMAIL_UNVERIFIED, NOT_INVITED -> 403;
		};
		ctx.status(status).header("Cache-Control", "no-store").json(ErrorBody.of(refused.code().wire()));
	}

	private static <T> T readBody(Context ctx, Class<T> type) {
		try {
			byte[] body = ctx.bodyInputStream().readNBytes((int) MAX_BODY_BYTES + 1);
			if (body.length > MAX_BODY_BYTES) {
				throw new io.javalin.http.HttpResponseException(413, "body too large");
			}
			return JSON.readValue(body, type);
		} catch (java.io.IOException unreadable) {
			throw new io.javalin.http.BadRequestResponse("invalid body");
		}
	}

	private String authorizationUrl(StateCookie binder) {
		return GOOGLE_AUTHORIZATION
				+ "?client_id=" + encodeUrl(settings.googleClientIds().iterator().next())
				+ "&redirect_uri=" + encodeUrl(settings.callbackUrl())
				+ "&response_type=code"
				+ "&scope=" + encodeUrl("openid email")
				+ "&state=" + binder.state()
				+ "&nonce=" + binder.nonce()
				+ "&code_challenge=" + challengeFor(binder.verifier())
				+ "&code_challenge_method=S256";
	}

	static String challengeFor(String verifier) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(missing);
		}
	}

	static String refusalLocation(RefusalCode code) {
		return SIGN_IN_PATH + "?refused=" + code.wire() + "&door=" + GOOGLE_DOOR;
	}

	static String binderCookie(String value, long maxAgeSeconds) {
		return BINDER_COOKIE + "=" + value + "; Path=/; Max-Age=" + maxAgeSeconds + "; HttpOnly; Secure; SameSite=Lax";
	}

	static String sessionCookie(Session session) {
		long maxAge = session.expiresAt().getEpochSecond() - session.issuedAt().getEpochSecond();
		return SESSION_COOKIE + "=" + session.id() + "; Path=/; Max-Age=" + maxAge + "; HttpOnly; Secure; SameSite=Lax";
	}

	private static String encode(StateCookie binder) {
		try {
			byte[] json = JSON.writeValueAsBytes(new BinderPayload(binder.state(), binder.nonce(), binder.verifier(), binder.next().value(), binder.issuedAt().getEpochSecond()));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
		} catch (java.io.IOException unwritable) {
			throw new IllegalStateException(unwritable);
		}
	}

	private static Optional<StateCookie> decode(String written) {
		try {
			BinderPayload payload = JSON.readValue(Base64.getUrlDecoder().decode(written), BinderPayload.class);
			return NextPath.parse(payload.next()).map(next -> new StateCookie(payload.state(), payload.nonce(), payload.verifier(), next, Instant.ofEpochSecond(payload.issuedAt())));
		} catch (java.io.IOException | IllegalArgumentException unreadable) {
			return Optional.empty();
		}
	}

	private static Optional<String> cookie(Context ctx, String name) {
		String header = ctx.header("Cookie");
		if (header == null) {
			return Optional.empty();
		}
		for (String pair : header.split(";")) {
			String trimmed = pair.strip();
			if (trimmed.startsWith(name + "=")) {
				return Optional.of(trimmed.substring(name.length() + 1)).filter(value -> !value.isEmpty());
			}
		}
		return Optional.empty();
	}

	private static Optional<UUID> asUuid(String written) {
		try {
			return Optional.of(UUID.fromString(written));
		} catch (IllegalArgumentException notAnId) {
			return Optional.empty();
		}
	}

	private static String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String encodeUrl(String raw) {
		return URLEncoder.encode(raw, StandardCharsets.UTF_8);
	}

	record ErrorBody(Error error) {
		@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
		record Error(String code, String field) {
		}

		static ErrorBody of(String code) {
			return new ErrorBody(new Error(code, null));
		}
	}
}
