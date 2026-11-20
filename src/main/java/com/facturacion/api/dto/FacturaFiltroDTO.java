package com.facturacion.api.dto;

import java.time.LocalDate;

public record FacturaFiltroDTO(
        Long numero,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String cliente,
        Boolean informadaArca,
        String estado,
        Double montoMinimo) {

    public static FacturaFiltroDTO vacio() {
        return new FacturaFiltroDTO(null, null, null, null, null, null, null);
    }
}
