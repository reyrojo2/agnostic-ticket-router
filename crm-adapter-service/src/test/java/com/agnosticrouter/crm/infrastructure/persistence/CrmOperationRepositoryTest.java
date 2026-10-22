package com.agnosticrouter.crm.infrastructure.persistence;

import com.agnosticrouter.crm.application.port.CrmOperationStore;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=PUBLIC"
})
// A test transaction would hide the closed-session failure from the HTTP endpoints.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CrmOperationRepositoryTest {
    @Autowired CrmIntegrationRepository integrations;
    @Autowired CrmOperationRepository operations;

    @Test
    void integrationRemainsReadableAfterRepositoryTransactionCloses() {
        CrmIntegration integration = new CrmIntegration();
        integration.setCode("ESPOCRM_TEST");
        integration.setName("Test CRM");
        integration.setBaseUrl("http://crm.test");
        integration.setSecretRef("ENV:TEST_KEY");
        integration = integrations.saveAndFlush(integration);

        CrmOperation operation = new CrmOperation();
        operation.setIntegration(integration);
        operation.setCode("CREATE_CASE");
        operation.setPathTemplate("/api/v1/Case");
        operation = operations.saveAndFlush(operation);
        try {
            CrmOperationStore store = operations;
            CrmOperation detached = store.findById(operation.getId()).orElseThrow();
            assertThat(detached.getIntegration().getCode()).isEqualTo("ESPOCRM_TEST");
            assertThat(detached.getIntegration().getBaseUrl()).isEqualTo("http://crm.test");
            assertThat(detached.getIntegration().getSecretRef()).isEqualTo("ENV:TEST_KEY");
        } finally {
            operations.delete(operation);
            integrations.delete(integration);
        }
    }
}
