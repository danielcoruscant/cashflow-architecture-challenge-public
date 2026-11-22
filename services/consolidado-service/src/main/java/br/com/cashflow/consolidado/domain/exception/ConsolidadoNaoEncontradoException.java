package br.com.cashflow.consolidado.domain.exception;

public class ConsolidadoNaoEncontradoException extends RuntimeException {

    public ConsolidadoNaoEncontradoException() {
        super("Consolidado diário não encontrado.");
    }
}
