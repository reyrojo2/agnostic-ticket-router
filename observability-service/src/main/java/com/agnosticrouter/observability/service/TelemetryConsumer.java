package com.agnosticrouter.observability.service;

import com.agnosticrouter.contracts.*;
import com.agnosticrouter.observability.domain.RequestTraceRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryConsumer {
    private final RequestTraceRepository repo;
    public TelemetryConsumer(RequestTraceRepository repo){this.repo=repo;}
    @RabbitListener(queues="observability.email.q")
    public void email(IncomingEmailEvent e){if(valid(e.requestId()))repo.received(e);}
    @RabbitListener(queues="observability.ticket.q")
    public void ticket(StandardTicketEvent e){if(valid(e.requestId()))repo.classified(e);}
    @RabbitListener(queues="observability.system.q")
    public void system(SystemEvent e){if(valid(e.requestId()))repo.system(e);}
    private boolean valid(String id){return id!=null&&!id.isBlank()&&id.length()<=100;}
}
