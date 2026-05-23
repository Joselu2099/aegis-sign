package com.aegis.sign;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.aegis.sign", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchUnitTest {

    @ArchTest
    static final ArchRule hexagonal_architecture_is_respected = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("com.aegis.sign.domain..")
            .layer("Application").definedBy("com.aegis.sign.application..")
            .layer("Infrastructure").definedBy("com.aegis.sign.infrastructure..")

            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();

}
