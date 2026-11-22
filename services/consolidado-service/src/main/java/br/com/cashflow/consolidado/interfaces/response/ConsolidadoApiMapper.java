package br.com.cashflow.consolidado.interfaces.response;

import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import org.springframework.stereotype.Component;

@Component
public class ConsolidadoApiMapper {

    public ConsolidadoDiarioResponse toResponse(ConsolidadoDiario consolidado) {
        return new ConsolidadoDiarioResponse()
                .accountId(consolidado.accountId())
                .data(consolidado.data())
                .creditos(consolidado.creditos())
                .debitos(consolidado.debitos())
                .saldo(consolidado.saldo());
    }
}
