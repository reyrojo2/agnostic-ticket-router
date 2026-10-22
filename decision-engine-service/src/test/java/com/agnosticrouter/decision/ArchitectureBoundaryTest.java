package com.agnosticrouter.decision;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Structural guardrails: application and domain must not depend on adapters. */
class ArchitectureBoundaryTest {
    private static final String ROOT = "com.agnosticrouter.decision";

    @Test
    void controllersMustNotReadRepositoriesDirectly() {
        var classes = new ClassFileImporter().importPackages(ROOT);
        noClasses().that().resideInAPackage(ROOT + ".api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".infrastructure.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void applicationMustNotImportAdaptersOrTransportControllers() {
        var classes = new ClassFileImporter().importPackages(ROOT);
        noClasses().that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".infrastructure..", ROOT + ".api..", ROOT + ".config..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domainMustNotDependOnApplicationOrInfrastructure() {
        var classes = new ClassFileImporter().importPackages(ROOT);
        noClasses().that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".application..", ROOT + ".infrastructure..", ROOT + ".api..", ROOT + ".config..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void modulesMustNotImportOtherServicesInternalClasses() {
        var classes = new ClassFileImporter().importPackages(ROOT);
        noClasses().that().resideInAPackage(ROOT + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.agnosticrouter.knowledge..", "com.agnosticrouter.crm..", "com.agnosticrouter.gmail..", "com.agnosticrouter.ingestion..", "com.agnosticrouter.notification..", "com.agnosticrouter.observability..")
                .allowEmptyShould(true)
                .because("Integration across services is only through common-contracts and HTTP/RabbitMQ")
                .check(classes);
    }
}
