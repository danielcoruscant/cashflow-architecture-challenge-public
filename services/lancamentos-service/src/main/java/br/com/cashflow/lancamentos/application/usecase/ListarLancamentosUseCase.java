package br.com.cashflow.lancamentos.application.usecase;

import br.com.cashflow.lancamentos.application.dto.PaginaLancamentosDto;
import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import org.springframework.stereotype.Service;

@Service
public class ListarLancamentosUseCase {

    private final LancamentoRepository lancamentoRepository;

    public ListarLancamentosUseCase(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    public PaginaLancamentosDto executar(String accountId, int page, int pageSize) {
        var resultado = lancamentoRepository.listar(accountId, page, pageSize);
        return new PaginaLancamentosDto(
                resultado.items().stream().map(LancamentoDto::from).toList(),
                resultado.page(),
                resultado.pageSize(),
                resultado.totalItems(),
                resultado.totalPages());
    }
}
