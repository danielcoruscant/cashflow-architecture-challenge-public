package br.com.cashflow.lancamentos.interfaces.api;

import br.com.cashflow.lancamentos.application.command.CompensarLancamentoCommand;
import br.com.cashflow.lancamentos.application.query.ConsultarLancamentoQuery;
import br.com.cashflow.lancamentos.application.usecase.CompensarLancamentoUseCase;
import br.com.cashflow.lancamentos.application.usecase.ConsultarLancamentoUseCase;
import br.com.cashflow.lancamentos.application.usecase.CriarLancamentoUseCase;
import br.com.cashflow.lancamentos.application.usecase.ListarLancamentosUseCase;
import br.com.cashflow.lancamentos.interfaces.api.generated.LancamentosApi;
import br.com.cashflow.lancamentos.interfaces.request.CriarLancamentoRequest;
import br.com.cashflow.lancamentos.interfaces.response.LancamentoResponse;
import br.com.cashflow.lancamentos.interfaces.response.PaginaLancamentosResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LancamentoController implements LancamentosApi {

    private final CriarLancamentoUseCase criarLancamentoUseCase;
    private final CompensarLancamentoUseCase compensarLancamentoUseCase;
    private final ConsultarLancamentoUseCase consultarLancamentoUseCase;
    private final ListarLancamentosUseCase listarLancamentosUseCase;
    private final LancamentoApiMapper lancamentoApiMapper;

    public LancamentoController(
            CriarLancamentoUseCase criarLancamentoUseCase,
            CompensarLancamentoUseCase compensarLancamentoUseCase,
            ConsultarLancamentoUseCase consultarLancamentoUseCase,
            ListarLancamentosUseCase listarLancamentosUseCase,
            LancamentoApiMapper lancamentoApiMapper) {
        this.criarLancamentoUseCase = criarLancamentoUseCase;
        this.compensarLancamentoUseCase = compensarLancamentoUseCase;
        this.consultarLancamentoUseCase = consultarLancamentoUseCase;
        this.listarLancamentosUseCase = listarLancamentosUseCase;
        this.lancamentoApiMapper = lancamentoApiMapper;
    }

    @Override
    public ResponseEntity<LancamentoResponse> criarLancamento(CriarLancamentoRequest criarLancamentoRequest) {
        var lancamento = criarLancamentoUseCase.executar(lancamentoApiMapper.toCommand(criarLancamentoRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoApiMapper.toResponse(lancamento));
    }

    @Override
    public ResponseEntity<PaginaLancamentosResponse> listarLancamentos(String accountId, Integer page, Integer pageSize) {
        return ResponseEntity.ok(lancamentoApiMapper.toResponse(
                listarLancamentosUseCase.executar(accountId, page, pageSize)));
    }

    @Override
    public ResponseEntity<LancamentoResponse> consultarLancamento(String id, String accountId) {
        var lancamento = consultarLancamentoUseCase.executar(new ConsultarLancamentoQuery(accountId, id));
        return ResponseEntity.ok(lancamentoApiMapper.toResponse(lancamento));
    }

    @Override
    public ResponseEntity<LancamentoResponse> compensarLancamento(String id, String accountId) {
        var lancamento = compensarLancamentoUseCase.executar(new CompensarLancamentoCommand(accountId, id));
        return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoApiMapper.toResponse(lancamento));
    }
}
