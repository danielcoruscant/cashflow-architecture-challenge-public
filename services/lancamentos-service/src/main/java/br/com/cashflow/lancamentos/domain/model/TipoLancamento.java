package br.com.cashflow.lancamentos.domain.model;

public enum TipoLancamento {
    CREDITO,
    DEBITO;

    public TipoLancamento oposto() {
        return this == CREDITO ? DEBITO : CREDITO;
    }
}
