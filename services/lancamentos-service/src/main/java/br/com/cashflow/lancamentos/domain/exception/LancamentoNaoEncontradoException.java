package br.com.cashflow.lancamentos.domain.exception;

public class LancamentoNaoEncontradoException extends RuntimeException {

    public LancamentoNaoEncontradoException(String id) {
        super("Lançamento não encontrado: " + id);
    }
}
