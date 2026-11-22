package br.com.cashflow.lancamentos.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoDocument(
        String id,
        String accountId,
        String tipo,
        BigDecimal valor,
        LocalDate data,
        String correlationId,
        String causationId,
        String compensaLancamentoId) {
}
