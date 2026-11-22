package br.com.cashflow.consolidado.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsolidadoDiario(
        String accountId,
        LocalDate data,
        BigDecimal creditos,
        BigDecimal debitos,
        BigDecimal saldo) {

    public ConsolidadoDiario {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O accountId do consolidado é obrigatório.");
        }
        if (data == null || creditos == null || debitos == null || saldo == null) {
            throw new IllegalArgumentException("Data e valores do consolidado são obrigatórios.");
        }
        creditos = creditos.setScale(2);
        debitos = debitos.setScale(2);
        saldo = saldo.setScale(2);
        if (creditos.signum() < 0 || debitos.signum() < 0 || saldo.compareTo(creditos.subtract(debitos)) != 0) {
            throw new IllegalArgumentException("Os valores do consolidado são inconsistentes.");
        }
    }
}
