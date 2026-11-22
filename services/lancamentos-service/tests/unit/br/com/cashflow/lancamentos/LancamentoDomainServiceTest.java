package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.domain.service.LancamentoDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LancamentoDomainServiceTest {

    private final LancamentoDomainService service = new LancamentoDomainService();

    @Test
    void compensacaoMantemContaDataEValorEInverteTipoSemAlterarOriginal() {
        var original = new Lancamento(
                "original",
                "conta-1",
                TipoLancamento.CREDITO,
                new ValorMonetario(new BigDecimal("42.50")),
                new DataLancamento(LocalDate.of(2026, 11, 20)),
                "correlation-1",
                "causation-1",
                null);

        Lancamento compensacao = service.criarCompensacao(original);

        assertEquals("conta-1", compensacao.accountId());
        assertEquals(TipoLancamento.DEBITO, compensacao.tipo());
        assertEquals(new BigDecimal("42.50"), compensacao.valor().valor());
        assertEquals(original.data(), compensacao.data());
        assertEquals("original", compensacao.compensaLancamentoId());
        assertEquals("correlation-1", compensacao.correlationId());
        assertEquals("original", compensacao.causationId());
        assertEquals(TipoLancamento.CREDITO, original.tipo());
        assertNull(original.compensaLancamentoId());
    }

    @Test
    void valorDeveSerPositivoERepresentavelEmDuasCasas() {
        assertEquals(new BigDecimal("10.00"), new ValorMonetario(new BigDecimal("10")).valor());
        assertThrows(IllegalArgumentException.class, () -> new ValorMonetario(new BigDecimal("0")));
        assertThrows(IllegalArgumentException.class, () -> new ValorMonetario(new BigDecimal("10.001")));
        assertThrows(NullPointerException.class, () -> new DataLancamento(null));
    }

    @Test
    void tipoLancamentoInverteCreditoEDebito() {
        assertEquals(TipoLancamento.DEBITO, TipoLancamento.CREDITO.oposto());
        assertEquals(TipoLancamento.CREDITO, TipoLancamento.DEBITO.oposto());
    }
}
