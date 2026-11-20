package com.facturacion.api.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaRequestDTO {

    private Long clienteId;
    private Boolean sinCliente;
    private Long condicionIvaId;
    private Long puntoVentaId;
    private Long tipoMonedaId;
    private LocalDate fechaEmision;
    private String estado;
    private Double importeCobrado;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

    private List<FacturaDetalleRequestDTO> detalles;
}
