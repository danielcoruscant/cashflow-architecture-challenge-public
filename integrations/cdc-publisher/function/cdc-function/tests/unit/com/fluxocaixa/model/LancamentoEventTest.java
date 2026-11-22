package com.fluxocaixa.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LancamentoEventTest {

    @Test
    void deveConverterDocumentoDoCosmosEmEventoComTimestampIso() {
        LancamentoEvent event = LancamentoEvent.from(document());

        assertEquals("event-1", event.eventId());
        assertEquals("account-1", event.accountId());
        assertEquals(new BigDecimal("25.50"), event.valor());
        assertEquals("1970-01-01T00:00:10Z", event.timestamp());
    }

    @Test
    void deveRejeitarValorNuloOuNaoPositivo() {
        LancamentoChangeDocument withoutAmount = document();
        withoutAmount.setValor(null);
        LancamentoChangeDocument zeroAmount = document();
        zeroAmount.setValor(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> LancamentoEvent.from(withoutAmount));
        assertThrows(IllegalArgumentException.class, () -> LancamentoEvent.from(zeroAmount));
    }

    @Test
    void deveRejeitarTimestampNuloOuNegativo() {
        LancamentoChangeDocument withoutTimestamp = document();
        withoutTimestamp.setTimestampEpochSeconds(null);
        LancamentoChangeDocument negativeTimestamp = document();
        negativeTimestamp.setTimestampEpochSeconds(-1L);

        assertThrows(IllegalArgumentException.class, () -> LancamentoEvent.from(withoutTimestamp));
        assertThrows(IllegalArgumentException.class, () -> LancamentoEvent.from(negativeTimestamp));
    }

    private static LancamentoChangeDocument document() {
        LancamentoChangeDocument document = new LancamentoChangeDocument();
        document.setId("event-1");
        document.setAccountId("account-1");
        document.setData("2026-11-21");
        document.setTipo("CREDITO");
        document.setValor(new BigDecimal("25.50"));
        document.setCorrelationId("correlation-1");
        document.setCausationId("cause-1");
        document.setTimestampEpochSeconds(10L);
        return document;
    }
}
