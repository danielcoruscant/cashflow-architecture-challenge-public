package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LancamentoTest {

    private static final TipoLancamento TIPO = TipoLancamento.CREDITO;
    private static final ValorMonetario VALOR = new ValorMonetario(new BigDecimal("10.00"));
    private static final DataLancamento DATA = new DataLancamento(LocalDate.of(2026, 11, 20));

    @Test
    void rejeitaIdentificadoresObrigatoriosAusentesOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> criar(null, "conta-1", TIPO, VALOR, DATA,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar(" ", "conta-1", TIPO, VALOR, DATA,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", null, TIPO, VALOR, DATA,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", " ", TIPO, VALOR, DATA,
                "correlation-1", "causation-1", null));
    }

    @Test
    void rejeitaDadosFinanceirosEIdentificadoresDeCorrelacaoAusentes() {
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", null, VALOR, DATA,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, null, DATA,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, null,
                "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, DATA,
                null, "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, DATA,
                " ", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, DATA,
                "correlation-1", null, null));
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, DATA,
                "correlation-1", " ", null));
    }

    @Test
    void naoPermiteCompensarOProprioLancamento() {
        assertThrows(IllegalArgumentException.class, () -> criar("id", "conta-1", TIPO, VALOR, DATA,
                "correlation-1", "causation-1", "id"));
    }

    private Lancamento criar(
            String id,
            String accountId,
            TipoLancamento tipo,
            ValorMonetario valor,
            DataLancamento data,
            String correlationId,
            String causationId,
            String compensaLancamentoId) {
        return new Lancamento(
                id, accountId, tipo, valor, data, correlationId, causationId, compensaLancamentoId);
    }
}
