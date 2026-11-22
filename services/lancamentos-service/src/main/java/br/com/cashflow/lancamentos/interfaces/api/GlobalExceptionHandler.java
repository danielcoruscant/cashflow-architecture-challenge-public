package br.com.cashflow.lancamentos.interfaces.api;

import br.com.cashflow.lancamentos.domain.exception.LancamentoNaoEncontradoException;
import br.com.cashflow.lancamentos.infrastructure.cosmos.LancamentoPersistenciaException;
import br.com.cashflow.lancamentos.interfaces.response.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LancamentoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleLancamentoNaoEncontrado(LancamentoNaoEncontradoException exception) {
        return erro(HttpStatus.NOT_FOUND, "LANCAMENTO_NAO_ENCONTRADO", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErroResponse> handleRequisicaoInvalida(Exception exception) {
        return erro(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Requisição inválida.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleRegraDeDominioInvalida(IllegalArgumentException exception) {
        return erro(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", exception.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErroResponse> handleOperacaoNaoImplementada(UnsupportedOperationException exception) {
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", exception.getMessage());
    }

    @ExceptionHandler(LancamentoPersistenciaException.class)
    public ResponseEntity<ErroResponse> handleFalhaDePersistencia(LancamentoPersistenciaException exception) {
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_PERSISTENCIA", exception.getMessage());
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String codigo, String mensagem) {
        HttpStatus statusSeguro = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        return ResponseEntity.status(statusSeguro.value()).body(new ErroResponse(codigo, mensagem));
    }
}
