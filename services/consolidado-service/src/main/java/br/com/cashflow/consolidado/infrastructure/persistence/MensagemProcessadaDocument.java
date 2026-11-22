package br.com.cashflow.consolidado.infrastructure.persistence;

import java.time.Instant;

public record MensagemProcessadaDocument(
        String id,
        String accountId,
        String tipoDocumento,
        Instant processadaEm) {
}
