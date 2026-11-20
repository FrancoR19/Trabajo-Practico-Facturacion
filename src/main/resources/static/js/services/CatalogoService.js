import { API_BASE, solicitar } from './http.js';
export class CatalogoService {
    async buscarClientes(texto, pagina, tamanio = 8) {
        return solicitar(`${API_BASE}/clientes?${this.parametros(texto, pagina, tamanio)}`);
    }
    async buscarListasPrecioArticulo(texto, pagina = 0, listaPrecioId, tamanio = 8) {
        const lista = listaPrecioId !== undefined ? `&listaPrecioId=${listaPrecioId}` : '';
        return solicitar(`${API_BASE}/lista-precio-articulos?${this.parametros(texto, pagina, tamanio)}${lista}`);
    }
    async obtenerPreciosPorListaYCodigos(listaPrecioId, codigos) {
        const params = new URLSearchParams({ listaPrecioId: String(listaPrecioId) });
        for (const codigo of codigos) {
            params.append('codigos', codigo);
        }
        return solicitar(`${API_BASE}/lista-precio-articulos/por-codigos?${params.toString()}`);
    }
    async listasPrecios() {
        return solicitar(`${API_BASE}/catalogos/listas-precios`);
    }
    async puntosVenta() {
        return solicitar(`${API_BASE}/catalogos/puntos-venta`);
    }
    async condicionesIva() {
        return solicitar(`${API_BASE}/catalogos/condiciones-iva`);
    }
    async tiposMoneda() {
        return solicitar(`${API_BASE}/catalogos/tipos-moneda`);
    }
    parametros(texto, pagina, tamanio) {
        return new URLSearchParams({ texto, page: String(pagina), size: String(tamanio) }).toString();
    }
}
