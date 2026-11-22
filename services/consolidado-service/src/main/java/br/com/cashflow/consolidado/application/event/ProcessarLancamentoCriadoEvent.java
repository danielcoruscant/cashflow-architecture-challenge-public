package br.com.cashflow.consolidado.application.event;

import br.com.cashflow.consolidado.domain.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProcessarLancamentoCriadoEvent(
        String eventId,
        String accountId,
        LocalDate data,
        TipoLancamento tipo,
        BigDecimal valor,
        String compensaLancamentoId,
        String correlationId,
        String causationId,
        Instant timestamp) {

    public ProcessarLancamentoCriadoEvent {
        requireText(eventId, "eventId");
        requireText(accountId, "accountId");
        requireText(correlationId, "correlationId");
        requireText(causationId, "causationId");
        if (data == null || tipo == null || timestamp == null) {
            throw new IllegalArgumentException("Data, tipo e timestamp do evento são obrigatórios.");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do evento deve ser maior que zero.");
        }
        try {
            valor = valor.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("O valor do evento deve ter no máximo duas casas decimais.", exception);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O campo " + field + " do evento é obrigatório.");
        }
    }
}
