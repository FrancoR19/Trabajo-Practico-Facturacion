import type {
  CatalogoItemDTO,
  ClienteDTO,
  ListaPrecioArticuloDTO,
  PaginaDTO,
} from '../models/factura.dto.js';
import { API_BASE, solicitar } from './http.js';

export class CatalogoService {
  async buscarClientes(texto: string, pagina: number, tamanio = 8): Promise<PaginaDTO<ClienteDTO>> {
    return solicitar<PaginaDTO<ClienteDTO>>(`${API_BASE}/clientes?${this.parametros(texto, pagina, tamanio)}`);
  }

  async buscarListasPrecioArticulo(
    texto: string,
    pagina = 0,
    listaPrecioId?: number,
    tamanio = 8,
  ): Promise<PaginaDTO<ListaPrecioArticuloDTO>> {
    const lista = listaPrecioId !== undefined ? `&listaPrecioId=${listaPrecioId}` : '';
    return solicitar<PaginaDTO<ListaPrecioArticuloDTO>>(
      `${API_BASE}/lista-precio-articulos?${this.parametros(texto, pagina, tamanio)}${lista}`,
    );
  }

  async obtenerPreciosPorListaYCodigos(
    listaPrecioId: number,
    codigos: string[],
  ): Promise<ListaPrecioArticuloDTO[]> {
    const params = new URLSearchParams({ listaPrecioId: String(listaPrecioId) });
    for (const codigo of codigos) {
      params.append('codigos', codigo);
    }
    return solicitar<ListaPrecioArticuloDTO[]>(
      `${API_BASE}/lista-precio-articulos/por-codigos?${params.toString()}`,
    );
  }

  async listasPrecios(): Promise<CatalogoItemDTO[]> {
    return solicitar<CatalogoItemDTO[]>(`${API_BASE}/catalogos/listas-precios`);
  }

  async puntosVenta(): Promise<CatalogoItemDTO[]> {
    return solicitar<CatalogoItemDTO[]>(`${API_BASE}/catalogos/puntos-venta`);
  }

  async condicionesIva(): Promise<CatalogoItemDTO[]> {
    return solicitar<CatalogoItemDTO[]>(`${API_BASE}/catalogos/condiciones-iva`);
  }

  async tiposMoneda(): Promise<CatalogoItemDTO[]> {
    return solicitar<CatalogoItemDTO[]>(`${API_BASE}/catalogos/tipos-moneda`);
  }

  private parametros(texto: string, pagina: number, tamanio: number): string {
    return new URLSearchParams({ texto, page: String(pagina), size: String(tamanio) }).toString();
  }
}
