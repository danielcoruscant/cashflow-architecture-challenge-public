package br.com.cashflow.consolidado.infrastructure.servicebus;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LancamentoCreatedMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoCreatedMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final LancamentoCreatedConsumer consumer;

    public LancamentoCreatedMessageHandler(ObjectMapper objectMapper, LancamentoCreatedConsumer consumer) {
        this.objectMapper = objectMapper;
        this.consumer = consumer;
    }

    public void process(ServiceBusReceivedMessageContext context) {
        String payload = context.getMessage().getBody().toString();
        ProcessarLancamentoCriadoEvent event;
        try {
            event = objectMapper.readValue(payload, ProcessarLancamentoCriadoEvent.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            LOGGER.warn("Evento inválido rejeitado pelo consumidor.", exception);
            context.deadLetter();
            return;
        }

        MDC.MDCCloseable mdcCloseable = MDC.putCloseable("correlationId", event.correlationId());
        try {
            LOGGER.info("Processando evento eventId={}", event.eventId());
            try {
                consumer.consumir(event);
                context.complete();
                LOGGER.info("Evento processado eventId={}", event.eventId());
            } catch (RuntimeException exception) {
                context.abandon();
                LOGGER.error("Falha ao projetar evento eventId={}", event.eventId(), exception);
                throw exception;
            }
        } finally {
            mdcCloseable.close();
        }
    }
}
