import { FacturaService } from './services/FacturaService.js';
const servicio = new FacturaService();
const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
let items = [];
function porId(id) {
    const elemento = document.getElementById(id);
    if (!elemento)
        throw new Error(`No existe el elemento #${id}`);
    return elemento;
}
const formFiltros = porId('form-filtros');
const formFactura = porId('form-factura');
const itemsBody = porId('items-body');
const facturasBody = porId('facturas-body');
const mensaje = porId('mensaje');
function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = `mensaje ${tipo}`;
    mensaje.hidden = false;
}
function mensajeDe(error) {
    return error instanceof Error ? error.message : 'Ocurrió un error inesperado';
}
function hoy() {
    return new Date().toLocaleDateString('en-CA');
}
function formatearFecha(iso) {
    const [anio, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${anio}`;
}
function valorInput(id) {
    return porId(id).value.trim();
}
function numeroOpcional(valor) {
    return valor === '' ? undefined : Number(valor);
}
function leerFiltros() {
    return {
        fechaDesde: valorInput('filtro-desde') || undefined,
        fechaHasta: valorInput('filtro-hasta') || undefined,
        estado: porId('filtro-estado').value || undefined,
        montoMinimo: numeroOpcional(valorInput('filtro-monto')),
    };
}
function crearCelda(texto) {
    const celda = document.createElement('td');
    celda.textContent = texto;
    return celda;
}
function crearBoton(texto, accion, id, deshabilitado) {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.textContent = texto;
    boton.className = accion === 'eliminar' ? 'peligro' : 'secundario';
    boton.dataset.accion = accion;
    boton.dataset.id = String(id);
    boton.disabled = deshabilitado;
    return boton;
}
function renderFacturas(facturas) {
    facturasBody.replaceChildren();
    if (facturas.length === 0) {
        const fila = document.createElement('tr');
        const celda = crearCelda('No hay comprobantes para mostrar');
        celda.colSpan = 9;
        fila.append(celda);
        facturasBody.append(fila);
        return;
    }
    for (const factura of facturas) {
        const fila = document.createElement('tr');
        const acciones = document.createElement('td');
        acciones.className = 'acciones';
        acciones.append(crearBoton('Anular', 'anular', factura.id, factura.estado === 'ANULADA'), crearBoton('Eliminar', 'eliminar', factura.id, false));
        fila.append(crearCelda(String(factura.numeroFactura)), crearCelda(formatearFecha(factura.fechaEmision)), crearCelda(factura.clienteDenominacion), crearCelda(factura.condicionIva), crearCelda(factura.puntoVentaDescripcion), crearCelda(formatoMoneda.format(factura.importeTotal)), crearCelda(factura.estado), crearCelda(String(factura.cantidadItems)), acciones);
        facturasBody.append(fila);
    }
}
async function cargarFacturas() {
    try {
        renderFacturas(await servicio.obtenerFacturas(leerFiltros()));
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
function crearInput(tipo, campo, indice, valor, atributos = {}) {
    const input = document.createElement('input');
    input.type = tipo;
    input.value = valor;
    input.dataset.campo = campo;
    input.dataset.indice = String(indice);
    for (const [nombre, contenido] of Object.entries(atributos)) {
        input.setAttribute(nombre, contenido);
    }
    const celda = document.createElement('td');
    celda.append(input);
    return celda;
}
function renderItems() {
    itemsBody.replaceChildren();
    items.forEach((item, indice) => {
        const quitar = document.createElement('button');
        quitar.type = 'button';
        quitar.textContent = 'Quitar';
        quitar.className = 'peligro';
        quitar.dataset.accion = 'quitar';
        quitar.dataset.indice = String(indice);
        const celdaQuitar = document.createElement('td');
        celdaQuitar.append(quitar);
        const fila = document.createElement('tr');
        fila.append(crearInput('number', 'articuloId', indice, item.articuloId === null ? '' : String(item.articuloId), { min: '1', required: 'true' }), crearInput('number', 'cantidad', indice, String(item.cantidad), { min: '0.01', step: 'any', required: 'true' }), crearInput('number', 'porcentajeBonificacion', indice, String(item.porcentajeBonificacion), { min: '0', max: '100', step: 'any' }), crearInput('text', 'descripcion', indice, item.descripcion, { placeholder: 'Opcional' }), celdaQuitar);
        itemsBody.append(fila);
    });
}
function agregarItem() {
    items.push({ articuloId: null, cantidad: 1, porcentajeBonificacion: 0, descripcion: '' });
    renderItems();
}
function actualizarItem(input) {
    const item = items[Number(input.dataset.indice)];
    if (!item)
        return;
    switch (input.dataset.campo) {
        case 'articuloId':
            item.articuloId = input.value === '' ? null : Number(input.value);
            break;
        case 'cantidad':
            item.cantidad = Number(input.value);
            break;
        case 'porcentajeBonificacion':
            item.porcentajeBonificacion = Number(input.value);
            break;
        case 'descripcion':
            item.descripcion = input.value;
            break;
    }
}
function construirDetalles() {
    return items.map((item) => ({
        articuloId: item.articuloId ?? undefined,
        cantidad: item.cantidad,
        porcentajeBonificacion: item.porcentajeBonificacion,
        descripcion: item.descripcion || undefined,
    }));
}
function restablecerFormulario() {
    formFactura.reset();
    porId('fecha-emision').value = hoy();
    items = [];
    agregarItem();
}
async function guardarFactura(evento) {
    evento.preventDefault();
    if (items.length === 0) {
        mostrarMensaje('Agregá al menos un ítem a la factura', 'error');
        return;
    }
    if (items.some((item) => item.articuloId === null || !(item.cantidad > 0))) {
        mostrarMensaje('Cada ítem necesita un artículo y una cantidad mayor a cero', 'error');
        return;
    }
    const factura = {
        clienteId: numeroOpcional(valorInput('cliente-id')),
        condicionIvaId: Number(valorInput('condicion-iva-id')),
        puntoVentaId: Number(valorInput('punto-venta-id')),
        tipoMonedaId: Number(valorInput('tipo-moneda-id')),
        fechaEmision: valorInput('fecha-emision'),
        importeCobrado: Number(valorInput('importe-cobrado') || 0),
        observaciones: valorInput('observaciones') || undefined,
        detalles: construirDetalles(),
    };
    try {
        const creada = await servicio.crearFactura(factura);
        mostrarMensaje(`Factura N° ${creada.numeroFactura} creada por ${formatoMoneda.format(creada.importeTotal)}`, 'ok');
        restablecerFormulario();
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
async function anularFactura(id) {
    const motivo = window.prompt('Motivo de la anulación (opcional):');
    if (motivo === null)
        return;
    try {
        const anulada = await servicio.actualizarEstado(id, 'ANULADA', motivo.trim() || undefined);
        mostrarMensaje(`Factura N° ${anulada.numeroFactura} anulada`, 'ok');
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
async function eliminarFactura(id) {
    if (!window.confirm('¿Eliminar definitivamente esta factura?'))
        return;
    try {
        await servicio.eliminarFactura(id);
        mostrarMensaje('Factura eliminada', 'ok');
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
function inicializar() {
    porId('fecha-emision').value = hoy();
    agregarItem();
    formFiltros.addEventListener('submit', (evento) => {
        evento.preventDefault();
        void cargarFacturas();
    });
    porId('btn-limpiar').addEventListener('click', () => {
        formFiltros.reset();
        void cargarFacturas();
    });
    porId('btn-agregar-item').addEventListener('click', agregarItem);
    itemsBody.addEventListener('input', (evento) => {
        if (evento.target instanceof HTMLInputElement)
            actualizarItem(evento.target);
    });
    itemsBody.addEventListener('click', (evento) => {
        const boton = evento.target.closest('button[data-accion="quitar"]');
        if (!boton)
            return;
        items.splice(Number(boton.dataset.indice), 1);
        renderItems();
    });
    facturasBody.addEventListener('click', (evento) => {
        const boton = evento.target.closest('button[data-accion]');
        if (!boton)
            return;
        const id = Number(boton.dataset.id);
        if (boton.dataset.accion === 'anular')
            void anularFactura(id);
        if (boton.dataset.accion === 'eliminar')
            void eliminarFactura(id);
    });
    formFactura.addEventListener('submit', (evento) => {
        void guardarFactura(evento);
    });
    void cargarFacturas();
}
inicializar();
