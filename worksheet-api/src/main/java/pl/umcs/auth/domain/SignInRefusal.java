package pl.umcs.auth.domain;

public final class SignInRefusal extends RuntimeException {

	public enum Code {
		CANCELLED("cancelled"),
		STATE_MISMATCH("state-mismatch"),
		TOKEN_FOR_ANOTHER_CLIENT("token-for-another-client"),
		EMAIL_UNVERIFIED("email-unverified"),
		NOT_INVITED("not-invited"),
		PROVIDER_UNAVAILABLE("provider-unavailable");

		private final String wire;

		Code(String wire) {
			this.wire = wire;
		}

		public String wire() {
			return wire;
		}
	}

	private final Code code;

	public SignInRefusal(Code code) {
		super(code.wire(), null, false, false);
		this.code = code;
	}

	public Code code() {
		return code;
	}
}
