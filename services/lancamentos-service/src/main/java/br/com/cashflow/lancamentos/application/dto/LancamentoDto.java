package br.com.cashflow.lancamentos.application.dto;

import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoDto(
        String id,
        String accountId,
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate data,
        String compensaLancamentoId) {

    public static LancamentoDto from(Lancamento lancamento) {
        return new LancamentoDto(
                lancamento.id(),
                lancamento.accountId(),
                lancamento.tipo(),
                lancamento.valor().valor(),
                lancamento.data().valor(),
                lancamento.compensaLancamentoId());
    }
}
