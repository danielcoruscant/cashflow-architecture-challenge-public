package br.com.cashflow.consolidado.application.query;

import java.time.LocalDate;

public record ConsultarConsolidadoPorDataQuery(String accountId, LocalDate data) {
}
