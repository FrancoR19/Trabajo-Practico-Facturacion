package com.facturacion.api.dto;

public record ListaPrecioArticuloDTO(
        Long id,
        String listaPrecio,
        String articuloCodigo,
        String articuloDenominacion,
        double precioVenta) {
}
