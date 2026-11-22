package br.com.cashflow.lancamentos.application.usecase;

import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.application.query.ConsultarLancamentoQuery;
import br.com.cashflow.lancamentos.domain.exception.LancamentoNaoEncontradoException;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsultarLancamentoUseCase {

    private final LancamentoRepository lancamentoRepository;

    public ConsultarLancamentoUseCase(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    public LancamentoDto executar(ConsultarLancamentoQuery query) {
        return lancamentoRepository.buscarPorId(query.accountId(), query.id())
                .map(LancamentoDto::from)
                .orElseThrow(() -> new LancamentoNaoEncontradoException(query.id()));
    }
}
