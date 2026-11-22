package br.com.cashflow.consolidado.interfaces.api;

import br.com.cashflow.consolidado.application.usecase.ConsultarConsolidadoUseCase;
import br.com.cashflow.consolidado.application.query.ConsultarConsolidadoPorDataQuery;
import br.com.cashflow.consolidado.interfaces.api.generated.ConsolidadoApi;
import br.com.cashflow.consolidado.interfaces.response.ConsolidadoApiMapper;
import br.com.cashflow.consolidado.interfaces.response.ConsolidadoDiarioResponse;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsolidadoController implements ConsolidadoApi {

    private final ConsultarConsolidadoUseCase consultarConsolidadoUseCase;
    private final ConsolidadoApiMapper consolidadoApiMapper;

    public ConsolidadoController(
            ConsultarConsolidadoUseCase consultarConsolidadoUseCase,
            ConsolidadoApiMapper consolidadoApiMapper) {
        this.consultarConsolidadoUseCase = consultarConsolidadoUseCase;
        this.consolidadoApiMapper = consolidadoApiMapper;
    }

    @Override
    public ResponseEntity<ConsolidadoDiarioResponse> consultarConsolidadoAtual(String accountId) {
        return ResponseEntity.ok(
                consolidadoApiMapper.toResponse(consultarConsolidadoUseCase.consultarAtual(accountId)));
    }

    @Override
    public ResponseEntity<ConsolidadoDiarioResponse> consultarConsolidadoPorData(LocalDate data, String accountId) {
        return ResponseEntity.ok(
                consolidadoApiMapper.toResponse(consultarConsolidadoUseCase
                        .consultarPorData(new ConsultarConsolidadoPorDataQuery(accountId, data))));
    }
}
