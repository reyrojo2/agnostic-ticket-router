package com.agnosticrouter.crm.infrastructure.client;

import com.agnosticrouter.crm.application.service.*;
import com.agnosticrouter.crm.application.model.*;
import com.agnosticrouter.crm.domain.model.*;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmHttpExecutorTest {
    @Test void extractsThePublicNumberSeparatelyAndSupportsOtherCrmResponsePaths() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1/Case",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] body="{\"id\":\"long-technical-id\",\"number\":5,\"ticket\":{\"public\":\"CASE-0005\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            var ticket=mock(StandardTicketEvent.class);var templates=mock(TemplateRenderer.class);
            when(templates.renderText("/api/v1/Case",ticket)).thenReturn("/api/v1/Case");
            var executor=new CrmHttpExecutor(templates,mock(CrmPayloadComposer.class),new ResponseValueExtractor(new ObjectMapper()),mock(CrmAuthenticationConfigurer.class));
            var integration=new CrmIntegration();integration.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
            var operation=new CrmOperation();operation.setPathTemplate("/api/v1/Case");operation.setExternalIdPath("id");operation.setExternalNumberPath("number");
            var result=executor.execute(new CrmExecutionPlan(null,integration,operation),ticket);
            assertTrue(result.success());assertEquals("long-technical-id",result.externalId());assertEquals("5",result.externalNumber());assertEquals("5",result.ticketReference());
            operation.setExternalNumberPath("ticket.public");
            assertEquals("CASE-0005",executor.execute(new CrmExecutionPlan(null,integration,operation),ticket).ticketReference());
        }finally{server.stop(0);}
    }
}
