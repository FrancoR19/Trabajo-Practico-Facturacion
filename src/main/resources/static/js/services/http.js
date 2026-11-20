const metaTag = typeof document !== 'undefined' ? document.querySelector('meta[name="api-base-url"]') : null;
export const API_BASE = metaTag?.getAttribute('content') || 'http://localhost:8080/api';
export function conCuerpoJson(metodo, cuerpo) {
    return {
        method: metodo,
        headers: {
            'Content-Type': 'application/json',
            'X-Usuario': 'sistema',
        },
        body: JSON.stringify(cuerpo),
    };
}
export async function solicitar(url, init) {
    const opciones = {
        ...init,
        headers: {
            'X-Usuario': 'sistema',
            ...(init?.headers ?? {}),
        },
    };
    const respuesta = await fetch(url, opciones);
    if (!respuesta.ok) {
        throw new Error(await leerMensajeError(respuesta));
    }
    if (respuesta.status === 204) {
        return undefined;
    }
    return (await respuesta.json());
}
async function leerMensajeError(respuesta) {
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
