package pl.umcs;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ConversionLibraryBoundaryTest {

	@Test
	void the_conversion_library_depends_on_no_framework() {
		classes().that().resideInAnyPackage("pl.umcs.domain..", "pl.umcs.api..")
				.should().onlyDependOnClassesThat().resideInAnyPackage("java..", "pl.umcs.domain..", "pl.umcs.api..", "lombok..")
				.check(new ClassFileImporter()
						.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
						.importPackages("pl.umcs.domain", "pl.umcs.api"));
	}
}
