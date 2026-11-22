package br.com.cashflow.lancamentos.application.usecase;

import br.com.cashflow.lancamentos.application.command.CompensarLancamentoCommand;
import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.domain.exception.LancamentoNaoEncontradoException;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import br.com.cashflow.lancamentos.domain.service.LancamentoDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class CompensarLancamentoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompensarLancamentoUseCase.class);

    private final LancamentoRepository lancamentoRepository;
    private final LancamentoDomainService lancamentoDomainService;

    public CompensarLancamentoUseCase(
            LancamentoRepository lancamentoRepository,
            LancamentoDomainService lancamentoDomainService) {
        this.lancamentoRepository = lancamentoRepository;
        this.lancamentoDomainService = lancamentoDomainService;
    }

    @SuppressWarnings("unused")
    public LancamentoDto executar(CompensarLancamentoCommand command) {
        var original = lancamentoRepository.buscarPorId(command.accountId(), command.lancamentoId())
                .orElseThrow(() -> new LancamentoNaoEncontradoException(command.lancamentoId()));
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", original.correlationId())) {
            var compensacao = lancamentoDomainService.criarCompensacao(original);
            lancamentoRepository.salvar(compensacao);
            LOGGER.info("Lançamento compensatório persistido id={} originalId={}",
                    compensacao.id(), original.id());
            return LancamentoDto.from(compensacao);
        }
    }
}
