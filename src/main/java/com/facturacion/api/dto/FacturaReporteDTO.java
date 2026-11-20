package com.facturacion.api.dto;

import com.facturacion.api.modelo.EstadoFactura;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FacturaReporteDTO {

    private Long id;
    private Long numeroFactura;
    private LocalDate fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private String tipoComprobante;
    private double importeTotal;
    private String estado;
    private boolean informadaArca;
    private long cantidadItems;
    private String codigoMoneda;

    public FacturaReporteDTO(
            Long id,
            Long numeroFactura,
            LocalDate fechaEmision,
            String clienteDenominacion,
            String condicionIva,
            String puntoVentaDescripcion,
            String tipoComprobante,
            double importeTotal,
            EstadoFactura estado,
            boolean informadaArca,
            long cantidadItems,
            String codigoMoneda) {
        this.id = id;
        this.numeroFactura = numeroFactura;
        this.fechaEmision = fechaEmision;
        this.clienteDenominacion = clienteDenominacion;
        this.condicionIva = condicionIva;
        this.puntoVentaDescripcion = puntoVentaDescripcion;
        this.tipoComprobante = tipoComprobante;
        this.importeTotal = importeTotal;
        this.estado = estado != null ? estado.name() : null;
        this.informadaArca = informadaArca;
        this.cantidadItems = cantidadItems;
        this.codigoMoneda = codigoMoneda;
    }

    public FacturaReporteDTO(
            Long id,
            Long numeroFactura,
            LocalDate fechaEmision,
            String clienteDenominacion,
            String condicionIva,
            String puntoVentaDescripcion,
            double importeTotal,
            EstadoFactura estado,
            boolean informadaArca,
            long cantidadItems) {
        this(id, numeroFactura, fechaEmision, clienteDenominacion, condicionIva,
                puntoVentaDescripcion, "-", importeTotal, estado, informadaArca, cantidadItems, "PES");
    }
}
