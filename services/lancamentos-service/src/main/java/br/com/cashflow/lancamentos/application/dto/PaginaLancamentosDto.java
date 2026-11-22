package br.com.cashflow.lancamentos.application.dto;

import java.util.List;

public record PaginaLancamentosDto(
        List<LancamentoDto> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}
