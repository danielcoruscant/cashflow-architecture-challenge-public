package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import br.com.cashflow.consolidado.domain.inbox.MensagemProcessada;
import br.com.cashflow.consolidado.infrastructure.inbox.CosmosMensagemProcessadaRepository;
import br.com.cashflow.consolidado.infrastructure.cosmos.ConsolidadoPersistenciaException;
import br.com.cashflow.consolidado.infrastructure.persistence.MensagemProcessadaDocument;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CosmosMensagemProcessadaRepositoryTest {

    @Mock
    private CosmosContainer container;

    @Test
    void existePorIdRetornaFalseQuandoDocumentoNaoExiste() {
        CosmosException notFound = mock(CosmosException.class);
        when(notFound.getStatusCode()).thenReturn(404);
        when(container.readItem("evento-1", new PartitionKey("conta-1"), MensagemProcessadaDocument.class))
                .thenThrow(notFound);
        var repository = new CosmosMensagemProcessadaRepository(container);

        assertFalse(repository.existePorId("conta-1", "evento-1"));
    }

    @SuppressWarnings("unchecked")
@Test
    void existePorIdRetornaTrueQuandoDocumentoExiste() {
        CosmosItemResponse<MensagemProcessadaDocument> response = mock(CosmosItemResponse.class);
        when(response.getItem()).thenReturn(new MensagemProcessadaDocument(
                "evento-1", "conta-1", "MENSAGEM_PROCESSADA", Instant.parse("2026-11-20T10:00:00Z")));
        when(container.readItem("evento-1", new PartitionKey("conta-1"), MensagemProcessadaDocument.class))
                .thenReturn(response);

        assertTrue(new CosmosMensagemProcessadaRepository(container).existePorId("conta-1", "evento-1"));
    }

    @Test
    void falhaDeLeituraOuGravacaoDaInboxDeveSerMapeada() {
        CosmosException unavailable = mock(CosmosException.class);
        when(unavailable.getStatusCode()).thenReturn(503);
        when(container.readItem("evento-1", new PartitionKey("conta-1"), MensagemProcessadaDocument.class))
                .thenThrow(unavailable);
        var repository = new CosmosMensagemProcessadaRepository(container);

        assertThrows(ConsolidadoPersistenciaException.class, () -> repository.existePorId("conta-1", "evento-1"));

        doThrow(unavailable).when(container).createItem(
                any(MensagemProcessadaDocument.class),
                any(PartitionKey.class),
                any(CosmosItemRequestOptions.class));
        MensagemProcessada mensagem = new MensagemProcessada(
                "evento-1", "conta-1", Instant.parse("2026-11-20T10:00:00Z"));
        assertThrows(ConsolidadoPersistenciaException.class, () -> repository.salvar(mensagem));
    }

    @Test
    void salvarGravaInboxNaParticaoDaConta() {
        var repository = new CosmosMensagemProcessadaRepository(container);
        MensagemProcessada mensagem = new MensagemProcessada(
                "evento-1",
                "conta-1",
                Instant.parse("2026-11-20T10:00:00Z"));

        repository.salvar(mensagem);

        ArgumentCaptor<MensagemProcessadaDocument> documentCaptor =
                ArgumentCaptor.forClass(MensagemProcessadaDocument.class);
        ArgumentCaptor<PartitionKey> partitionCaptor = ArgumentCaptor.forClass(PartitionKey.class);
        verify(container).createItem(
                documentCaptor.capture(),
                partitionCaptor.capture(),
                any(CosmosItemRequestOptions.class));
        org.junit.jupiter.api.Assertions.assertEquals("evento-1", documentCaptor.getValue().id());
        org.junit.jupiter.api.Assertions.assertEquals("conta-1", documentCaptor.getValue().accountId());
        org.junit.jupiter.api.Assertions.assertEquals(
                new PartitionKey("conta-1"),
                partitionCaptor.getValue());
    }

    @Test
    void rejeitaChavesDeParticaoOuEventoAusentes() {
        var repository = new CosmosMensagemProcessadaRepository(container);

        assertThrows(IllegalArgumentException.class, () -> repository.existePorId(" ", "evento-1"));
        assertThrows(IllegalArgumentException.class, () -> repository.existePorId("conta-1", " "));
        assertThrows(NullPointerException.class, () -> repository.salvar(null));
    }
}
