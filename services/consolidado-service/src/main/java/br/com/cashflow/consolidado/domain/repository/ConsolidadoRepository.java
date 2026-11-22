package br.com.cashflow.consolidado.domain.repository;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import java.time.LocalDate;
import java.util.Optional;

public interface ConsolidadoRepository {

    Optional<ConsolidadoDiario> buscarAtual(String accountId);

    Optional<ConsolidadoDiario> buscarPorData(String accountId, LocalDate data);

    void salvar(ConsolidadoDiario consolidado);

    boolean aplicar(ProcessarLancamentoCriadoEvent evento);
}
