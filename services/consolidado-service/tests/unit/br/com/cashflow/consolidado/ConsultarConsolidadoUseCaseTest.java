package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cashflow.consolidado.application.query.ConsultarConsolidadoPorDataQuery;
import br.com.cashflow.consolidado.application.usecase.ConsultarConsolidadoUseCase;
import br.com.cashflow.consolidado.domain.exception.ConsolidadoNaoEncontradoException;
import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.repository.ConsolidadoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarConsolidadoUseCaseTest {

    @Mock
    private ConsolidadoRepository consolidadoRepository;

    @InjectMocks
    private ConsultarConsolidadoUseCase consultarConsolidadoUseCase;

    @Test
    void deveConsultarConsolidadoPelaDataInformada() {
        LocalDate data = LocalDate.of(2026, 11, 20);
        ConsolidadoDiario consolidado = new ConsolidadoDiario(
                "conta-1",
                data,
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("75.00"));
        when(consolidadoRepository.buscarPorData("conta-1", data)).thenReturn(Optional.of(consolidado));

        ConsolidadoDiario resultado = consultarConsolidadoUseCase
                .consultarPorData(new ConsultarConsolidadoPorDataQuery("conta-1", data));

        assertSame(consolidado, resultado);
        verify(consolidadoRepository).buscarPorData("conta-1", data);
    }

    @Test
    void deveInformarQuandoConsolidadoNaoExisteParaData() {
        LocalDate data = LocalDate.of(2026, 11, 20);
        when(consolidadoRepository.buscarPorData("conta-1", data)).thenReturn(Optional.empty());

        assertThrows(ConsolidadoNaoEncontradoException.class,
                () -> consultarConsolidadoUseCase.consultarPorData(new ConsultarConsolidadoPorDataQuery("conta-1", data)));
    }
}
