import type {
  ErrorDTO,
  FacturaFiltrosDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
} from '../models/factura.dto.js';

const CABECERAS_JSON = { 'Content-Type': 'application/json' };

export class FacturaService {
  private readonly baseUrl = 'http://localhost:8080/api/facturas';

  async obtenerFacturas(filtros: FacturaFiltrosDTO = {}): Promise<FacturaReporteDTO[]> {
    const params = new URLSearchParams();
    if (filtros.fechaDesde) params.set('fechaDesde', filtros.fechaDesde);
    if (filtros.fechaHasta) params.set('fechaHasta', filtros.fechaHasta);
    if (filtros.estado) params.set('estado', filtros.estado);
    if (filtros.montoMinimo !== undefined) params.set('montoMinimo', String(filtros.montoMinimo));

    const consulta = params.toString();
    const respuesta = await fetch(consulta ? `${this.baseUrl}?${consulta}` : this.baseUrl);
    return this.leerJson<FacturaReporteDTO[]>(respuesta);
  }

  async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    const respuesta = await fetch(this.baseUrl, {
      method: 'POST',
      headers: CABECERAS_JSON,
      body: JSON.stringify(factura),
    });
    return this.leerJson<FacturaReporteDTO>(respuesta);
  }

  async actualizarEstado(id: number, estado: string, observaciones?: string): Promise<FacturaReporteDTO> {
    const cuerpo: FacturaRequestDTO = { estado };
    if (observaciones) cuerpo.observaciones = observaciones;

    const respuesta = await fetch(`${this.baseUrl}/${id}`, {
      method: 'PUT',
      headers: CABECERAS_JSON,
      body: JSON.stringify(cuerpo),
    });
    return this.leerJson<FacturaReporteDTO>(respuesta);
  }

  async eliminarFactura(id: number): Promise<void> {
    const respuesta = await fetch(`${this.baseUrl}/${id}`, { method: 'DELETE' });
    if (!respuesta.ok) {
      throw new Error(await this.leerMensajeError(respuesta));
    }
  }

  private async leerJson<T>(respuesta: Response): Promise<T> {
    if (!respuesta.ok) {
      throw new Error(await this.leerMensajeError(respuesta));
    }
    return (await respuesta.json()) as T;
  }

  private async leerMensajeError(respuesta: Response): Promise<string> {
    try {
      const error = (await respuesta.json()) as ErrorDTO;
      if (error && typeof error.mensaje === 'string') return error.mensaje;
    } catch {
      return `Error ${respuesta.status}`;
    }
    return `Error ${respuesta.status}`;
  }
}
