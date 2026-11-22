package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import br.com.cashflow.consolidado.domain.service.ConsolidadoDomainService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ConsolidadoDomainServiceTest {

    private final ConsolidadoDomainService service = new ConsolidadoDomainService();
    private final LocalDate data = LocalDate.of(2026, 11, 20);

    @Test
    void separaContaEDiaDeNegocio() {
        ConsolidadoDiario contaUm = service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, new BigDecimal("10.50"));
        ConsolidadoDiario contaDois = service.aplicarLancamento(
                null, "conta-2", data, TipoLancamento.DEBITO, new BigDecimal("3.25"));

        assertEquals(new BigDecimal("10.50"), contaUm.saldo());
        assertEquals(new BigDecimal("-3.25"), contaDois.saldo());
        assertEquals("conta-1", contaUm.accountId());
        assertEquals("conta-2", contaDois.accountId());
    }

    @Test
    void aplicacaoDaCompensacaoInverteOImpactoEArredondaValoresExatos() {
        ConsolidadoDiario credito = service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, new BigDecimal("10.5"));
        ConsolidadoDiario compensado = service.aplicarLancamento(
                credito, "conta-1", data, TipoLancamento.DEBITO, new BigDecimal("10.50"));

        assertEquals(new BigDecimal("10.50"), compensado.creditos());
        assertEquals(new BigDecimal("10.50"), compensado.debitos());
        assertEquals(new BigDecimal("0.00"), compensado.saldo());
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, new BigDecimal("1.005")));
    }

    @Test
    void naoPermiteMisturarDatasOuContasNoMesmoSaldoDiario() {
        ConsolidadoDiario existente = service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, new BigDecimal("1.00"));

        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                existente, "conta-1", data.plusDays(1), TipoLancamento.CREDITO, new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                existente, "conta-2", data, TipoLancamento.CREDITO, new BigDecimal("1.00")));
    }

    @Test
    void rejeitaDadosInvalidosAntesDeAlterarProjecao() {
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                null, null, data, TipoLancamento.CREDITO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                null, " ", data, TipoLancamento.CREDITO, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> service.aplicarLancamento(
                null, "conta-1", null, TipoLancamento.CREDITO, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> service.aplicarLancamento(
                null, "conta-1", data, null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, null));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarLancamento(
                null, "conta-1", data, TipoLancamento.CREDITO, BigDecimal.ONE.negate()));
    }
}
