package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchOperationResult;
import com.azure.cosmos.models.CosmosBatchResponse;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import br.com.cashflow.consolidado.domain.service.ConsolidadoDomainService;
import br.com.cashflow.consolidado.infrastructure.cosmos.ConsolidadoPersistenciaException;
import br.com.cashflow.consolidado.infrastructure.cosmos.CosmosConsolidadoRepository;
import br.com.cashflow.consolidado.infrastructure.persistence.ConsolidadoDocument;
import br.com.cashflow.consolidado.infrastructure.persistence.MensagemProcessadaDocument;

@ExtendWith(MockitoExtension.class)
class CosmosConsolidadoRepositoryTest {

        private static final String ACCOUNT_ID = "conta-1";
        private static final LocalDate DATE = LocalDate.of(2026, 11, 20);

        @Mock
        private CosmosContainer container;

        private CosmosConsolidadoRepository repository;

        @BeforeEach
        void setUp() {
                repository = new CosmosConsolidadoRepository(container, new ConsolidadoDomainService());
        }

        @Test
        void aplicarDeveCriarInboxESaldoNaMesmaParticaoEmUmBatch() {
                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(notFound);
                CosmosBatchResponse successfulResponse = successfulBatch();
                when(container.executeCosmosBatch(any(CosmosBatch.class))).thenReturn(successfulResponse);

                assertTrue(repository.aplicar(event(TipoLancamento.CREDITO, "25.00")));

                ArgumentCaptor<CosmosBatch> batchCaptor = ArgumentCaptor.forClass(CosmosBatch.class);
                verify(container).executeCosmosBatch(batchCaptor.capture());
                CosmosBatch batch = batchCaptor.getValue();
                assertEquals(new PartitionKey(ACCOUNT_ID), batch.getPartitionKeyValue());
                assertEquals(2, batch.getOperations().size());
                MensagemProcessadaDocument inbox = batch.getOperations().getFirst().getItem();
                ConsolidadoDocument daily = batch.getOperations().get(1).getItem();
                assertEquals("evento-1", inbox.id());
                assertEquals("MENSAGEM_PROCESSADA", inbox.tipoDocumento());
                assertEquals(DATE.toString(), daily.id());
                assertEquals(new BigDecimal("25.00"), daily.creditos());
        }

        @Test
        void aplicarDeveTratarConflitoNaInboxComoDuplicataSemRepetirBatch() {
                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(notFound);
                CosmosBatchResponse duplicateResponse = failedBatch(409, 424);
                when(container.executeCosmosBatch(any(CosmosBatch.class))).thenReturn(duplicateResponse);

                assertFalse(repository.aplicar(event(TipoLancamento.CREDITO, "25.00")));

                verify(container, times(1)).executeCosmosBatch(any(CosmosBatch.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        void conflitoDeEtagDeveRelerOSaldoAntesDeRepetirOBatch() {
                CosmosItemResponse<ConsolidadoDocument> firstRead = itemResponse(
                                new ConsolidadoDocument(
                                                DATE.toString(), ACCOUNT_ID, "CONSOLIDADO_DIARIO", DATE,
                                                new BigDecimal("10.00"), new BigDecimal("0.00"),
                                                new BigDecimal("10.00")),
                                "etag-1");
                CosmosItemResponse<ConsolidadoDocument> secondRead = itemResponse(
                                new ConsolidadoDocument(
                                                DATE.toString(), ACCOUNT_ID, "CONSOLIDADO_DIARIO", DATE,
                                                new BigDecimal("12.00"), new BigDecimal("0.00"),
                                                new BigDecimal("12.00")),
                                "etag-2");
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenReturn(firstRead, secondRead);
                CosmosBatchResponse conflictResponse = failedBatch(424, 412);
                CosmosBatchResponse successfulResponse = successfulBatch();
                when(container.executeCosmosBatch(any(CosmosBatch.class)))
                                .thenReturn(conflictResponse, successfulResponse);

                assertTrue(repository.aplicar(event(TipoLancamento.CREDITO, "5.00")));

                ArgumentCaptor<CosmosBatch> batchCaptor = ArgumentCaptor.forClass(CosmosBatch.class);
                verify(container, times(2)).executeCosmosBatch(batchCaptor.capture());
                ConsolidadoDocument retriedDocument = batchCaptor.getAllValues().get(1).getOperations().get(1)
                                .getItem();
                assertEquals(new BigDecimal("17.00"), retriedDocument.creditos());
                assertEquals(new BigDecimal("17.00"), retriedDocument.saldo());
        }

        @Test
        void conflitosDeEtagPersistentesDevemEncerrarRetryComErro() {
                CosmosItemResponse<ConsolidadoDocument> snapshot = itemResponse(
                                new ConsolidadoDocument(
                                                DATE.toString(), ACCOUNT_ID, "CONSOLIDADO_DIARIO", DATE,
                                                new BigDecimal("10.00"), new BigDecimal("0.00"),
                                                new BigDecimal("10.00")),
                                "etag");
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenReturn(snapshot);
                CosmosBatchResponse conflict = failedBatch(424, 412);
                when(container.executeCosmosBatch(any(CosmosBatch.class))).thenReturn(conflict);

                assertThrows(ConsolidadoPersistenciaException.class,
                                () -> repository.aplicar(event(TipoLancamento.CREDITO, "5.00")));

                verify(container, times(5)).readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID),
                                ConsolidadoDocument.class);
                verify(container, times(5)).executeCosmosBatch(any(CosmosBatch.class));
        }

        @Test
        void buscarAtualDeveConsultarSomenteAParticaoDaConta() {
                ConsolidadoDocument document = new ConsolidadoDocument(
                                DATE.toString(),
                                ACCOUNT_ID,
                                "CONSOLIDADO_DIARIO",
                                DATE,
                                new BigDecimal("10.00"),
                                new BigDecimal("2.00"),
                                new BigDecimal("8.00"));
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<ConsolidadoDocument> results = mock(CosmosPagedIterable.class);
                when(results.stream()).thenReturn(Stream.of(document));
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(ConsolidadoDocument.class)))
                                .thenReturn(results);

                assertTrue(repository.buscarAtual(ACCOUNT_ID).isPresent());

                ArgumentCaptor<SqlQuerySpec> queryCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);
                ArgumentCaptor<CosmosQueryRequestOptions> optionsCaptor = ArgumentCaptor
                                .forClass(CosmosQueryRequestOptions.class);
                verify(container).queryItems(
                                queryCaptor.capture(),
                                optionsCaptor.capture(),
                                eq(ConsolidadoDocument.class));
                assertTrue(queryCaptor.getValue().getQueryText().contains("ORDER BY c.data DESC"));
                assertEquals(new PartitionKey(ACCOUNT_ID), optionsCaptor.getValue().getPartitionKey());
        }

        @Test
        void buscarAtualDeveRetornarVazioSemResultados() {
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<ConsolidadoDocument> results = mock(CosmosPagedIterable.class);
                when(results.stream()).thenReturn(Stream.empty());
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(ConsolidadoDocument.class)))
                                .thenReturn(results);

                assertTrue(repository.buscarAtual(ACCOUNT_ID).isEmpty());
        }

        @Test
        void buscarAtualDeveMapearFalhaDoCosmos() {
                CosmosException unavailable = mock(CosmosException.class);
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(ConsolidadoDocument.class)))
                                .thenThrow(unavailable);

                assertThrows(ConsolidadoPersistenciaException.class, () -> repository.buscarAtual(ACCOUNT_ID));
        }

        @Test
        void buscarPorDataDeveRetornarSaldoETratarAusencia() {
                ConsolidadoDocument document = new ConsolidadoDocument(
                                DATE.toString(), ACCOUNT_ID, "CONSOLIDADO_DIARIO", DATE,
                                new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("8.00"));
                @SuppressWarnings("unchecked")
                CosmosItemResponse<ConsolidadoDocument> response = mock(CosmosItemResponse.class);
                when(response.getItem()).thenReturn(document);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenReturn(response);

                assertEquals(new BigDecimal("8.00"), repository.buscarPorData(ACCOUNT_ID, DATE).orElseThrow().saldo());

                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem("2026-11-21", new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(notFound);
                assertTrue(repository.buscarPorData(ACCOUNT_ID, DATE.plusDays(1)).isEmpty());
        }

        @Test
        void buscarPorDataDeveValidarParametrosEMapearFalhasNaoNotFound() {
                assertThrows(IllegalArgumentException.class, () -> repository.buscarPorData(" ", DATE));
                assertThrows(IllegalArgumentException.class, () -> repository.buscarPorData(ACCOUNT_ID, null));

                CosmosException unavailable = mock(CosmosException.class);
                when(unavailable.getStatusCode()).thenReturn(503);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(unavailable);

                assertThrows(
                                ConsolidadoPersistenciaException.class,
                                () -> repository.buscarPorData(ACCOUNT_ID, DATE));
        }

        @Test
        void salvarDeveMapearFalhaDoCosmos() {
                CosmosException unavailable = mock(CosmosException.class);
                doThrow(unavailable).when(container).upsertItem(
                                any(ConsolidadoDocument.class),
                                any(PartitionKey.class),
                                any(com.azure.cosmos.models.CosmosItemRequestOptions.class));
                ConsolidadoDiario saldo = new ConsolidadoDiario(
                                ACCOUNT_ID, DATE, new BigDecimal("10.00"), new BigDecimal("2.00"),
                                new BigDecimal("8.00"));

                assertThrows(ConsolidadoPersistenciaException.class, () -> repository.salvar(saldo));
        }

        @Test
        void respostaDoBatchSemConflitoDeveSerMapeadaComoErro() {
                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(notFound);
                CosmosBatchResponse invalidRequest = failedBatch(400, 424);
                when(container.executeCosmosBatch(any(CosmosBatch.class))).thenReturn(invalidRequest);

                assertThrows(
                                ConsolidadoPersistenciaException.class,
                                () -> repository.aplicar(event(TipoLancamento.CREDITO, "25.00")));
        }

        @Test
        void falhaInesperadaAoLerSnapshotDeveSerMapeada() {
                CosmosException unavailable = mock(CosmosException.class);
                when(unavailable.getStatusCode()).thenReturn(503);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(unavailable);
                assertThrows(
                                ConsolidadoPersistenciaException.class,
                                () -> repository.aplicar(event(TipoLancamento.CREDITO, "25.00")));
        }

        @Test
        void falhaAoExecutarBatchDeveSerMapeada() {
                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem(DATE.toString(), new PartitionKey(ACCOUNT_ID), ConsolidadoDocument.class))
                                .thenThrow(notFound);
                CosmosException unavailable = mock(CosmosException.class);
                doThrow(unavailable).when(container).executeCosmosBatch(any(CosmosBatch.class));

                assertThrows(
                                ConsolidadoPersistenciaException.class,
                                () -> repository.aplicar(event(TipoLancamento.CREDITO, "25.00")));
        }

        private ProcessarLancamentoCriadoEvent event(TipoLancamento type, String amount) {
                return new ProcessarLancamentoCriadoEvent(
                                "evento-1",
                                ACCOUNT_ID,
                                DATE,
                                type,
                                new BigDecimal(amount),
                                null,
                                "correlation-1",
                                "causation-1",
                                Instant.parse("2026-11-20T10:00:00Z"));
        }

        private CosmosItemResponse<ConsolidadoDocument> itemResponse(ConsolidadoDocument document, String etag) {
                @SuppressWarnings("unchecked")
                CosmosItemResponse<ConsolidadoDocument> response = mock(CosmosItemResponse.class);
                when(response.getItem()).thenReturn(document);
                when(response.getETag()).thenReturn(etag);
                return response;
        }

        private CosmosBatchResponse successfulBatch() {
                CosmosBatchResponse response = mock(CosmosBatchResponse.class);
                when(response.isSuccessStatusCode()).thenReturn(true);
                return response;
        }

        private CosmosBatchResponse failedBatch(int firstStatus, int secondStatus) {
                CosmosBatchOperationResult first = mock(CosmosBatchOperationResult.class);
                CosmosBatchOperationResult second = mock(CosmosBatchOperationResult.class);
                when(first.getStatusCode()).thenReturn(firstStatus);
                if (firstStatus != 409) {
                        when(second.getStatusCode()).thenReturn(secondStatus);
                }
                CosmosBatchResponse response = mock(CosmosBatchResponse.class);
                when(response.isSuccessStatusCode()).thenReturn(false);
                when(response.getResults()).thenReturn(List.of(first, second));
                return response;
        }
}
