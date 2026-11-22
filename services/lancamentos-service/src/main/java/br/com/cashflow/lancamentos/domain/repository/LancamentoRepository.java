package br.com.cashflow.lancamentos.domain.repository;

import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.PaginaLancamentos;
import java.util.Optional;

public interface LancamentoRepository {

    void salvar(Lancamento lancamento);

    Optional<Lancamento> buscarPorId(String accountId, String id);

    PaginaLancamentos<Lancamento> listar(String accountId, int page, int pageSize);
}
