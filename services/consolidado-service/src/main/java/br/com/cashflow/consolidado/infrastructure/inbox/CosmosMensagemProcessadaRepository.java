package br.com.cashflow.consolidado.infrastructure.inbox;

import br.com.cashflow.consolidado.domain.inbox.MensagemProcessada;
import br.com.cashflow.consolidado.domain.repository.MensagemProcessadaRepository;
import br.com.cashflow.consolidado.infrastructure.cosmos.ConsolidadoPersistenciaException;
import br.com.cashflow.consolidado.infrastructure.persistence.MensagemProcessadaDocument;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class CosmosMensagemProcessadaRepository implements MensagemProcessadaRepository {

    private static final String DOCUMENT_TYPE = "MENSAGEM_PROCESSADA";

    private final CosmosContainer container;

    public CosmosMensagemProcessadaRepository(CosmosContainer container) {
        this.container = container;
    }

    @Override
    public boolean existePorId(String accountId, String id) {
        requireText(accountId, "accountId");
        requireText(id, "eventId");
        try {
            return container.readItem(id, new PartitionKey(accountId), MensagemProcessadaDocument.class)
                    .getItem() != null;
        } catch (CosmosException exception) {
            if (exception.getStatusCode() == 404) {
                return false;
            }
            throw new ConsolidadoPersistenciaException("Falha ao consultar a Inbox.", exception);
        }
    }

    @Override
    public void salvar(MensagemProcessada mensagemProcessada) {
        Objects.requireNonNull(mensagemProcessada, "A mensagem processada é obrigatória.");
        requireText(mensagemProcessada.accountId(), "accountId");
        requireText(mensagemProcessada.id(), "eventId");
        Objects.requireNonNull(mensagemProcessada.processadaEm(), "A data de processamento é obrigatória.");
        try {
            container.createItem(
                    new MensagemProcessadaDocument(
                            mensagemProcessada.id(),
                            mensagemProcessada.accountId(),
                            DOCUMENT_TYPE,
                            mensagemProcessada.processadaEm()),
                    new PartitionKey(mensagemProcessada.accountId()),
                    new CosmosItemRequestOptions());
        } catch (CosmosException exception) {
            throw new ConsolidadoPersistenciaException("Falha ao persistir mensagem na Inbox.", exception);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " é obrigatório.");
        }
    }
}
