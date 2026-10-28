package com.stockiq.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/** Página de resultados con el formato de la API (no se expone el Page de Spring). {@code pagina} empieza en 0. */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
