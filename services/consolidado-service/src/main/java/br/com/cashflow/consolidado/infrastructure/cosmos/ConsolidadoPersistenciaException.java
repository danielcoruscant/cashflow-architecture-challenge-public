package br.com.cashflow.consolidado.infrastructure.cosmos;

public class ConsolidadoPersistenciaException extends RuntimeException {

    public ConsolidadoPersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsolidadoPersistenciaException(String message) {
        super(message);
    }
}
