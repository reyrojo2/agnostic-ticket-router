package com.agnosticrouter.gmail;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Prevents a future refactoring from bypassing the local application boundary. */
class ArchitectureBoundaryTest {
    @Test
    void httpControllersMustNotDependDirectlyOnRepositories() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.gmail");
        noClasses().that().resideInAPackage("com.agnosticrouter.gmail.api..")
                .should().dependOnClassesThat().resideInAPackage("com.agnosticrouter.gmail.repo..")
                .check(classes);
    }

    @Test
    void applicationServicesMustNotImportTransportControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.gmail");
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("com.agnosticrouter.gmail.api..")
                .check(classes);
    }

    @Test
    void domainMustNotImportRabbitOrHttpControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.gmail");
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.amqp..", "org.springframework.web..")
                .check(classes);
    }
}
