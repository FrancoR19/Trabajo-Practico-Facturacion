package com.facturacion.api.dto;

public record ClienteDTO(
        Long id,
        String cuitCuil,
        String denominacion,
        Long condicionIvaId,
        String condicionIvaDenominacion) {

    public ClienteDTO(Long id, String cuitCuil, String denominacion) {
        this(id, cuitCuil, denominacion, null, null);
    }
}
