package br.com.cashflow.lancamentos.domain.model;

import java.util.List;
import java.util.Objects;

public record PaginaLancamentos<T>(List<T> items, int page, int pageSize, long totalItems) {

    public PaginaLancamentos {
        items = List.copyOf(Objects.requireNonNull(items, "A lista de itens é obrigatória."));
        if (page < 0 || pageSize < 1 || totalItems < 0) {
            throw new IllegalArgumentException("Os metadados da página são inválidos.");
        }
    }

    public int totalPages() {
        return totalItems == 0 ? 0 : (int) ((totalItems + pageSize - 1) / pageSize);
    }
}
