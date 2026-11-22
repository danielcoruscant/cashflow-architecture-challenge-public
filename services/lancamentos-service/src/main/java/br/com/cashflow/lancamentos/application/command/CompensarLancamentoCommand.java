package br.com.cashflow.lancamentos.application.command;

public record CompensarLancamentoCommand(String accountId, String lancamentoId) {
}
