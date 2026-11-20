package com.facturacion.api.dto;

public record ClienteDTO(
        Long id,
        String cuitCuil,
        String denominacion,
        Long condicionIvaId,
        String condicionIvaDenominacion,
        Long listaPrecioId,
        String listaPrecioDenominacion) {

    public ClienteDTO(Long id, String cuitCuil, String denominacion) {
        this(id, cuitCuil, denominacion, null, null, null, null);
    }

    public ClienteDTO(Long id, String cuitCuil, String denominacion,
                      Long condicionIvaId, String condicionIvaDenominacion) {
        this(id, cuitCuil, denominacion, condicionIvaId, condicionIvaDenominacion, null, null);
    }
}
