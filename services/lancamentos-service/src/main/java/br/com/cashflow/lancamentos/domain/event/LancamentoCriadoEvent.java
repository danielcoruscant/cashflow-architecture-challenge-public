package br.com.cashflow.lancamentos.domain.event;

import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LancamentoCriadoEvent(
        String id,
        LocalDate data,
        TipoLancamento tipo,
        BigDecimal valor,
        Instant timestamp) {
}
