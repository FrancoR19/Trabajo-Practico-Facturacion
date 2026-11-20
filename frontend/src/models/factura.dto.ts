export interface FacturaReporteDTO {
  id: number;
  numeroFactura: number;
  fechaEmision: string;
  clienteDenominacion: string;
  condicionIva: string;
  puntoVentaDescripcion: string;
  tipoComprobante?: string;
  importeTotal: number;
  estado: string;
  informadaArca: boolean;
  cantidadItems: number;
  codigoMoneda?: string;
}

export interface FacturaDetalleRequestDTO {
  listaPrecioArticuloId?: number;
  cantidad?: number;
  porcentajeBonificacion?: number;
  descripcion?: string;
}

export interface FacturaRequestDTO {
  clienteId?: number;
  sinCliente?: boolean;
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
  numero?: number;
  fechaDesde?: string;
  fechaHasta?: string;
  cliente?: string;
  informadaArca?: boolean;
  estado?: string;
  montoMinimo?: number;
}

export interface PaginaDTO<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface ClienteDTO {
  id: number;
  cuitCuil: string;
  denominacion: string;
  condicionIvaId?: number;
  condicionIvaDenominacion?: string;
  listaPrecioId?: number;
  listaPrecioDenominacion?: string;
}

export interface ListaPrecioArticuloDTO {
  id: number;
  listaPrecio: string;
  articuloCodigo: string;
  articuloDenominacion: string;
  precioVenta: number;
}

export interface CatalogoItemDTO {
  id: number;
  descripcion: string;
}

export interface FacturaEdicionDetalleDTO {
  listaPrecioArticuloId: number;
  listaPrecio: string;
  articuloCodigo: string;
  articuloDenominacion: string;
  precioUnitario: number;
  cantidad: number;
  porcentajeBonificacion: number;
}

export interface FacturaEdicionDTO {
  id: number;
  numero: number;
  fechaEmision: string;
  clienteId: number | null;
  clienteDenominacion: string | null;
  condicionIvaId: number;
  puntoVentaId: number;
  tipoMonedaId: number;
  listaPrecioId?: number | null;
  importeCobrado: number;
  observaciones: string | null;
  estado: string;
  informadaArca: boolean;
  detalles: FacturaEdicionDetalleDTO[];
}

export interface ErrorDTO {
  status: number;
  mensaje: string;
}
