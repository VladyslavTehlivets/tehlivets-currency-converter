package pl.umcs.auth.domain;

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
	void the_sign_in_service_reaches_providers_only_through_ports() {
		noClasses().that().resideInAPackage("pl.umcs.auth.application..")
				.should().dependOnClassesThat().resideInAnyPackage("pl.umcs.auth.provider..", "pl.umcs.auth.persistence..", "pl.umcs.auth.http..")
				.check(AUTH_CLASSES);
	}
}
