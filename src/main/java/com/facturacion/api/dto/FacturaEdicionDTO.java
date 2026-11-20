package com.facturacion.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record FacturaEdicionDTO(
        Long id,
        Long numero,
        LocalDate fechaEmision,
        Long clienteId,
        String clienteDenominacion,
        Long condicionIvaId,
        Long puntoVentaId,
        Long tipoMonedaId,
        double importeCobrado,
        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String observaciones,
        String estado,
        boolean informadaArca,
        List<FacturaEdicionDetalleDTO> detalles) {
}
