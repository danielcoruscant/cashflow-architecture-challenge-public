package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.infrastructure.mapper.LancamentoDocumentMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LancamentoDocumentMapperTest {

    private final LancamentoDocumentMapper mapper = new LancamentoDocumentMapper();

    @Test
    void deveConverterLancamentoParaDocumentoEDeVolta() {
        var lancamento = new Lancamento(
                "abc",
                "conta-1",
                TipoLancamento.CREDITO,
                new ValorMonetario(new BigDecimal("42.50")),
                new DataLancamento(LocalDate.of(2026, 11, 20)),
                "correlation-1",
                "causation-1",
                null);

        assertEquals(lancamento, mapper.toDomain(mapper.toDocument(lancamento)));
    }
}
