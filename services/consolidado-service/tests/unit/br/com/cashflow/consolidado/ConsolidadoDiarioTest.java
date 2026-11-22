package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ConsolidadoDiarioTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Test
    void rejeitaIdentidadeEValoresObrigatoriosAusentes() {
        assertThrows(IllegalArgumentException.class, () -> consolidado(null, DATE, ZERO, ZERO, ZERO));
        assertThrows(IllegalArgumentException.class, () -> consolidado(" ", DATE, ZERO, ZERO, ZERO));
        assertThrows(IllegalArgumentException.class, () -> consolidado("conta-1", null, ZERO, ZERO, ZERO));
        assertThrows(IllegalArgumentException.class, () -> consolidado("conta-1", DATE, null, ZERO, ZERO));
        assertThrows(IllegalArgumentException.class, () -> consolidado("conta-1", DATE, ZERO, null, ZERO));
        assertThrows(IllegalArgumentException.class, () -> consolidado("conta-1", DATE, ZERO, ZERO, null));
    }

    @Test
    void rejeitaValoresNegativosESaldoInconsistente() {
        assertThrows(IllegalArgumentException.class, () -> consolidado(
                "conta-1", DATE, new BigDecimal("-1.00"), ZERO, new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> consolidado(
                "conta-1", DATE, ZERO, new BigDecimal("-1.00"), BigDecimal.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> consolidado(
                "conta-1", DATE, BigDecimal.ONE, ZERO, ZERO));
    }

    @Test
    void rejeitaValoresComPrecisaoIncompativelComMoeda() {
        assertThrows(ArithmeticException.class, () -> consolidado(
                "conta-1", DATE, new BigDecimal("1.001"), ZERO, new BigDecimal("1.001")));
    }

    private ConsolidadoDiario consolidado(
            String accountId, LocalDate date, BigDecimal credits, BigDecimal debits, BigDecimal balance) {
        return new ConsolidadoDiario(accountId, date, credits, debits, balance);
    }
}
