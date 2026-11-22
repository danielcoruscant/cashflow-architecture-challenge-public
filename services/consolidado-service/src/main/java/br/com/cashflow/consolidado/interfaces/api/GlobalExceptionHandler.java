package br.com.cashflow.consolidado.interfaces.api;

import br.com.cashflow.consolidado.domain.exception.ConsolidadoNaoEncontradoException;
import br.com.cashflow.consolidado.infrastructure.cosmos.ConsolidadoPersistenciaException;
import br.com.cashflow.consolidado.interfaces.response.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConsolidadoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleConsolidadoNaoEncontrado(
            ConsolidadoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse().codigo("CONSOLIDADO_NAO_ENCONTRADO").mensagem(exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErroResponse> handleOperacaoNaoImplementada(UnsupportedOperationException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse().codigo("ERRO_INTERNO").mensagem(exception.getMessage()));
    }

    @ExceptionHandler(ConsolidadoPersistenciaException.class)
    public ResponseEntity<ErroResponse> handleFalhaDePersistencia(ConsolidadoPersistenciaException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse().codigo("ERRO_PERSISTENCIA").mensagem(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleParametroInvalido(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse()
                        .codigo("PARAMETRO_INVALIDO")
                        .mensagem("Parâmetro inválido: " + exception.getName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleRegraDeDominioInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse().codigo("REQUISICAO_INVALIDA").mensagem(exception.getMessage()));
    }
}
