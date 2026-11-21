package pl.umcs.auth.domain;

public final class SignInRefused extends RuntimeException {

	private final RefusalCode code;

	public SignInRefused(RefusalCode code) {
		super(code.wire(), null, false, false);
		this.code = code;
	}

	public RefusalCode code() {
		return code;
	}
}
