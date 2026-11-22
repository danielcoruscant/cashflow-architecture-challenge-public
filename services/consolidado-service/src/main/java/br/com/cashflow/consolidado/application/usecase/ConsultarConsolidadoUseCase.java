package br.com.cashflow.consolidado.application.usecase;

import br.com.cashflow.consolidado.application.query.ConsultarConsolidadoPorDataQuery;
import br.com.cashflow.consolidado.domain.exception.ConsolidadoNaoEncontradoException;
import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.repository.ConsolidadoRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsultarConsolidadoUseCase {

    private final ConsolidadoRepository consolidadoRepository;

    public ConsultarConsolidadoUseCase(ConsolidadoRepository consolidadoRepository) {
        this.consolidadoRepository = consolidadoRepository;
    }

    public ConsolidadoDiario consultarAtual(String accountId) {
        return consolidadoRepository.buscarAtual(accountId)
                .orElseThrow(ConsolidadoNaoEncontradoException::new);
    }

    public ConsolidadoDiario consultarPorData(ConsultarConsolidadoPorDataQuery query) {
        return consolidadoRepository.buscarPorData(query.accountId(), query.data())
                .orElseThrow(ConsolidadoNaoEncontradoException::new);
    }
}
