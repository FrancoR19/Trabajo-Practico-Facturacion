import type {
  FacturaEdicionDTO,
  FacturaFiltrosDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
  PaginaDTO,
} from '../models/factura.dto.js';
import { API_BASE, conCuerpoJson, solicitar } from './http.js';

export class FacturaService {
  private readonly baseUrl = `${API_BASE}/facturas`;

  construirParametrosFiltro(filtros: FacturaFiltrosDTO = {}): URLSearchParams {
    const params = new URLSearchParams();
    if (filtros.numero !== undefined) params.set('numero', String(filtros.numero));
    if (filtros.fechaDesde) params.set('fechaDesde', filtros.fechaDesde);
    if (filtros.fechaHasta) params.set('fechaHasta', filtros.fechaHasta);
    if (filtros.cliente) params.set('cliente', filtros.cliente);
    if (filtros.informadaArca !== undefined) params.set('informadaArca', String(filtros.informadaArca));
    if (filtros.estado) params.set('estado', filtros.estado);
    if (filtros.montoMinimo !== undefined) params.set('montoMinimo', String(filtros.montoMinimo));
    return params;
  }

  async obtenerFacturas(
    filtros: FacturaFiltrosDTO = {},
    pagina = 0,
    tamanio = 5,
  ): Promise<PaginaDTO<FacturaReporteDTO>> {
    const params = this.construirParametrosFiltro(filtros);
    params.set('page', String(pagina));
    params.set('size', String(tamanio));
    return solicitar<PaginaDTO<FacturaReporteDTO>>(`${this.baseUrl}?${params.toString()}`);
  }

  urlReporte(tipo: 'pdf' | 'excel', filtros: FacturaFiltrosDTO = {}): string {
    const params = this.construirParametrosFiltro(filtros);
    const query = params.toString();
    return `${API_BASE}/reportes/${tipo}${query ? `?${query}` : ''}`;
  }

  urlPdfFactura(id: number): string {
    return `${API_BASE}/facturas/${id}/pdf`;
  }

  async obtenerFactura(id: number): Promise<FacturaEdicionDTO> {
    return solicitar<FacturaEdicionDTO>(`${this.baseUrl}/${id}`);
  }

  async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(this.baseUrl, conCuerpoJson('POST', factura));
  }

  async actualizarFactura(id: number, factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(`${this.baseUrl}/${id}`, conCuerpoJson('PUT', factura));
  }

  async actualizarEstado(id: number, estado: string, observaciones?: string): Promise<FacturaReporteDTO> {
    const cuerpo: FacturaRequestDTO = { estado };
    if (observaciones) cuerpo.observaciones = observaciones;
    return solicitar<FacturaReporteDTO>(`${this.baseUrl}/${id}`, conCuerpoJson('PUT', cuerpo));
  }

  async cambiarEstado(id: number): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(`${this.baseUrl}/${id}/estado`, { method: 'PATCH' });
  }

  async informarArca(id: number): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(`${this.baseUrl}/${id}/informar-arca`, { method: 'POST' });
  }

  async eliminarFactura(id: number): Promise<void> {
    return solicitar<void>(`${this.baseUrl}/${id}`, { method: 'DELETE' });
  }
}
