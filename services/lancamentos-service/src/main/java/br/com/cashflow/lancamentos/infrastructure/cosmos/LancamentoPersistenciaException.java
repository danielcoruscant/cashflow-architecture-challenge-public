package br.com.cashflow.lancamentos.infrastructure.cosmos;

public class LancamentoPersistenciaException extends RuntimeException {

    public LancamentoPersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
