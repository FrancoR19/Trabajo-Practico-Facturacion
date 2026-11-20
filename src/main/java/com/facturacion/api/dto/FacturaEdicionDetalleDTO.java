package com.facturacion.api.dto;

public record FacturaEdicionDetalleDTO(
        Long listaPrecioArticuloId,
        String listaPrecio,
        String articuloCodigo,
        String articuloDenominacion,
        double precioUnitario,
        double cantidad,
        double porcentajeBonificacion) {
}
