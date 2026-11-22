package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cashflow.consolidado.application.projection.AtualizarConsolidadoProjection;
import br.com.cashflow.consolidado.domain.service.ConsolidadoDomainService;
import br.com.cashflow.consolidado.infrastructure.cosmos.ConsolidadoPersistenciaException;
import br.com.cashflow.consolidado.infrastructure.cosmos.CosmosConsolidadoRepository;
import br.com.cashflow.consolidado.infrastructure.persistence.ConsolidadoDocument;
import br.com.cashflow.consolidado.infrastructure.persistence.MensagemProcessadaDocument;
import br.com.cashflow.consolidado.infrastructure.servicebus.LancamentoCreatedConsumer;
import br.com.cashflow.consolidado.infrastructure.servicebus.LancamentoCreatedMessageHandler;
import com.azure.core.util.BinaryData;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchOperationResult;
import com.azure.cosmos.models.CosmosBatchResponse;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Simula atomicidade e redelivery em memória; não exercita o Cosmos nem o broker.
 */
class LocalRedeliveryIntegrationTest {
    private static final String ACCOUNT = "conta-1";
    private static final LocalDate DATE = LocalDate.parse("2026-11-20");

    @Test
    void falhasAntesEDepoisDoCommitRetomadaEReconciliacaoComLedger() {
        SimulatedPartition partition = new SimulatedPartition();
        LancamentoCreatedMessageHandler handler = handler(partition.container);
        String credito = event("credito", "CREDITO", "100.00");
        String debito = event("debito", "DEBITO", "25.00");

        // Falha antes do commit: nada é persistido e a entrega é abandonada.
        partition.failBeforeCommit = true;
        ServiceBusReceivedMessageContext first = delivery(credito);
        assertThrows(ConsolidadoPersistenciaException.class, () -> handler.process(first));
        verify(first).abandon();
        verify(first, never()).complete();
        assertEquals(0, partition.inbox.size());

        // O batch é confirmado, mas o ACK falha; o broker poderá redeliver.
        ServiceBusReceivedMessageContext ackFailure = delivery(credito);
        doThrow(new IllegalStateException("ack indisponível")).when(ackFailure).complete();
        assertThrows(IllegalStateException.class, () -> handler.process(ackFailure));
        verify(ackFailure).abandon();
        assertEquals(new BigDecimal("100.00"), partition.daily.saldo());

        // Nova instância do consumidor após reinício, com a mesma partição persistida.
        LancamentoCreatedMessageHandler restarted = handler(partition.container);
        ServiceBusReceivedMessageContext redelivery = delivery(credito);
        restarted.process(redelivery);
        verify(redelivery).complete();
        assertEquals(new BigDecimal("100.00"), partition.daily.saldo());
        assertEquals(Set.of("credito"), partition.inbox);

        ServiceBusReceivedMessageContext debitDelivery = delivery(debito);
        restarted.process(debitDelivery);
        verify(debitDelivery).complete();

        // Ledger do cenário: um crédito de 100 e um débito de 25.
        assertEquals(Set.of("credito", "debito"), partition.inbox);
        assertEquals(new BigDecimal("100.00"), partition.daily.creditos());
        assertEquals(new BigDecimal("25.00"), partition.daily.debitos());
        assertEquals(new BigDecimal("75.00"), partition.daily.saldo());
    }

    private static LancamentoCreatedMessageHandler handler(CosmosContainer container) {
        var repository = new CosmosConsolidadoRepository(container, new ConsolidadoDomainService());
        var consumer = new LancamentoCreatedConsumer(new AtualizarConsolidadoProjection(repository));
        return new LancamentoCreatedMessageHandler(new ObjectMapper().findAndRegisterModules(), consumer);
    }

    private static ServiceBusReceivedMessageContext delivery(String payload) {
        var context = mock(ServiceBusReceivedMessageContext.class);
        var message = mock(ServiceBusReceivedMessage.class);
        when(context.getMessage()).thenReturn(message);
        when(message.getBody()).thenReturn(BinaryData.fromString(payload));
        return context;
    }

    private static String event(String id, String type, String amount) {
        return """
                {"eventId":"%s","accountId":"conta-1","data":"2026-11-20","tipo":"%s",\
                "valor":%s,"correlationId":"corr","causationId":"cause","timestamp":"2026-11-20T10:00:00Z"}"""
                .formatted(id, type, amount);
    }

    private static class SimulatedPartition {
        final CosmosContainer container = mock(CosmosContainer.class);
        final Set<String> inbox = new HashSet<>();
        ConsolidadoDocument daily;
        boolean failBeforeCommit;

        SimulatedPartition() {
            when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT), ConsolidadoDocument.class))
                    .thenAnswer(invocation -> {
                        if (daily == null) {
                            CosmosException notFound = mock(CosmosException.class);
                            when(notFound.getStatusCode()).thenReturn(404);
                            throw notFound;
                        }
                        @SuppressWarnings("unchecked")
                        CosmosItemResponse<ConsolidadoDocument> response = mock(CosmosItemResponse.class);
                        when(response.getItem()).thenReturn(daily);
                        when(response.getETag()).thenReturn("local-etag");
                        return response;
                    });
            when(container.executeCosmosBatch(any(CosmosBatch.class))).thenAnswer(invocation -> {
                CosmosBatch batch = invocation.getArgument(0);
                assertEquals(new PartitionKey(ACCOUNT), batch.getPartitionKeyValue());
                assertEquals(2, batch.getOperations().size());
                MensagemProcessadaDocument marker = batch.getOperations().getFirst().getItem();
                ConsolidadoDocument next = batch.getOperations().get(1).getItem();
                if (failBeforeCommit) {
                    failBeforeCommit = false;
                    throw mock(CosmosException.class);
                }
                CosmosBatchResponse response = mock(CosmosBatchResponse.class);
                if (inbox.contains(marker.id())) {
                    CosmosBatchOperationResult conflict = mock(CosmosBatchOperationResult.class);
                    when(conflict.getStatusCode()).thenReturn(409);
                    when(response.isSuccessStatusCode()).thenReturn(false);
                    when(response.getResults()).thenReturn(List.of(conflict));
                } else {
                    inbox.add(marker.id());
                    daily = next;
                    when(response.isSuccessStatusCode()).thenReturn(true);
                }
                return response;
            });
        }
    }
}
