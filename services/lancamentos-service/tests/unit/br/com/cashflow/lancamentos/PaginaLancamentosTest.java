package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cashflow.lancamentos.domain.model.PaginaLancamentos;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaLancamentosTest {

    @Test
    void calculaTotalDePaginasIncluindoPaginaVazia() {
        assertEquals(0, new PaginaLancamentos<>(List.of(), 0, 10, 0).totalPages());
        assertEquals(3, new PaginaLancamentos<>(List.of("item"), 0, 10, 21).totalPages());
    }

    @Test
    void validaItensEMetadadosDaPagina() {
        assertThrows(NullPointerException.class, () -> new PaginaLancamentos<>(null, 0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new PaginaLancamentos<>(List.of(), -1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new PaginaLancamentos<>(List.of(), 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PaginaLancamentos<>(List.of(), 0, 10, -1));
    }
}
