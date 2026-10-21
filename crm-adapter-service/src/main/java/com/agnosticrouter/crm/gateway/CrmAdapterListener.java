package com.agnosticrouter.crm.gateway;

import com.agnosticrouter.contracts.RabbitNames;
import com.agnosticrouter.contracts.StandardTicketEvent;
import com.agnosticrouter.crm.service.ProcessClassifiedTicket;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Transport adapter: delegates CRM business workflow to its use case. */
@Component
public class CrmAdapterListener {
    private final ProcessClassifiedTicket processor;
    public CrmAdapterListener(ProcessClassifiedTicket processor) { this.processor=processor; }
    @RabbitListener(queues=RabbitNames.Q_CRM_STANDARD_TICKETS)
    public void handle(StandardTicketEvent ticket) { processor.process(ticket); }
}
