package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.consolidado.application.event.ProcessarLancamentoCriadoEvent;
import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProcessarLancamentoCriadoEventTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 20);
    private static final Instant TIMESTAMP = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void normalizaValorComNoMaximoDuasCasas() {
        assertEquals(new BigDecimal("10.00"), event(
                "event-1", "account-1", DATE, TipoLancamento.CREDITO, new BigDecimal("10"),
                "correlation-1", "causation-1", TIMESTAMP).valor());
    }

    @Test
    void exigeIdentificadoresDeEventoContaCorrelacaoECausacao() {
        assertThrows(IllegalArgumentException.class, () -> event(null, "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event(" ", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", null, DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", " ", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, null, "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, " ", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", null, TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", " ", TIMESTAMP));
    }

    @Test
    void exigeDataTipoTimestampEValorPositivo() {
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", null,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                null, BigDecimal.ONE, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE, "correlation-1", "causation-1", null));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, null, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ZERO, "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, BigDecimal.ONE.negate(), "correlation-1", "causation-1", TIMESTAMP));
        assertThrows(IllegalArgumentException.class, () -> event("event-1", "account-1", DATE,
                TipoLancamento.CREDITO, new BigDecimal("1.001"), "correlation-1", "causation-1", TIMESTAMP));
    }

    private ProcessarLancamentoCriadoEvent event(
            String eventId,
            String accountId,
            LocalDate data,
            TipoLancamento tipo,
            BigDecimal valor,
            String correlationId,
            String causationId,
            Instant timestamp) {
        return new ProcessarLancamentoCriadoEvent(
                eventId, accountId, data, tipo, valor, null, correlationId, causationId, timestamp);
    }
}
