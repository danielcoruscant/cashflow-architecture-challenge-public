package br.com.cashflow.lancamentos.domain.model;

public record Lancamento(
        String id,
        String accountId,
        TipoLancamento tipo,
        ValorMonetario valor,
        DataLancamento data,
        String correlationId,
        String causationId,
        String compensaLancamentoId) {

    public Lancamento {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O identificador do lançamento é obrigatório.");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O accountId do lançamento é obrigatório.");
        }
        if (tipo == null || valor == null || data == null) {
            throw new IllegalArgumentException("Tipo, valor e data do lançamento são obrigatórios.");
        }
        if (correlationId == null || correlationId.isBlank()
                || causationId == null || causationId.isBlank()) {
            throw new IllegalArgumentException("CorrelationId e causationId são obrigatórios.");
        }
        if (id.equals(compensaLancamentoId)) {
            throw new IllegalArgumentException("Um lançamento não pode compensar a si próprio.");
        }
    }
}
