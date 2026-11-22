package br.com.cashflow.lancamentos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record ValorMonetario(BigDecimal valor) {

    public ValorMonetario {
        Objects.requireNonNull(valor, "O valor monetário é obrigatório.");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor monetário deve ser maior que zero.");
        }
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("O valor monetário deve ter no máximo duas casas decimais.", exception);
        }
    }
}
