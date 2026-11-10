package pl.umcs.worksheet;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import pl.umcs.worksheet.application.ConvertOnWorksheet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class WorksheetBoundaryTest {

	private static final JavaClasses WORKSHEET_CLASSES = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("pl.umcs.worksheet");

	@Test
	void the_worksheet_domain_imports_no_http_or_sql() {
		noClasses().that().resideInAPackage("pl.umcs.worksheet.domain..")
				.should().dependOnClassesThat().resideInAnyPackage("io.javalin..", "com.fasterxml.jackson..", "java.sql..", "javax.sql..")
				.check(WORKSHEET_CLASSES);
	}

	@Test
	void only_the_convert_use_case_reaches_the_library_converter() {
		noClasses().that().doNotHaveFullyQualifiedName(ConvertOnWorksheet.class.getName())
				.should().dependOnClassesThat().resideInAPackage("pl.umcs.api.converter..")
				.check(WORKSHEET_CLASSES);
	}
}
