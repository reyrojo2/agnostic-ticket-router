package com.agnosticrouter.crm.application.service;

import com.agnosticrouter.crm.application.model.CrmExecutionPlan;
import com.agnosticrouter.crm.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmPreviewServiceTest {
    @Test void previewsUsingRealRouteAndComposerWithoutSendingHttp() {
        CrmRouter router=mock(CrmRouter.class);
        CrmPayloadComposer composer=mock(CrmPayloadComposer.class);
        CrmRoute route=new CrmRoute();route.setId(25L);
        CrmIntegration integration=new CrmIntegration();integration.setCode("ESPOCRM_LOCAL");
        CrmOperation operation=new CrmOperation();operation.setCode("CREATE_CASE");
        when(router.resolve(any())).thenReturn(new CrmExecutionPlan(route,integration,operation));
        when(composer.compose(eq(operation),any())).thenReturn(new CrmPayloadComposer.RenderedPayload("{\"name\":\"demo\"}",List.of("cTipo"),List.of()));
        var result=new CrmPreviewService(router,composer).preview(
                new CrmPreviewService.PreviewRequest("TELCO_DEMO",null,"Demo","Text",Map.of("DESTINO","BACKOFFICE")));
        assertEquals("ESPOCRM_LOCAL",result.integrationCode());
        assertTrue(result.bodyJson().contains("demo"));
        verify(router).resolve(any());
        verify(composer).compose(eq(operation),any());
    }
}
