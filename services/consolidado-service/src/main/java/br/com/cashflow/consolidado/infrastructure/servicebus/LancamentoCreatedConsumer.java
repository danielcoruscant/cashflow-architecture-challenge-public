package br.com.cashflow.consolidado.infrastructure.servicebus;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.application.projection.AtualizarConsolidadoProjection;
import org.springframework.stereotype.Component;

@Component
public class LancamentoCreatedConsumer {

    private final AtualizarConsolidadoProjection projection;

    public LancamentoCreatedConsumer(AtualizarConsolidadoProjection projection) {
        this.projection = projection;
    }

    public void consumir(ProcessarLancamentoCriadoEvent evento) {
        projection.atualizar(evento);
    }
}
