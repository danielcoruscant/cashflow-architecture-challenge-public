package br.com.cashflow.lancamentos.infrastructure.mapper;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.infrastructure.persistence.LancamentoDocument;
import org.springframework.stereotype.Component;

@Component
public class LancamentoDocumentMapper {

    public LancamentoDocument toDocument(Lancamento lancamento) {
        return new LancamentoDocument(
                lancamento.id(),
                lancamento.accountId(),
                lancamento.tipo().name(),
                lancamento.valor().valor(),
                lancamento.data().valor(),
                lancamento.correlationId(),
                lancamento.causationId(),
                lancamento.compensaLancamentoId());
    }

    public Lancamento toDomain(LancamentoDocument document) {
        return new Lancamento(
                document.id(),
                document.accountId(),
                TipoLancamento.valueOf(document.tipo()),
                new ValorMonetario(document.valor()),
                new DataLancamento(document.data()),
                document.correlationId(),
                document.causationId(),
                document.compensaLancamentoId());
    }
}
