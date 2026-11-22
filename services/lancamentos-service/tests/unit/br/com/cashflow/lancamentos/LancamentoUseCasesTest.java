package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cashflow.lancamentos.application.command.CriarLancamentoCommand;
import br.com.cashflow.lancamentos.application.command.CompensarLancamentoCommand;
import br.com.cashflow.lancamentos.application.dto.LancamentoDto;
import br.com.cashflow.lancamentos.application.query.ConsultarLancamentoQuery;
import br.com.cashflow.lancamentos.application.usecase.ConsultarLancamentoUseCase;
import br.com.cashflow.lancamentos.application.usecase.CriarLancamentoUseCase;
import br.com.cashflow.lancamentos.domain.exception.LancamentoNaoEncontradoException;
import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import br.com.cashflow.lancamentos.domain.service.LancamentoDomainService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import br.com.cashflow.lancamentos.application.usecase.CompensarLancamentoUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LancamentoUseCasesTest {

    private static final LocalDate DATA = LocalDate.of(2026, 11, 20);

    @Mock
    private LancamentoRepository lancamentoRepository;

    @Test
    void criarDevePersistirLancamentoComDadosDoComando() {
        var useCase = new CriarLancamentoUseCase(lancamentoRepository);
        var command = new CriarLancamentoCommand("conta-1", TipoLancamento.CREDITO, new BigDecimal("100.00"), DATA);
        Logger logger = (Logger) LoggerFactory.getLogger(CriarLancamentoUseCase.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        LancamentoDto resultado;
        try {
            resultado = useCase.executar(command);
        } finally {
            logger.detachAppender(appender);
        }

        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentoRepository).salvar(captor.capture());
        Lancamento salvo = captor.getValue();
        assertEquals(TipoLancamento.CREDITO, salvo.tipo());
        assertEquals("conta-1", salvo.accountId());
        assertEquals(new BigDecimal("100.00"), salvo.valor().valor());
        assertEquals(DATA, salvo.data().valor());
        assertEquals(salvo.correlationId(), salvo.causationId());
        assertEquals(salvo.id(), resultado.id());
        assertEquals(salvo.correlationId(), appender.list.get(0).getMDCPropertyMap().get("correlationId"));
    }

    @Test
    void consultarDeveRetornarLancamentoExistente() {
        var useCase = new ConsultarLancamentoUseCase(lancamentoRepository);
        var lancamento = new Lancamento(
                "abc", "conta-1", TipoLancamento.DEBITO,
                new ValorMonetario(new BigDecimal("10")), new DataLancamento(DATA),
                "correlation-1", "causation-1", null);
        when(lancamentoRepository.buscarPorId("conta-1", "abc")).thenReturn(Optional.of(lancamento));

        LancamentoDto resultado = useCase.executar(new ConsultarLancamentoQuery("conta-1", "abc"));

        assertEquals(
                new LancamentoDto("abc", "conta-1", TipoLancamento.DEBITO, new BigDecimal("10.00"), DATA, null),
                resultado);
    }

    @Test
    void consultarDeveFalharQuandoLancamentoNaoExiste() {
        var useCase = new ConsultarLancamentoUseCase(lancamentoRepository);
        when(lancamentoRepository.buscarPorId(any(), any())).thenReturn(Optional.empty());

        assertThrows(LancamentoNaoEncontradoException.class,
                () -> useCase.executar(new ConsultarLancamentoQuery("conta-1", "inexistente")));
    }

    @Test
    void compensarDeveCriarNovoLancamentoDeTipoOpostoReferenciandoOriginal() {
        var original = new Lancamento(
                "original", "conta-1", TipoLancamento.CREDITO,
                new ValorMonetario(new BigDecimal("25.00")), new DataLancamento(DATA),
                "correlation-1", "causation-1", null);
        when(lancamentoRepository.buscarPorId("conta-1", "original")).thenReturn(Optional.of(original));
        var useCase = new CompensarLancamentoUseCase(lancamentoRepository, new LancamentoDomainService());

        LancamentoDto resultado = useCase.executar(new CompensarLancamentoCommand("conta-1", "original"));

        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentoRepository).salvar(captor.capture());
        assertEquals(TipoLancamento.DEBITO, captor.getValue().tipo());
        assertEquals("original", captor.getValue().compensaLancamentoId());
        assertEquals("correlation-1", captor.getValue().correlationId());
        assertEquals("original", captor.getValue().causationId());
        assertEquals("conta-1", resultado.accountId());
        assertEquals(new BigDecimal("25.00"), resultado.valor());
    }

    @Test
    void compensarDeveFalharQuandoOriginalNaoExisteNaConta() {
        when(lancamentoRepository.buscarPorId("conta-1", "inexistente")).thenReturn(Optional.empty());
        var useCase = new CompensarLancamentoUseCase(lancamentoRepository, new LancamentoDomainService());

        assertThrows(
                LancamentoNaoEncontradoException.class,
                () -> useCase.executar(new CompensarLancamentoCommand("conta-1", "inexistente")));
    }
}
