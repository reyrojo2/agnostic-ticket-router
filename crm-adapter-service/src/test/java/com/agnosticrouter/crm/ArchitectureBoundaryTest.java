package com.agnosticrouter.crm;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Prevents a future refactoring from bypassing the local application boundary. */
class ArchitectureBoundaryTest {
    @Test
    void httpControllersMustNotDependDirectlyOnRepositories() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.crm");
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..repo..")
                .check(classes);
    }

    @Test
    void applicationServicesMustNotImportTransportControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.crm");
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .check(classes);
    }

    @Test
    void domainMustNotImportRabbitOrHttpControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.crm");
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.amqp..", "org.springframework.web..")
                .check(classes);
    }
}
