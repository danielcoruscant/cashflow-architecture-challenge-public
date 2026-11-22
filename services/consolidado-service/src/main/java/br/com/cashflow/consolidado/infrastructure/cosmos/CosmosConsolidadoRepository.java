package br.com.cashflow.consolidado.infrastructure.cosmos;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.repository.ConsolidadoRepository;
import br.com.cashflow.consolidado.domain.service.ConsolidadoDomainService;
import br.com.cashflow.consolidado.infrastructure.persistence.ConsolidadoDocument;
import br.com.cashflow.consolidado.infrastructure.persistence.MensagemProcessadaDocument;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchItemRequestOptions;
import com.azure.cosmos.models.CosmosBatchOperationResult;
import com.azure.cosmos.models.CosmosBatchResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CosmosConsolidadoRepository implements ConsolidadoRepository {

    private static final String DAILY_DOCUMENT_TYPE = "CONSOLIDADO_DIARIO";
    private static final int MAX_BATCH_ATTEMPTS = 5;

    private final CosmosContainer container;
    private final ConsolidadoDomainService domainService;

    public CosmosConsolidadoRepository(CosmosContainer container, ConsolidadoDomainService domainService) {
        this.container = container;
        this.domainService = domainService;
    }

    @Override
    public Optional<ConsolidadoDiario> buscarAtual(String accountId) {
        requireText(accountId, "accountId");
        SqlQuerySpec query = new SqlQuerySpec(
                "SELECT TOP 1 * FROM c WHERE c.tipoDocumento = @tipoDocumento ORDER BY c.data DESC",
                List.of(new SqlParameter("@tipoDocumento", DAILY_DOCUMENT_TYPE)));
        var options = new CosmosQueryRequestOptions().setPartitionKey(new PartitionKey(accountId));
        try {
            return container.queryItems(query, options, ConsolidadoDocument.class)
                    .stream()
                    .findFirst()
                    .map(this::toDomain);
        } catch (CosmosException exception) {
            throw new ConsolidadoPersistenciaException("Falha ao consultar o consolidado atual.", exception);
        }
    }

    @Override
    public Optional<ConsolidadoDiario> buscarPorData(String accountId, LocalDate data) {
        requireText(accountId, "accountId");
        if (data == null) {
            throw new IllegalArgumentException("A data do consolidado é obrigatória.");
        }
        try {
            return Optional.of(container.readItem(
                            data.toString(),
                            new PartitionKey(accountId),
                            ConsolidadoDocument.class)
                    .getItem())
                    .map(this::toDomain);
        } catch (CosmosException exception) {
            if (exception.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new ConsolidadoPersistenciaException("Falha ao consultar o consolidado por data.", exception);
        }
    }

    @Override
    public void salvar(ConsolidadoDiario consolidado) {
        try {
            ConsolidadoDocument document = toDocument(consolidado);
            container.upsertItem(
                    document,
                    new PartitionKey(consolidado.accountId()),
                    new CosmosItemRequestOptions());
        } catch (CosmosException exception) {
            throw new ConsolidadoPersistenciaException("Falha ao persistir o consolidado.", exception);
        }
    }

    @Override
    public boolean aplicar(ProcessarLancamentoCriadoEvent evento) {
        for (int attempt = 1; attempt <= MAX_BATCH_ATTEMPTS; attempt++) {
            DailySnapshot current = lerSnapshot(evento.accountId(), evento.data());
            ConsolidadoDiario updated = domainService.aplicarLancamento(
                    current == null ? null : toDomain(current.document()),
                    evento.accountId(),
                    evento.data(),
                    evento.tipo(),
                    evento.valor());
            CosmosBatchResponse response;
            try {
                response = container.executeCosmosBatch(criarBatch(evento, updated, current));
            } catch (CosmosException exception) {
                throw new ConsolidadoPersistenciaException("Falha ao aplicar evento ao consolidado.", exception);
            }

            if (response.isSuccessStatusCode()) {
                return true;
            }
            List<CosmosBatchOperationResult> results = response.getResults();
            if (!results.isEmpty() && results.getFirst().getStatusCode() == 409) {
                return false;
            }
            if (results.stream().anyMatch(result -> result.getStatusCode() == 409 || result.getStatusCode() == 412)) {
                continue;
            }
            throw new ConsolidadoPersistenciaException(
                    "O batch de atualização do consolidado falhou com status " + response.getStatusCode() + ".");
        }
        throw new ConsolidadoPersistenciaException(
                "O consolidado não pôde ser atualizado após conflitos concorrentes.");
    }

    private DailySnapshot lerSnapshot(String accountId, LocalDate data) {
        try {
            CosmosItemResponse<ConsolidadoDocument> response = container.readItem(
                    data.toString(),
                    new PartitionKey(accountId),
                    ConsolidadoDocument.class);
            return new DailySnapshot(response.getItem(), response.getETag());
        } catch (CosmosException exception) {
            if (exception.getStatusCode() == 404) {
                return null;
            }
            throw new ConsolidadoPersistenciaException("Falha ao ler o consolidado para atualização.", exception);
        }
    }

    private CosmosBatch criarBatch(
            ProcessarLancamentoCriadoEvent evento,
            ConsolidadoDiario updated,
            DailySnapshot current) {
        CosmosBatch batch = CosmosBatch.createCosmosBatch(new PartitionKey(evento.accountId()));
        batch.createItemOperation(new MensagemProcessadaDocument(
                evento.eventId(),
                evento.accountId(),
                "MENSAGEM_PROCESSADA",
                Instant.now()));
        ConsolidadoDocument document = toDocument(updated);
        if (current == null) {
            batch.createItemOperation(document);
        } else {
            batch.replaceItemOperation(
                    document.id(),
                    document,
                    new CosmosBatchItemRequestOptions().setIfMatchETag(current.etag()));
        }
        return batch;
    }

    private ConsolidadoDocument toDocument(ConsolidadoDiario consolidado) {
        return new ConsolidadoDocument(
                consolidado.data().toString(),
                consolidado.accountId(),
                DAILY_DOCUMENT_TYPE,
                consolidado.data(),
                consolidado.creditos(),
                consolidado.debitos(),
                consolidado.saldo());
    }

    private ConsolidadoDiario toDomain(ConsolidadoDocument document) {
        return new ConsolidadoDiario(
                document.accountId(),
                document.data(),
                document.creditos(),
                document.debitos(),
                document.saldo());
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " é obrigatório.");
        }
    }

    private record DailySnapshot(ConsolidadoDocument document, String etag) {
    }
}
