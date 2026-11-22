package com.fluxocaixa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class LancamentoConsumerRuntimeTest {

    @Test
    void deveIniciarEFecharProcessorAposSinalDeDesligamento() {
        ServiceBusProcessorClient processor = org.mockito.Mockito.mock(ServiceBusProcessorClient.class);
        AtomicReference<Runnable> shutdownHook = new AtomicReference<>();
        doAnswer(invocation -> {
            shutdownHook.get().run();
            return null;
        }).when(processor).start();

        LancamentoConsumer.run(
                Map.of(
                        "SERVICE_BUS_NAMESPACE", "cashflow.servicebus.windows.net",
                        "SERVICE_BUS_SUBSCRIPTION", "diagnostico-local"),
                (namespace, subscription) -> {
                    assertTrue(namespace.startsWith("cashflow"));
                    assertTrue(subscription.equals("diagnostico-local"));
                    return processor;
                },
                shutdownHook::set);

        verify(processor).start();
        verify(processor).close();
    }

    @Test
    void deveFecharProcessorEPropagarFalhaDeInicializacao() {
        ServiceBusProcessorClient processor = org.mockito.Mockito.mock(ServiceBusProcessorClient.class);
        doThrow(new IllegalStateException("falha ao iniciar")).when(processor).start();

        assertThrows(
                IllegalStateException.class,
                () -> LancamentoConsumer.run(
                        Map.of(
                                "SERVICE_BUS_NAMESPACE", "cashflow.servicebus.windows.net",
                                "SERVICE_BUS_SUBSCRIPTION", "diagnostico-local"),
                        (namespace, subscription) -> processor,
                        shutdown -> {
                        }));

        verify(processor).close();
    }

    @Test
    void deveFecharProcessorEPreservarInterrupcao() {
        ServiceBusProcessorClient processor = org.mockito.Mockito.mock(ServiceBusProcessorClient.class);
        Thread.currentThread().interrupt();
        try {
            LancamentoConsumer.run(
                    Map.of(
                            "SERVICE_BUS_NAMESPACE", "cashflow.servicebus.windows.net",
                            "SERVICE_BUS_SUBSCRIPTION", "diagnostico-local"),
                    (namespace, subscription) -> processor,
                    shutdown -> {
                    });

            assertTrue(Thread.currentThread().isInterrupted());
            verify(processor).close();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void naoDeveCriarProcessorQuandoConfiguracaoDaSubscriptionEhInvalida() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LancamentoConsumer.run(
                        Map.of(
                                "SERVICE_BUS_NAMESPACE", "cashflow.servicebus.windows.net",
                                "SERVICE_BUS_SUBSCRIPTION", "consolidado-sub"),
                        (namespace, subscription) -> {
                            throw new AssertionError("A configuração inválida não deve criar o processor.");
                        },
                        shutdown -> {
                        }));
    }

    @Test
    void deveCriarProcessorComConfiguracaoExclusiva() {
        ServiceBusProcessorClient configured =
                LancamentoConsumer.createProcessor(
                        "cashflow.servicebus.windows.net",
                        "diagnostico-local");

        assertNotNull(configured);
        configured.close();
    }

    @Test
    void deveRegistrarMensagemEErroDoProcessor() {
        ServiceBusReceivedMessageContext messageContext = org.mockito.Mockito.mock(
                ServiceBusReceivedMessageContext.class);
        ServiceBusReceivedMessage message = org.mockito.Mockito.mock(ServiceBusReceivedMessage.class);
        when(messageContext.getMessage()).thenReturn(message);
        when(message.getMessageId()).thenReturn("message-1");
        when(message.getBody()).thenReturn(BinaryData.fromString("payload"));

        LancamentoConsumer.logMessage(messageContext);

        ServiceBusErrorContext errorContext = org.mockito.Mockito.mock(ServiceBusErrorContext.class);
        when(errorContext.getException()).thenReturn(new IllegalStateException("erro de transporte"));
        LancamentoConsumer.logError(errorContext);

        verify(messageContext).getMessage();
    }
}
