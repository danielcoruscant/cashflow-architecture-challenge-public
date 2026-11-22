package br.com.cashflow.consolidado.domain.service;

import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ConsolidadoDomainService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public ConsolidadoDiario aplicarLancamento(
            ConsolidadoDiario atual,
            String accountId,
            LocalDate data,
            TipoLancamento tipo,
            BigDecimal valor) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O accountId é obrigatório.");
        }
        Objects.requireNonNull(data, "A data do lançamento é obrigatória.");
        Objects.requireNonNull(tipo, "O tipo do lançamento é obrigatório.");
        BigDecimal valorValidado = validarValor(valor);

        ConsolidadoDiario base = atual == null
                ? new ConsolidadoDiario(accountId, data, ZERO, ZERO, ZERO)
                : atual;
        if (!base.accountId().equals(accountId) || !base.data().equals(data)) {
            throw new IllegalArgumentException("O lançamento não pertence à conta e data do consolidado.");
        }

        BigDecimal creditos = base.creditos();
        BigDecimal debitos = base.debitos();
        if (tipo == TipoLancamento.CREDITO) {
            creditos = creditos.add(valorValidado);
        } else {
            debitos = debitos.add(valorValidado);
        }
        return new ConsolidadoDiario(accountId, data, creditos, debitos, creditos.subtract(debitos));
    }

    private BigDecimal validarValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "O valor do lançamento é obrigatório.");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do lançamento deve ser maior que zero.");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("O valor do lançamento deve ter no máximo duas casas decimais.", exception);
        }
    }
}
