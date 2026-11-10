package pl.umcs.worksheet.http;

final class FieldRefusal extends RuntimeException {

	private final String code;
	private final String field;

	FieldRefusal(String code, String field) {
		super(code, null, false, false);
		this.code = code;
		this.field = field;
	}

	String code() {
		return code;
	}

	String field() {
		return field;
	}
}
