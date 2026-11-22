package br.com.cashflow.lancamentos.application.usecase;

import br.com.cashflow.lancamentos.application.command.CriarLancamentoCommand;
import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class CriarLancamentoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CriarLancamentoUseCase.class);

    private final LancamentoRepository lancamentoRepository;

    public CriarLancamentoUseCase(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @SuppressWarnings("unused")
    public LancamentoDto executar(CriarLancamentoCommand command) {
        String correlationId = UUID.randomUUID().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            Lancamento lancamento = new Lancamento(
                    UUID.randomUUID().toString(),
                    command.accountId(),
                    command.tipo(),
                    new ValorMonetario(command.valor()),
                    new DataLancamento(command.data()),
                    correlationId,
                    correlationId,
                    null);
            lancamentoRepository.salvar(lancamento);
            LOGGER.info("Lançamento persistido id={}", lancamento.id());
            return LancamentoDto.from(lancamento);
        }
    }
}
