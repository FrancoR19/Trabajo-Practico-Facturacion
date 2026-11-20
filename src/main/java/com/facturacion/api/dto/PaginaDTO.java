package com.facturacion.api.dto;

import java.util.List;

public record PaginaDTO<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

    private static final int TAMANIO_MAXIMO = 100;

    public static <T> PaginaDTO<T> de(List<T> contenido, int pagina, int tamanio, long totalElementos) {
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);
        return new PaginaDTO<>(contenido, pagina, tamanio, totalElementos, totalPaginas);
    }

    public static int normalizarPagina(int pagina) {
        return Math.max(pagina, 0);
    }

    public static int normalizarTamanio(int tamanio) {
        return Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
    }
}
