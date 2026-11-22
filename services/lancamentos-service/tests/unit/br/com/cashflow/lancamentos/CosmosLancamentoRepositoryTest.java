package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.infrastructure.cosmos.CosmosLancamentoRepository;
import br.com.cashflow.lancamentos.infrastructure.cosmos.LancamentoPersistenciaException;
import br.com.cashflow.lancamentos.infrastructure.mapper.LancamentoDocumentMapper;
import br.com.cashflow.lancamentos.infrastructure.persistence.LancamentoDocument;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CosmosLancamentoRepositoryTest {

        @Mock
        private CosmosContainer container;

        private CosmosLancamentoRepository repository;

        @BeforeEach
        void setUp() {
                repository = new CosmosLancamentoRepository(container, new LancamentoDocumentMapper());
        }

        @Test
        void salvarDeveGravarODocumentoNaParticaoDaConta() {
                Lancamento lancamento = lancamento("lancamento-1");

                repository.salvar(lancamento);

                ArgumentCaptor<LancamentoDocument> documentCaptor = ArgumentCaptor.forClass(LancamentoDocument.class);
                ArgumentCaptor<PartitionKey> partitionCaptor = ArgumentCaptor.forClass(PartitionKey.class);
                verify(container).createItem(
                                documentCaptor.capture(),
                                partitionCaptor.capture(),
                                any(CosmosItemRequestOptions.class));
                assertEquals("lancamento-1", documentCaptor.getValue().id());
                assertEquals("conta-1", documentCaptor.getValue().accountId());
                assertEquals(new PartitionKey("conta-1"), partitionCaptor.getValue());
        }

        @Test
        void buscarPorIdDeveRetornarVazioPara404() {
                CosmosException notFound = mock(CosmosException.class);
                when(notFound.getStatusCode()).thenReturn(404);
                when(container.readItem("ausente", new PartitionKey("conta-1"), LancamentoDocument.class))
                                .thenThrow(notFound);

                assertTrue(repository.buscarPorId("conta-1", "ausente").isEmpty());
        }

        @Test
        void salvarDeveMapearFalhaDoCosmos() {
                CosmosException unavailable = mock(CosmosException.class);
                doThrow(unavailable).when(container).createItem(
                                any(LancamentoDocument.class),
                                any(PartitionKey.class),
                                any(CosmosItemRequestOptions.class));

                assertThrows(LancamentoPersistenciaException.class,
                                () -> repository.salvar(lancamento("lancamento-1")));
        }

        @Test
        void buscarPorIdDeveFalharExplicitamenteParaErroDiferenteDe404() {
                CosmosException serviceUnavailable = mock(CosmosException.class);
                when(serviceUnavailable.getStatusCode()).thenReturn(503);
                when(container.readItem("lancamento-1", new PartitionKey("conta-1"), LancamentoDocument.class))
                                .thenThrow(serviceUnavailable);

                assertThrows(
                                LancamentoPersistenciaException.class,
                                () -> repository.buscarPorId("conta-1", "lancamento-1"));
        }

        @Test
        void buscarPorIdDeveRejeitarContaOuIdentificadorAusentes() {
                assertThrows(IllegalArgumentException.class, () -> repository.buscarPorId(" ", "lancamento-1"));
                assertThrows(IllegalArgumentException.class, () -> repository.buscarPorId("conta-1", " "));
        }

        @Test
        void listarDeveRestringirContaAplicarPaginacaoERetornarContagem() {
                LancamentoDocument document = new LancamentoDocument(
                                "lancamento-1",
                                "conta-1",
                                "CREDITO",
                                new BigDecimal("10.00"),
                                LocalDate.of(2026, 11, 20),
                                "correlation-1",
                                "causation-1",
                                null);
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<LancamentoDocument> pageResults = mock(CosmosPagedIterable.class);
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<Long> countResults = mock(CosmosPagedIterable.class);
                when(pageResults.stream()).thenReturn(Stream.of(document));
                when(countResults.stream()).thenReturn(Stream.of(12L));
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(LancamentoDocument.class)))
                                .thenReturn(pageResults);
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(Long.class)))
                                .thenReturn(countResults);

                var result = repository.listar("conta-1", 1, 10);

                assertEquals(1, result.items().size());
                assertEquals("lancamento-1", result.items().getFirst().id());
                assertEquals(1, result.page());
                assertEquals(10, result.pageSize());
                assertEquals(12, result.totalItems());
                ArgumentCaptor<SqlQuerySpec> queryCaptor = ArgumentCaptor.forClass(SqlQuerySpec.class);
                ArgumentCaptor<CosmosQueryRequestOptions> optionsCaptor = ArgumentCaptor
                                .forClass(CosmosQueryRequestOptions.class);
                verify(container, org.mockito.Mockito.times(2))
                                .queryItems(queryCaptor.capture(), optionsCaptor.capture(), any());
                assertTrue(queryCaptor.getAllValues().getFirst().getQueryText().contains("c.accountId = @accountId"));
                assertTrue(queryCaptor.getAllValues().getFirst().getQueryText()
                                .contains("OFFSET @offset LIMIT @pageSize"));
                assertEquals(new PartitionKey("conta-1"), optionsCaptor.getAllValues().getFirst().getPartitionKey());
                assertEquals(new PartitionKey("conta-1"), optionsCaptor.getAllValues().get(1).getPartitionKey());
        }

        @Test
        void listarDeveRetornarZeroQuandoConsultaDeContagemNaoTemResultado() {
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<LancamentoDocument> pageResults = mock(CosmosPagedIterable.class);
                @SuppressWarnings("unchecked")
                CosmosPagedIterable<Long> countResults = mock(CosmosPagedIterable.class);
                when(pageResults.stream()).thenReturn(Stream.empty());
                when(countResults.stream()).thenReturn(Stream.empty());
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(LancamentoDocument.class)))
                                .thenReturn(pageResults);
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(Long.class)))
                                .thenReturn(countResults);

                var result = repository.listar("conta-1", 0, 20);

                assertTrue(result.items().isEmpty());
                assertEquals(0, result.totalItems());
        }

        @Test
        void listarDeveRejeitarParametrosInvalidos() {
                assertThrows(IllegalArgumentException.class, () -> repository.listar(null, 0, 20));
                assertThrows(IllegalArgumentException.class, () -> repository.listar(" ", 0, 20));
                assertThrows(IllegalArgumentException.class, () -> repository.listar("conta-1", -1, 20));
                assertThrows(IllegalArgumentException.class, () -> repository.listar("conta-1", 0, 0));
                assertThrows(IllegalArgumentException.class, () -> repository.listar("conta-1", 0, 101));
        }

        @Test
        void listarDeveMapearFalhaDoCosmos() {
                CosmosException unavailable = mock(CosmosException.class);
                when(container.queryItems(
                                any(SqlQuerySpec.class),
                                any(CosmosQueryRequestOptions.class),
                                eq(LancamentoDocument.class)))
                                .thenThrow(unavailable);

                assertThrows(LancamentoPersistenciaException.class, () -> repository.listar("conta-1", 0, 20));
        }

        @Test
        void listarDeveRejeitarOffsetForaDaFaixaRepresentavel() {
                assertThrows(IllegalArgumentException.class,
                                () -> repository.listar("conta-1", Integer.MAX_VALUE, 100));
        }

        private Lancamento lancamento(String id) {
                return new Lancamento(
                                id,
                                "conta-1",
                                TipoLancamento.CREDITO,
                                new ValorMonetario(new BigDecimal("10.00")),
                                new DataLancamento(LocalDate.of(2026, 11, 20)),
                                "correlation-1",
                                "causation-1",
                                null);
        }
}
