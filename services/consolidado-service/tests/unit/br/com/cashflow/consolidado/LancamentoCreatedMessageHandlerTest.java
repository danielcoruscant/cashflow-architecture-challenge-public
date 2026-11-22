package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import br.com.cashflow.consolidado.infrastructure.servicebus.LancamentoCreatedConsumer;
import br.com.cashflow.consolidado.infrastructure.servicebus.LancamentoCreatedMessageHandler;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LancamentoCreatedMessageHandlerTest {

    private static final String VALID_EVENT = """
            {"eventId":"evento-1","accountId":"conta-1","data":"2026-11-20","tipo":"CREDITO",\
            "valor":100.00,"compensaLancamentoId":null,"correlationId":"correlation-1",\
            "causationId":"causation-1","timestamp":"2026-11-20T10:00:00Z"}""";

    @Mock
    private LancamentoCreatedConsumer consumer;

    @Mock
    private ServiceBusReceivedMessageContext context;

    @Mock
    private ServiceBusReceivedMessage message;

    private LancamentoCreatedMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LancamentoCreatedMessageHandler(new ObjectMapper().findAndRegisterModules(), consumer);
        when(context.getMessage()).thenReturn(message);
    }

    @Test
    void completaMensagemSomenteDepoisDoRetornoBemSucedidoDaProjecao() {
        when(message.getBody()).thenReturn(BinaryData.fromString(VALID_EVENT));
        Logger logger = (Logger) LoggerFactory.getLogger(LancamentoCreatedMessageHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            handler.process(context);
        } finally {
            logger.detachAppender(appender);
        }

        verify(consumer).consumir(argThat(event ->
                event.eventId().equals("evento-1")
                        && event.accountId().equals("conta-1")
                        && event.tipo() == TipoLancamento.CREDITO
                        && event.compensaLancamentoId() == null
                        && event.timestamp().equals(Instant.parse("2026-11-20T10:00:00Z"))));
        verify(context).complete();
        verify(context, never()).abandon();
        verify(context, never()).deadLetter();
        assertEquals("correlation-1", appender.list.get(0).getMDCPropertyMap().get("correlationId"));
    }

    @Test
    void abandonaMensagemEPropagaFalhaTransitoriaDaProjecao() {
        when(message.getBody()).thenReturn(BinaryData.fromString(VALID_EVENT));
        doThrow(new IllegalStateException("Cosmos indisponível")).when(consumer).consumir(any());

        assertThrows(IllegalStateException.class, () -> handler.process(context));

        verify(context).abandon();
        verify(context, never()).complete();
        verify(context, never()).deadLetter();
    }

    @Test
    void enviaEventoInvalidoParaDeadLetterSemInvocarProjecao() {
        when(message.getBody()).thenReturn(BinaryData.fromString("{"));

        handler.process(context);

        verify(context).deadLetter();
        verify(context, never()).complete();
        verify(context, never()).abandon();
        verify(consumer, never()).consumir(any());
    }
}
