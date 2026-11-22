package br.com.cashflow.lancamentos.application.command;

import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarLancamentoCommand(String accountId, TipoLancamento tipo, BigDecimal valor, LocalDate data) {
}
