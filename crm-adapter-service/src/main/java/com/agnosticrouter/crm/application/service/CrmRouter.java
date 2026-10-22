package com.agnosticrouter.crm.application.service;
import com.agnosticrouter.crm.domain.model.CrmIntegration;
import com.agnosticrouter.crm.domain.model.CrmOperation;
import com.agnosticrouter.crm.domain.model.CrmRoute;
import com.agnosticrouter.crm.application.port.CrmIntegrationStore;
import com.agnosticrouter.crm.application.port.CrmOperationStore;
import com.agnosticrouter.crm.application.port.CrmRouteStore;

import com.agnosticrouter.crm.application.model.CrmExecutionPlan;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.StandardTicketEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrmRouter {
    private final CrmRouteStore routes;
    private final CrmIntegrationStore integrations;
    private final CrmOperationStore operations;

    public CrmRouter(
            CrmRouteStore routes,
            CrmIntegrationStore integrations,
            CrmOperationStore operations
    ) {
        this.routes = routes;
        this.integrations = integrations;
        this.operations = operations;
    }

    public CrmExecutionPlan resolve(StandardTicketEvent ticket) {
        List<CrmRoute> candidates = routes.findByProfileCodeAndActiveTrueOrderByPriorityAsc(ticket.profileCode());
        CrmRoute route = candidates.stream()
                .filter(candidate -> matches(candidate, ticket))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe una ruta CRM aplicable para el perfil " + ticket.profileCode()
                ));

        CrmIntegration integration = integrations.findByCodeAndActiveTrue(route.getIntegrationCode())
                .orElseThrow(() -> new IllegalStateException(
                        "La integración " + route.getIntegrationCode() + " no existe o está inactiva"
                ));

        CrmOperation operation = operations.findByIntegrationIdAndCodeAndActiveTrue(
                        integration.getId(),
                        route.getOperationCode()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la operación " + route.getOperationCode() + " para " + integration.getCode()
                ));

        return new CrmExecutionPlan(route, integration, operation);
    }

    private boolean matches(CrmRoute route, StandardTicketEvent ticket) {
        if (route.getDimensionCode() == null || route.getDimensionCode().isBlank()) {
            return true;
        }
        DecisionValue decision = ticket.decisions().get(route.getDimensionCode());
        if (decision == null) {
            return false;
        }
        return route.getOptionCode() == null
                || route.getOptionCode().isBlank()
                || route.getOptionCode().equals(decision.value());
    }
}
