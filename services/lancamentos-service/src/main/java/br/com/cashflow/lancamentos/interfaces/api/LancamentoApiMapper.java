package br.com.cashflow.lancamentos.interfaces.api;

import br.com.cashflow.lancamentos.application.command.CriarLancamentoCommand;
import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.application.dto.PaginaLancamentosDto;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.interfaces.request.CriarLancamentoRequest;
import br.com.cashflow.lancamentos.interfaces.response.PaginaLancamentosResponse;
import br.com.cashflow.lancamentos.interfaces.response.LancamentoResponse;
import org.springframework.stereotype.Component;

@Component
public class LancamentoApiMapper {

    public CriarLancamentoCommand toCommand(CriarLancamentoRequest request) {
        return new CriarLancamentoCommand(
                request.getAccountId(),
                TipoLancamento.valueOf(request.getTipo().getValue()),
                request.getValor(),
                request.getData());
    }

    public LancamentoResponse toResponse(LancamentoDto dto) {
        return new LancamentoResponse(
                dto.id(),
                dto.accountId(),
                br.com.cashflow.lancamentos.interfaces.response.TipoLancamento.fromValue(dto.tipo().name()),
                dto.valor(),
                dto.data(),
                dto.compensaLancamentoId());
    }

    public PaginaLancamentosResponse toResponse(PaginaLancamentosDto dto) {
        return new PaginaLancamentosResponse(
                dto.items().stream().map(this::toResponse).toList(),
                dto.page(),
                dto.pageSize(),
                dto.totalItems(),
                dto.totalPages());
    }
}
