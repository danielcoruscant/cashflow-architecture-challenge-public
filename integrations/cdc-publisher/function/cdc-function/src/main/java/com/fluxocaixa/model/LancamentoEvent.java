package com.fluxocaixa.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record LancamentoEvent(
        String eventId,
        String accountId,
        String data,
        String tipo,
        BigDecimal valor,
        String compensaLancamentoId,
        String correlationId,
        String causationId,
        String timestamp) {

    public static LancamentoEvent from(LancamentoChangeDocument document) {
        requireText(document.getId(), "id");
        requireText(document.getAccountId(), "accountId");
        requireText(document.getData(), "data");
        requireText(document.getTipo(), "tipo");
        requireText(document.getCorrelationId(), "correlationId");
        requireText(document.getCausationId(), "causationId");
        if (document.getValor() == null || document.getValor().signum() <= 0) {
            throw new IllegalArgumentException("Documento com valor inválido.");
        }
        if (document.getTimestampEpochSeconds() == null || document.getTimestampEpochSeconds() < 0) {
            throw new IllegalArgumentException("Documento sem o timestamp do Cosmos DB.");
        }

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(
                Instant.ofEpochSecond(document.getTimestampEpochSeconds()));
        return new LancamentoEvent(
                document.getId(),
                document.getAccountId(),
                document.getData(),
                document.getTipo(),
                document.getValor(),
                document.getCompensaLancamentoId(),
                document.getCorrelationId(),
                document.getCausationId(),
                timestamp);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Documento sem o campo obrigatório '" + field + "'.");
        }
    }
}
