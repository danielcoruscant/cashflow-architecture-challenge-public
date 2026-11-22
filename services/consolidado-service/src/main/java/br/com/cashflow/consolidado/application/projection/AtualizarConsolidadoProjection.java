package br.com.cashflow.consolidado.application.projection;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.domain.repository.ConsolidadoRepository;
import org.springframework.stereotype.Component;

@Component
public class AtualizarConsolidadoProjection {

    private final ConsolidadoRepository consolidadoRepository;

    public AtualizarConsolidadoProjection(ConsolidadoRepository consolidadoRepository) {
        this.consolidadoRepository = consolidadoRepository;
    }

    public void atualizar(ProcessarLancamentoCriadoEvent evento) {
        consolidadoRepository.aplicar(evento);
    }
}
