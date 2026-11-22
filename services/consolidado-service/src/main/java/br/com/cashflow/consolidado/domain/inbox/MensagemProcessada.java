package br.com.cashflow.consolidado.domain.inbox;

import java.time.Instant;

public record MensagemProcessada(String id, String accountId, Instant processadaEm) {
}
