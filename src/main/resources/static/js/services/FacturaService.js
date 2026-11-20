import { API_BASE, conCuerpoJson, solicitar } from './http.js';
export class FacturaService {
    constructor() {
        this.baseUrl = `${API_BASE}/facturas`;
    }
    construirParametrosFiltro(filtros = {}) {
        const params = new URLSearchParams();
        if (filtros.numero !== undefined)
            params.set('numero', String(filtros.numero));
        if (filtros.fechaDesde)
            params.set('fechaDesde', filtros.fechaDesde);
        if (filtros.fechaHasta)
            params.set('fechaHasta', filtros.fechaHasta);
        if (filtros.cliente)
            params.set('cliente', filtros.cliente);
        if (filtros.informadaArca !== undefined)
            params.set('informadaArca', String(filtros.informadaArca));
        if (filtros.estado)
            params.set('estado', filtros.estado);
        if (filtros.montoMinimo !== undefined)
            params.set('montoMinimo', String(filtros.montoMinimo));
        return params;
    }
    async obtenerFacturas(filtros = {}, pagina = 0, tamanio = 5) {
        const params = this.construirParametrosFiltro(filtros);
        params.set('page', String(pagina));
        params.set('size', String(tamanio));
        return solicitar(`${this.baseUrl}?${params.toString()}`);
    }
    urlReporte(tipo, filtros = {}) {
        const params = this.construirParametrosFiltro(filtros);
        const query = params.toString();
        return `${API_BASE}/reportes/${tipo}${query ? `?${query}` : ''}`;
    }
    urlPdfFactura(id) {
        return `${API_BASE}/facturas/${id}/pdf`;
    }
    async obtenerFactura(id) {
        return solicitar(`${this.baseUrl}/${id}`);
    }
    async crearFactura(factura) {
        return solicitar(this.baseUrl, conCuerpoJson('POST', factura));
    }
    async actualizarFactura(id, factura) {
        return solicitar(`${this.baseUrl}/${id}`, conCuerpoJson('PUT', factura));
    }
    async actualizarEstado(id, estado, observaciones) {
        const cuerpo = { estado };
        if (observaciones)
            cuerpo.observaciones = observaciones;
        return solicitar(`${this.baseUrl}/${id}`, conCuerpoJson('PUT', cuerpo));
    }
    async cambiarEstado(id) {
        return solicitar(`${this.baseUrl}/${id}/estado`, { method: 'PATCH' });
    }
    async informarArca(id) {
        return solicitar(`${this.baseUrl}/${id}/informar-arca`, { method: 'POST' });
    }
    async eliminarFactura(id) {
        return solicitar(`${this.baseUrl}/${id}`, { method: 'DELETE' });
    }
}
