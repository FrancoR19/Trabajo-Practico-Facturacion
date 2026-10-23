const CABECERAS_JSON = { 'Content-Type': 'application/json' };
export class FacturaService {
    constructor() {
        this.baseUrl = 'http://localhost:8080/api/facturas';
    }
    async obtenerFacturas(filtros = {}) {
        const params = new URLSearchParams();
        if (filtros.fechaDesde)
            params.set('fechaDesde', filtros.fechaDesde);
        if (filtros.fechaHasta)
            params.set('fechaHasta', filtros.fechaHasta);
        if (filtros.estado)
            params.set('estado', filtros.estado);
        if (filtros.montoMinimo !== undefined)
            params.set('montoMinimo', String(filtros.montoMinimo));
        const consulta = params.toString();
        const respuesta = await fetch(consulta ? `${this.baseUrl}?${consulta}` : this.baseUrl);
        return this.leerJson(respuesta);
    }
    async crearFactura(factura) {
        const respuesta = await fetch(this.baseUrl, {
            method: 'POST',
            headers: CABECERAS_JSON,
            body: JSON.stringify(factura),
        });
        return this.leerJson(respuesta);
    }
    async actualizarEstado(id, estado, observaciones) {
        const cuerpo = { estado };
        if (observaciones)
            cuerpo.observaciones = observaciones;
        const respuesta = await fetch(`${this.baseUrl}/${id}`, {
            method: 'PUT',
            headers: CABECERAS_JSON,
            body: JSON.stringify(cuerpo),
        });
        return this.leerJson(respuesta);
    }
    async eliminarFactura(id) {
        const respuesta = await fetch(`${this.baseUrl}/${id}`, { method: 'DELETE' });
        if (!respuesta.ok) {
            throw new Error(await this.leerMensajeError(respuesta));
        }
    }
    async leerJson(respuesta) {
        if (!respuesta.ok) {
            throw new Error(await this.leerMensajeError(respuesta));
        }
        return (await respuesta.json());
    }
    async leerMensajeError(respuesta) {
        try {
            const error = (await respuesta.json());
            if (error && typeof error.mensaje === 'string')
                return error.mensaje;
        }
        catch {
            return `Error ${respuesta.status}`;
        }
        return `Error ${respuesta.status}`;
    }
}
