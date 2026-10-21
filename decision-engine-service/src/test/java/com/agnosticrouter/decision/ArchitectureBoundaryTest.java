package com.agnosticrouter.decision;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Prevents a future refactoring from bypassing the local application boundary. */
class ArchitectureBoundaryTest {
    @Test
    void httpControllersMustNotDependDirectlyOnRepositories() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.decision");
        noClasses().that().resideInAPackage("com.agnosticrouter.decision.api..")
                .should().dependOnClassesThat().resideInAPackage("com.agnosticrouter.decision.repo..")
                .check(classes);
    }

    @Test
    void applicationServicesMustNotImportTransportControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.decision");
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("com.agnosticrouter.decision.api..")
                .check(classes);
    }

    @Test
    void domainMustNotImportRabbitOrHttpControllers() {
        var classes = new ClassFileImporter().importPackages("com.agnosticrouter.decision");
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.amqp..", "org.springframework.web..")
                .check(classes);
    }
}
