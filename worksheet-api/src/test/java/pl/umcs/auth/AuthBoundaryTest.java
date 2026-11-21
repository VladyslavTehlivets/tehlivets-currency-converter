package pl.umcs.auth;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AuthBoundaryTest {

	private static final JavaClasses AUTH_CLASSES = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("pl.umcs.auth");

	@Test
	void the_auth_domain_imports_no_http_json_or_sql() {
		noClasses().that().resideInAPackage("pl.umcs.auth.domain..")
				.should().dependOnClassesThat().resideInAnyPackage("io.javalin..", "com.fasterxml.jackson..", "java.sql..", "javax.sql..", "java.net.http..")
				.check(AUTH_CLASSES);
	}

	@Test
	void only_the_sign_in_service_decides_who_joins_a_member() {
		noClasses().that().resideInAnyPackage("pl.umcs.auth.http..", "pl.umcs.auth.google..")
				.should().dependOnClassesThat().resideInAPackage("pl.umcs.auth.persistence..")
				.check(AUTH_CLASSES);
	}
}
