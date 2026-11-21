package pl.umcs.auth.provider;

public interface JwksSource {

	JwkSet fetch();
}
