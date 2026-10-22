package com.agnosticrouter.decision.infrastructure.client;

import com.agnosticrouter.decision.application.model.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SystemOneDecisionClientTest {
    @Test void sendsConfiguredUrlModelAndKeyHeaderWithoutRestart() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> key = new AtomicReference<>(), body = new AtomicReference<>();
        server.createContext("/custom/systemone", exchange -> {
            key.set(exchange.getRequestHeaders().getFirst("x-provider-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"answers\":{\"q\":{\"choice\":\"OK\"}}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var connection = new AiConnection("http://127.0.0.1:" + server.getAddress().getPort(), "/custom/systemone", "chosen-model", "es", "API_KEY_HEADER", "x-provider-key", "test-key");
            var response = new SystemOneDecisionClient().decide(connection, Map.of("text", "test"), "q", new SystemOneModels.Question("choice", "Pick", Map.of("OK", "OK")), .7);
            assertEquals("test-key", key.get()); assertTrue(body.get().contains("chosen-model")); assertEquals("OK", response.answers().get("q").choice());
        } finally { server.stop(0); }
    }
    @Test void providerErrorsNeverExposeResponseBodies() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/systemone", exchange -> {
            exchange.getRequestBody().readAllBytes(); byte[] error = "echoed-secret-test-key".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, error.length); exchange.getResponseBody().write(error); exchange.close();
        });
        server.start();
        try {
            var connection = new AiConnection("http://127.0.0.1:" + server.getAddress().getPort(), "/v1/systemone", "", "", "BEARER", "", "test-key");
            var failure = assertThrows(AiProviderFailure.class, () -> new SystemOneDecisionClient().decide(connection, Map.of(), "q", new SystemOneModels.Question("choice", "Pick", Map.of()), .7));
            assertEquals(401, failure.status()); assertFalse(failure.getMessage().contains("test-key"));
        } finally { server.stop(0); }
    }
    @Test void extractsOnlyMissingModelMetadataWithoutKeepingEchoedInputs() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/systemone",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] error="{\"detail\":[{\"type\":\"missing\",\"loc\":[\"body\",\"model\"],\"input\":{\"key\":\"echoed-secret\"},\"msg\":\"echoed-secret\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(422,error.length);
            exchange.getResponseBody().write(error);exchange.close();
        });
        server.start();
        try {
            var connection=new AiConnection("http://127.0.0.1:"+server.getAddress().getPort(),"/v1/systemone","","","NONE","",null);
            var failure=assertThrows(AiProviderFailure.class,()->new SystemOneDecisionClient().decide(connection,Map.of(),"q",new SystemOneModels.Question("noul","Test",null),.5));
            assertEquals(422,failure.status());assertTrue(failure.modelRequired());
            assertFalse(failure.toString().contains("echoed-secret"));assertNull(failure.getCause());
        } finally {server.stop(0);}
    }
}
