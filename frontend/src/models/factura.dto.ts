export interface FacturaReporteDTO {
  id: number;
  numeroFactura: number;
  fechaEmision: string;
  clienteDenominacion: string;
  condicionIva: string;
  puntoVentaDescripcion: string;
  importeTotal: number;
  estado: string;
  cantidadItems: number;
}

export interface FacturaDetalleRequestDTO {
  articuloId?: number;
  listaPrecioArticuloId?: number;
  cantidad?: number;
  porcentajeBonificacion?: number;
  descripcion?: string;
}

export interface FacturaRequestDTO {
  clienteId?: number;
  condicionIvaId?: number;
  puntoVentaId?: number;
  tipoMonedaId?: number;
  fechaEmision?: string;
  estado?: string;
  importeCobrado?: number;
  observaciones?: string;
  detalles?: FacturaDetalleRequestDTO[];
}

export interface FacturaFiltrosDTO {
  fechaDesde?: string;
  fechaHasta?: string;
  estado?: string;
  montoMinimo?: number;
}

export interface ErrorDTO {
  status: number;
  mensaje: string;
}
