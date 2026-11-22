package br.com.cashflow.consolidado.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsolidadoDocument(
        String id,
        String accountId,
        String tipoDocumento,
        LocalDate data,
        BigDecimal creditos,
        BigDecimal debitos,
        BigDecimal saldo) {
}
