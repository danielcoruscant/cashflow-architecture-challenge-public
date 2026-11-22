package br.com.cashflow.lancamentos.infrastructure.cosmos;

import br.com.cashflow.lancamentos.domain.model.PaginaLancamentos;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.infrastructure.mapper.LancamentoDocumentMapper;
import br.com.cashflow.lancamentos.infrastructure.persistence.LancamentoDocument;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CosmosLancamentoRepository implements LancamentoRepository {

    private static final String COUNT_QUERY =
            "SELECT VALUE COUNT(1) FROM c WHERE c.accountId = @accountId";

    private final CosmosContainer container;
    private final LancamentoDocumentMapper mapper;

    public CosmosLancamentoRepository(CosmosContainer container, LancamentoDocumentMapper mapper) {
        this.container = container;
        this.mapper = mapper;
    }

    @Override
    public void salvar(Lancamento lancamento) {
        try {
            LancamentoDocument document = mapper.toDocument(lancamento);
            container.createItem(document, new PartitionKey(lancamento.accountId()), new CosmosItemRequestOptions());
        } catch (CosmosException exception) {
            throw new LancamentoPersistenciaException("Falha ao persistir o lançamento.", exception);
        }
    }

    @Override
    public Optional<Lancamento> buscarPorId(String accountId, String id) {
        requireText(accountId, "accountId");
        requireText(id, "id");
        try {
            LancamentoDocument document = container
                    .readItem(id, new PartitionKey(accountId), LancamentoDocument.class)
                    .getItem();
            return Optional.ofNullable(document).map(mapper::toDomain);
        } catch (CosmosException exception) {
            if (exception.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new LancamentoPersistenciaException("Falha ao consultar o lançamento.", exception);
        }
    }

    @Override
    public PaginaLancamentos<Lancamento> listar(String accountId, int page, int pageSize) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O accountId é obrigatório.");
        }
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Os parâmetros de paginação são inválidos.");
        }

        int offset;
        try {
            offset = Math.multiplyExact(page, pageSize);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("A página solicitada excede o limite suportado.", exception);
        }
        var options = new CosmosQueryRequestOptions().setPartitionKey(new PartitionKey(accountId));
        SqlQuerySpec pageQuery = new SqlQuerySpec(
                "SELECT * FROM c WHERE c.accountId = @accountId ORDER BY c.data DESC, c.id ASC "
                        + "OFFSET @offset LIMIT @pageSize",
                List.of(
                        new SqlParameter("@accountId", accountId),
                        new SqlParameter("@offset", offset),
                        new SqlParameter("@pageSize", pageSize)));
        SqlQuerySpec countQuery = new SqlQuerySpec(
                COUNT_QUERY,
                List.of(new SqlParameter("@accountId", accountId)));
        try {
            List<Lancamento> items = container.queryItems(pageQuery, options, LancamentoDocument.class)
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
            long totalItems = container.queryItems(countQuery, options, Long.class)
                    .stream()
                    .findFirst()
                    .orElse(0L);
            return new PaginaLancamentos<>(items, page, pageSize, totalItems);
        } catch (CosmosException exception) {
            throw new LancamentoPersistenciaException("Falha ao listar lançamentos.", exception);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " é obrigatório.");
        }
    }
}
