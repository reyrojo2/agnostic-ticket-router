package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.*;
import com.agnosticrouter.decision.application.port.*;
import com.agnosticrouter.decision.domain.model.AiProvider;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiProviderProbeServiceTest {
    @Test void probeUsesYesNoSoItDoesNotRequireAnInvalidSingleChoiceCatalog() {
        var store=mock(AiProviderStore.class);var resolver=mock(AiProviderResolver.class);var transport=mock(SystemOneTransport.class);
        var provider=new AiProvider(1L,"test","http://localhost","/v1/systemone","model","es","NONE","",null,true,false);
        var connection=new AiConnection("http://localhost","/v1/systemone","model","es","NONE","",null);
        when(store.findById(1L)).thenReturn(Optional.of(provider));when(resolver.connection(provider)).thenReturn(connection);
        when(transport.decide(eq(connection),any(),eq("connection_test"),argThat(q -> q.type().equals("noul") && q.criteria()==null),anyDouble()))
                .thenReturn(new SystemOneModels.Response("model",Map.of("connection_test",new SystemOneModels.Answer("noul",null,null,.99,null,null,null,null,null,null)),null,null));
        var service=new AiProviderProbeService(store,resolver,transport);
        assertTrue(service.test(1L).success());
        when(transport.decide(any(),any(),anyString(),any(),anyDouble())).thenThrow(new AiProviderFailure(422,true));
        var failure=service.test(1L);assertFalse(failure.success()); assertTrue(failure.message().contains("Modelo"));assertTrue(failure.message().contains("422"));
    }
}
