package br.com.cashflow.lancamentos.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record DataLancamento(LocalDate valor) {

    public DataLancamento {
        Objects.requireNonNull(valor, "A data do lançamento é obrigatória.");
    }
}
