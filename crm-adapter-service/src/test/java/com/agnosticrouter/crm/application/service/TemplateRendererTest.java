package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.contracts.DecisionValue;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateRendererTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final TemplateRenderer renderer = new TemplateRenderer(mapper);

    @Test void rendersEveryClassificationDimensionAndEscapesText() throws Exception {
        StandardTicketEvent event = sample();
        String json = renderer.renderJson("""
            {"name":"{{subject}}","description":"{{body}}\\n{{classificationSummary}}\\n{{requestId}}"}
            """, event);
        JsonNode node = mapper.readTree(json);
        assertEquals(event.subject(), node.get("name").asText());
        assertTrue(node.get("description").asText().contains("DESTINO: BACKOFFICE [RULE]"));
        assertTrue(node.get("description").asText().contains("PRODUCTO: INTERNET_DEDICADO [RULE]"));
        assertTrue(node.get("description").asText().contains("ATR-001"));
    }

    @Test void supportsMappingToConfiguredCustomCrmFields() throws Exception {
        JsonNode node = mapper.readTree(renderer.renderJson(
                "{\"cDestination\":\"{{decisions.DESTINO.value}}\"}", sample()));
        assertEquals("BACKOFFICE", node.get("cDestination").asText());
    }

    private StandardTicketEvent sample() {
        return new StandardTicketEvent("ATR-001", "TELCO_DEMO", 1, "source@example.test",
            List.of(), List.of(), "Cambio de RUC", "Solicitud con \"comillas\" y salto\nlinea", Map.of(),
            Map.of(
                "DESTINO", new DecisionValue("DESTINO", "BACKOFFICE", "RULE", null, null, null, null, true, Map.of(), null),
                "PRODUCTO", new DecisionValue("PRODUCTO", "INTERNET_DEDICADO", "RULE", null, null, null, null, true, Map.of(), null)
            ), false, Instant.now());
    }
}
