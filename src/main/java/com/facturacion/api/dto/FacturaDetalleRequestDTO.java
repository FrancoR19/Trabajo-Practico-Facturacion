package com.facturacion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaDetalleRequestDTO {

    private Long articuloId;
    private Long listaPrecioArticuloId;
    private Double cantidad;
    private Double porcentajeBonificacion;
    private String descripcion;
}
