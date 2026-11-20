import { Explorador } from './components/Explorador.js';
import { CatalogoService } from './services/CatalogoService.js';
import { FacturaService } from './services/FacturaService.js';
const TAMANIO_PAGINA = 5;
const ALICUOTA_IVA = 0.21;
const facturas = new FacturaService();
const catalogos = new CatalogoService();
const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const formatoDolar = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'USD' });
function formatearImporte(valor, codigoMoneda = 'PES') {
    if (codigoMoneda === 'DOL' || codigoMoneda === 'USD')
        return formatoDolar.format(valor);
    if (codigoMoneda === 'PES' || codigoMoneda === 'ARS')
        return formatoMoneda.format(valor);
    return `${codigoMoneda} ${valor.toFixed(2)}`;
}
function etiquetaMoneda(codigo) {
    if (codigo === 'PES')
        return 'ARS';
    if (codigo === 'DOL')
        return 'USD';
    return codigo;
}
function etiquetaTipo(factura) {
    const tipo = (factura.tipoComprobante || '').trim().toUpperCase();
    const etiquetas = {
        FA: 'Factura A',
        FB: 'Factura B',
        NCA: 'Nota de crédito A',
        NCB: 'Nota de crédito B',
        NDA: 'Nota de débito A',
        NDB: 'Nota de débito B',
    };
    if (etiquetas[tipo])
        return etiquetas[tipo];
    if (tipo)
        return tipo;
    return factura.condicionIva === 'Responsable Inscripto' ? 'Factura A' : 'Factura B';
}
let items = [];
let cliente = null;
let facturaEditandoId = null;
let paginaActual = 0;
let filtrosActivos = {};
function porId(id) {
    const elemento = document.getElementById(id);
    if (!elemento)
        throw new Error(`No existe el elemento #${id}`);
    return elemento;
}
const formFiltros = porId('form-filtros');
const seccionFormulario = porId('seccion-formulario');
const tituloFormulario = porId('titulo-formulario');
const formFactura = porId('form-factura');
const botonGuardar = porId('btn-guardar');
const campoCliente = porId('cliente-texto');
const selectCondicionIva = porId('condicion-iva');
const badgeTipoComprobante = porId('badge-tipo-comprobante');
const selectPuntoVenta = porId('punto-venta');
const selectTipoMoneda = porId('tipo-moneda');
const selectListaPrecio = porId('lista-precio');
const campoFecha = porId('fecha-emision');
const campoCobrado = porId('importe-cobrado');
const campoObservaciones = porId('observaciones');
const itemsBody = porId('items-body');
const spanSubtotalNeto = porId('total-subtotal-neto');
const spanIva = porId('total-iva');
const spanTotalFinal = porId('total-final');
const facturasBody = porId('facturas-body');
const botonAnterior = porId('btn-anterior');
const botonSiguiente = porId('btn-siguiente');
const infoPaginacion = porId('info-paginacion');
const mensaje = porId('mensaje');
const exploradorClientes = new Explorador({
    titulo: 'Buscar cliente',
    columnas: [
        { titulo: 'CUIT/CUIL', valor: (c) => c.cuitCuil },
        { titulo: 'Denominación', valor: (c) => c.denominacion },
        { titulo: 'Condición IVA', valor: (c) => c.condicionIvaDenominacion || '-' },
        { titulo: 'Lista de precios', valor: (c) => c.listaPrecioDenominacion || '-' },
    ],
    buscar: (texto, pagina) => catalogos.buscarClientes(texto, pagina),
    id: (c) => c.id,
    multiple: false,
});
const exploradorPrecios = new Explorador({
    titulo: 'Buscar artículos en lista de precios',
    subtitulo: () => `Lista de precios aplicada: ${etiquetaListaPrecioSeleccionada()}`,
    columnas: [
        { titulo: 'Código', valor: (p) => p.articuloCodigo },
        { titulo: 'Artículo', valor: (p) => p.articuloDenominacion },
        { titulo: 'Precio', valor: (p) => formatoMoneda.format(p.precioVenta) },
    ],
    buscar: (texto, pagina) => {
        const listaPrecioId = selectListaPrecio.value ? Number(selectListaPrecio.value) : undefined;
        if (!listaPrecioId) {
            return Promise.resolve({
                contenido: [],
                pagina: 0,
                tamanio: 8,
                totalElementos: 0,
                totalPaginas: 0,
            });
        }
        return catalogos.buscarListasPrecioArticulo(texto, pagina, listaPrecioId);
    },
    id: (p) => p.id,
    multiple: true,
});
function mostrarMensaje(texto, tipo) {
    mensaje.textContent = texto;
    mensaje.className = `mensaje ${tipo}`;
    mensaje.hidden = false;
    mensaje.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
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
function crearCelda(texto) {
    const celda = document.createElement('td');
    celda.textContent = texto;
    return celda;
}
function crearBoton(texto, clase, accion, id, deshabilitado, motivo) {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.textContent = texto;
    boton.className = clase;
    boton.dataset.accion = accion;
    boton.dataset.id = String(id);
    boton.disabled = deshabilitado;
    if (deshabilitado)
        boton.title = motivo;
    return boton;
}
function llenarSelect(select, opciones) {
    select.replaceChildren();
    for (const opcion of opciones) {
        const elemento = document.createElement('option');
        elemento.value = String(opcion.id);
        elemento.textContent = opcion.descripcion;
        select.append(elemento);
    }
}
async function cargarCatalogos() {
    const [puntosVenta, condicionesIva, tiposMoneda, listasPrecios] = await Promise.all([
        catalogos.puntosVenta(),
        catalogos.condicionesIva(),
        catalogos.tiposMoneda(),
        catalogos.listasPrecios(),
    ]);
    llenarSelect(selectPuntoVenta, puntosVenta);
    llenarSelect(selectCondicionIva, condicionesIva);
    llenarSelect(selectTipoMoneda, tiposMoneda);
    llenarSelect(selectListaPrecio, listasPrecios);
    fijarTipoMonedaPeso();
    if (!cliente && facturaEditandoId === null) {
        fijarConsumidorFinal();
    }
    else {
        actualizarTipoComprobante();
    }
}
function leerFiltros() {
    const valor = (id) => porId(id).value.trim();
    const arca = porId('filtro-arca').value;
    return {
        numero: valor('filtro-numero') === '' ? undefined : Number(valor('filtro-numero')),
        fechaDesde: valor('filtro-desde') || undefined,
        fechaHasta: valor('filtro-hasta') || undefined,
        cliente: valor('filtro-cliente') || undefined,
        informadaArca: arca === '' ? undefined : arca === 'true',
    };
}
function renderFacturas(pagina) {
    facturasBody.replaceChildren();
    if (pagina.contenido.length === 0) {
        const fila = document.createElement('tr');
        const celda = crearCelda('No hay comprobantes para mostrar');
        celda.colSpan = 12;
        fila.append(celda);
        facturasBody.append(fila);
    }
    for (const factura of pagina.contenido) {
        const anulada = factura.estado === 'ANULADA';
        const informada = factura.informadaArca;
        const motivoInformada = 'La factura ya fue informada a ARCA y no puede modificarse';
        const motivoAnulada = 'La factura está anulada';
        const motivoNoAnulable = anulada
            ? motivoAnulada
            : 'Solo se puede anular una factura informada a ARCA';
        const motivoNoEliminable = 'No se puede eliminar una factura ya informada a ARCA';
        const moneda = factura.codigoMoneda || 'PES';
        const estadoArca = document.createElement('td');
        const insignia = document.createElement('span');
        insignia.textContent = informada ? 'Informada' : 'Pendiente';
        insignia.className = informada ? 'insignia informada' : 'insignia pendiente';
        estadoArca.append(insignia);
        const acciones = document.createElement('td');
        acciones.className = 'acciones';
        const botonAnular = crearBoton('Anular', 'secundario', 'anular', factura.id, !informada || anulada, motivoNoAnulable);
        botonAnular.dataset.estado = factura.estado;
        botonAnular.dataset.informada = String(informada);
        botonAnular.dataset.numero = String(factura.numeroFactura ?? '');
        acciones.append(crearBoton('PDF', 'secundario', 'pdf', factura.id, false, ''), crearBoton('Editar', 'secundario', 'editar', factura.id, informada || anulada, informada ? motivoInformada : motivoAnulada), crearBoton('Informar a ARCA', '', 'informar', factura.id, informada || anulada, informada ? motivoInformada : motivoAnulada), botonAnular, crearBoton('Eliminar', 'peligro', 'eliminar', factura.id, informada, motivoNoEliminable));
        const fila = document.createElement('tr');
        if (facturaEditandoId !== null && factura.id === facturaEditandoId) {
            fila.classList.add('editando');
        }
        fila.append(crearCelda(String(factura.numeroFactura ?? '-')), crearCelda(etiquetaTipo(factura)), crearCelda(formatearFecha(factura.fechaEmision)), crearCelda(factura.clienteDenominacion), crearCelda(factura.condicionIva), crearCelda(factura.puntoVentaDescripcion), crearCelda(formatearImporte(factura.importeTotal, moneda)), crearCelda(etiquetaMoneda(moneda)), crearCelda(factura.estado), estadoArca, crearCelda(String(factura.cantidadItems)), acciones);
        facturasBody.append(fila);
    }
    infoPaginacion.textContent =
        `Página ${pagina.pagina + 1} de ${Math.max(pagina.totalPaginas, 1)} (${pagina.totalElementos} comprobantes)`;
    botonAnterior.disabled = pagina.pagina <= 0;
    botonSiguiente.disabled = pagina.pagina + 1 >= pagina.totalPaginas;
}
async function cargarFacturas() {
    try {
        const pagina = await facturas.obtenerFacturas(filtrosActivos, paginaActual, TAMANIO_PAGINA);
        if (pagina.contenido.length === 0 && paginaActual > 0) {
            paginaActual = Math.max(pagina.totalPaginas - 1, 0);
            await cargarFacturas();
            return;
        }
        renderFacturas(pagina);
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
function subtotalNeto(item) {
    return item.cantidad * item.precioUnitario * (1 - item.porcentajeBonificacion / 100);
}
function calcularTotales() {
    let subtotalNetoTotal = 0;
    let ivaTotal = 0;
    let totalFinal = 0;
    for (const item of items) {
        const netoSinRedondear = item.cantidad * item.precioUnitario * (1 - item.porcentajeBonificacion / 100);
        const neto = Math.round(netoSinRedondear * 100) / 100;
        const iva = Math.round(netoSinRedondear * ALICUOTA_IVA * 100) / 100;
        const subtotal = Math.round((neto + iva) * 100) / 100;
        subtotalNetoTotal += neto;
        ivaTotal += iva;
        totalFinal += subtotal;
    }
    return {
        subtotalNeto: Math.round(subtotalNetoTotal * 100) / 100,
        iva: Math.round(ivaTotal * 100) / 100,
        totalFinal: Math.round(totalFinal * 100) / 100,
    };
}
function actualizarTotales(autocompletarCobrado = true) {
    const { subtotalNeto, iva, totalFinal } = calcularTotales();
    spanSubtotalNeto.textContent = formatoMoneda.format(subtotalNeto);
    spanIva.textContent = formatoMoneda.format(iva);
    spanTotalFinal.textContent = formatoMoneda.format(totalFinal);
    if (autocompletarCobrado) {
        campoCobrado.value = totalFinal.toFixed(2);
    }
}
function buscarOpcionConsumidorFinal() {
    for (const opcion of Array.from(selectCondicionIva.options)) {
        const texto = opcion.textContent?.trim().toLowerCase() || '';
        if (texto.includes('consumidor final')) {
            return opcion;
        }
    }
    return null;
}
function esCondicionResponsableInscripto() {
    const opcion = selectCondicionIva.selectedOptions[0];
    const texto = opcion ? opcion.textContent?.trim().toLowerCase() || '' : '';
    return texto.includes('responsable inscripto');
}
function actualizarTipoComprobante() {
    const esResponsable = esCondicionResponsableInscripto();
    const tipo = esResponsable ? 'Factura A' : 'Factura B';
    badgeTipoComprobante.textContent = tipo;
    badgeTipoComprobante.className = `badge-comprobante ${esResponsable ? 'tipo-a' : 'tipo-b'}`;
}
function fijarTipoMonedaPeso() {
    const opciones = Array.from(selectTipoMoneda.options);
    const opcionPeso = opciones.find((opcion) => {
        const texto = opcion.textContent?.trim().toLowerCase() || '';
        return texto.includes('peso argentino') || texto === 'peso' || texto === 'pes' || texto === 'ars' || texto.includes('peso');
    }) ?? opciones[0];
    if (opcionPeso) {
        selectTipoMoneda.value = opcionPeso.value;
    }
    selectTipoMoneda.disabled = true;
}
function etiquetaListaPrecioSeleccionada() {
    const opcion = selectListaPrecio.selectedOptions[0];
    return opcion?.textContent?.trim() || '-';
}
function buscarOpcionListaPorNombre(nombreBuscado) {
    const objetivo = nombreBuscado.trim().toLowerCase();
    const opciones = Array.from(selectListaPrecio.options);
    return (opciones.find((opcion) => (opcion.textContent?.trim().toLowerCase() || '') === objetivo) ??
        opciones.find((opcion) => (opcion.textContent?.trim().toLowerCase() || '').includes(objetivo)) ??
        null);
}
function seleccionarListaPrecioPorDefecto(listaPrecioId, listaPrecioDenominacion, esEmpresa = false) {
    const anterior = selectListaPrecio.value;
    let asignada = false;
    if (listaPrecioId !== undefined && listaPrecioId !== null) {
        const valor = String(listaPrecioId);
        const existe = Array.from(selectListaPrecio.options).some((opcion) => opcion.value === valor);
        if (existe) {
            selectListaPrecio.value = valor;
            asignada = true;
        }
    }
    if (!asignada && listaPrecioDenominacion) {
        const opcionPorNombre = buscarOpcionListaPorNombre(listaPrecioDenominacion);
        if (opcionPorNombre) {
            selectListaPrecio.value = opcionPorNombre.value;
            asignada = true;
        }
    }
    if (!asignada) {
        const clavePorDefecto = esEmpresa ? 'mayorista' : 'minorista';
        const opcionPorTipo = buscarOpcionListaPorNombre(clavePorDefecto) ?? selectListaPrecio.options[0] ?? null;
        if (opcionPorTipo) {
            selectListaPrecio.value = opcionPorTipo.value;
        }
    }
    selectListaPrecio.disabled = false;
    return selectListaPrecio.value !== anterior;
}
async function sincronizarItemsConListaSeleccionada(mostrarAviso = true) {
    if (items.length === 0 || !selectListaPrecio.value)
        return;
    const listaId = Number(selectListaPrecio.value);
    const codigosUnicos = Array.from(new Set(items.map((item) => item.articuloCodigo)));
    const preciosNuevos = await catalogos.obtenerPreciosPorListaYCodigos(listaId, codigosUnicos);
    const mapaPorCodigo = new Map();
    for (const precio of preciosNuevos) {
        mapaPorCodigo.set(precio.articuloCodigo, precio);
    }
    const actualizados = [];
    const faltantes = [];
    for (const item of items) {
        const precioEncontrado = mapaPorCodigo.get(item.articuloCodigo);
        if (precioEncontrado) {
            actualizados.push({
                ...item,
                listaPrecioArticuloId: precioEncontrado.id,
                listaPrecio: precioEncontrado.listaPrecio,
                precioUnitario: precioEncontrado.precioVenta,
            });
        }
        else {
            faltantes.push(`${item.articuloCodigo} (${item.articuloDenominacion})`);
        }
    }
    items = actualizados;
    renderItems();
    const nombreLista = etiquetaListaPrecioSeleccionada();
    if (faltantes.length > 0) {
        mostrarMensaje(`Se quitaron ítems sin precio en ${nombreLista}: ${faltantes.join(', ')}`, 'error');
    }
    else if (mostrarAviso) {
        mostrarMensaje(`Precios actualizados según ${nombreLista}`, 'ok');
    }
}
function limpiarResaltadoEdicion() {
    facturasBody.querySelectorAll('tr.editando').forEach((fila) => {
        fila.classList.remove('editando');
    });
}
function fijarConsumidorFinal() {
    cliente = null;
    renderCliente();
    const opcionCF = buscarOpcionConsumidorFinal();
    if (opcionCF) {
        selectCondicionIva.value = opcionCF.value;
    }
    selectCondicionIva.disabled = true;
    actualizarTipoComprobante();
    const cambioLista = seleccionarListaPrecioPorDefecto(undefined, 'Lista Minorista', false);
    if (cambioLista && items.length > 0) {
        void sincronizarItemsConListaSeleccionada(true).catch((error) => mostrarMensaje(mensajeDe(error), 'error'));
    }
}
function crearInputItem(campo, indice, valor, atributos) {
    const input = document.createElement('input');
    input.type = 'number';
    input.value = String(valor);
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
    if (items.length === 0) {
        const fila = document.createElement('tr');
        const celda = crearCelda('Todavía no hay ítems. Usá «Agregar ítems» para elegirlos de la lista de precios seleccionada.');
        celda.colSpan = 7;
        fila.append(celda);
        itemsBody.append(fila);
    }
    items.forEach((item, indice) => {
        const subtotal = crearCelda(formatoMoneda.format(subtotalNeto(item)));
        subtotal.dataset.subtotal = 'true';
        const quitar = document.createElement('button');
        quitar.type = 'button';
        quitar.textContent = 'Quitar';
        quitar.className = 'peligro';
        quitar.dataset.accion = 'quitar';
        quitar.dataset.indice = String(indice);
        const celdaQuitar = document.createElement('td');
        celdaQuitar.append(quitar);
        const fila = document.createElement('tr');
        fila.append(crearCelda(item.articuloCodigo), crearCelda(item.articuloDenominacion), crearCelda(formatoMoneda.format(item.precioUnitario)), crearInputItem('cantidad', indice, item.cantidad, { min: '0.01', step: 'any', required: 'true' }), crearInputItem('porcentajeBonificacion', indice, item.porcentajeBonificacion, { min: '0', max: '100', step: 'any' }), subtotal, celdaQuitar);
        itemsBody.append(fila);
    });
    actualizarTotales(true);
}
function renderCliente() {
    campoCliente.value = cliente ? cliente.denominacion : 'Consumidor Final';
}
function reiniciarFormulario() {
    formFactura.reset();
    items = [];
    cliente = null;
    facturaEditandoId = null;
    limpiarResaltadoEdicion();
    seccionFormulario.classList.remove('modo-edicion');
    tituloFormulario.textContent = 'Nueva factura';
    botonGuardar.textContent = 'Guardar factura';
    campoFecha.value = hoy();
    fijarConsumidorFinal();
    fijarTipoMonedaPeso();
    renderItems();
}
function abrirFormulario(titulo, editando = false) {
    tituloFormulario.textContent = titulo;
    botonGuardar.textContent = editando ? 'Guardar cambios' : 'Guardar factura';
    seccionFormulario.classList.toggle('modo-edicion', editando);
    seccionFormulario.hidden = false;
    seccionFormulario.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
function cerrarFormulario() {
    seccionFormulario.hidden = true;
    reiniciarFormulario();
}
function nuevaFactura() {
    reiniciarFormulario();
    abrirFormulario('Nueva factura');
}
async function editarFactura(id) {
    try {
        const factura = await facturas.obtenerFactura(id);
        if (factura.informadaArca) {
            mostrarMensaje('La factura ya fue informada a ARCA y no puede modificarse', 'error');
            return;
        }
        reiniciarFormulario();
        facturaEditandoId = factura.id;
        cliente = factura.clienteId !== null && factura.clienteDenominacion !== null
            ? { id: factura.clienteId, denominacion: factura.clienteDenominacion }
            : null;
        renderCliente();
        selectCondicionIva.value = String(factura.condicionIvaId);
        selectCondicionIva.disabled = cliente === null;
        actualizarTipoComprobante();
        selectPuntoVenta.value = String(factura.puntoVentaId);
        fijarTipoMonedaPeso();
        campoFecha.value = factura.fechaEmision;
        campoObservaciones.value = factura.observaciones ?? '';
        items = factura.detalles.map((detalle) => ({ ...detalle }));
        const nombreListaDetalles = factura.detalles[0]?.listaPrecio;
        seleccionarListaPrecioPorDefecto(factura.listaPrecioId ?? undefined, nombreListaDetalles ?? 'Lista Minorista', cliente !== null && esCondicionResponsableInscripto());
        renderItems();
        actualizarTotales(false);
        campoCobrado.value = String(factura.importeCobrado);
        abrirFormulario(`EDITAR FACTURA — N° ${factura.numero}`, true);
        void cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
async function elegirCliente() {
    const [elegido] = await exploradorClientes.abrir();
    if (elegido) {
        cliente = {
            id: elegido.id,
            denominacion: elegido.denominacion,
            condicionIvaId: elegido.condicionIvaId,
            condicionIvaDenominacion: elegido.condicionIvaDenominacion,
            listaPrecioId: elegido.listaPrecioId,
            listaPrecioDenominacion: elegido.listaPrecioDenominacion,
        };
        renderCliente();
        selectCondicionIva.disabled = false;
        let asignado = false;
        if (elegido.condicionIvaId !== undefined && elegido.condicionIvaId !== null) {
            selectCondicionIva.value = String(elegido.condicionIvaId);
            asignado = selectCondicionIva.value === String(elegido.condicionIvaId);
        }
        if (!asignado && elegido.condicionIvaDenominacion) {
            const denomBuscada = elegido.condicionIvaDenominacion.trim().toLowerCase();
            for (const opcion of Array.from(selectCondicionIva.options)) {
                if (opcion.textContent?.trim().toLowerCase() === denomBuscada) {
                    selectCondicionIva.value = opcion.value;
                    asignado = true;
                    break;
                }
            }
        }
        const cuitDigitos = (elegido.cuitCuil || '').replace(/\D/g, '');
        const esCuitEmpresa = cuitDigitos.startsWith('30') || cuitDigitos.startsWith('33') || cuitDigitos.startsWith('34');
        if (!asignado && esCuitEmpresa) {
            for (const opcion of Array.from(selectCondicionIva.options)) {
                const texto = opcion.textContent?.trim().toLowerCase() || '';
                if (texto.includes('responsable inscripto')) {
                    selectCondicionIva.value = opcion.value;
                    asignado = true;
                    break;
                }
            }
        }
        actualizarTipoComprobante();
        const esEmpresa = esCuitEmpresa || esCondicionResponsableInscripto();
        const cambioLista = seleccionarListaPrecioPorDefecto(elegido.listaPrecioId, elegido.listaPrecioDenominacion, esEmpresa);
        if (cambioLista && items.length > 0) {
            try {
                await sincronizarItemsConListaSeleccionada(true);
            }
            catch (error) {
                mostrarMensaje(mensajeDe(error), 'error');
            }
        }
    }
}
async function agregarItems() {
    if (!selectListaPrecio.value) {
        mostrarMensaje('Seleccioná una lista de precios antes de agregar ítems', 'error');
        return;
    }
    const elegidos = await exploradorPrecios.abrir();
    for (const precio of elegidos) {
        const existente = items.find((item) => item.listaPrecioArticuloId === precio.id || item.articuloCodigo === precio.articuloCodigo);
        if (existente) {
            existente.cantidad += 1;
            continue;
        }
        items.push({
            listaPrecioArticuloId: precio.id,
            listaPrecio: precio.listaPrecio,
            articuloCodigo: precio.articuloCodigo,
            articuloDenominacion: precio.articuloDenominacion,
            precioUnitario: precio.precioVenta,
            cantidad: 1,
            porcentajeBonificacion: 0,
        });
    }
    renderItems();
}
function actualizarItem(input) {
    const item = items[Number(input.dataset.indice)];
    if (!item)
        return;
    if (input.dataset.campo === 'cantidad')
        item.cantidad = Number(input.value);
    if (input.dataset.campo === 'porcentajeBonificacion')
        item.porcentajeBonificacion = Number(input.value);
    const celda = input.closest('tr')?.querySelector('[data-subtotal]');
    if (celda)
        celda.textContent = formatoMoneda.format(subtotalNeto(item));
    actualizarTotales(true);
}
async function guardarFactura(evento) {
    evento.preventDefault();
    if (items.length === 0) {
        mostrarMensaje('Agregá al menos un ítem a la factura', 'error');
        return;
    }
    if (items.some((item) => !(item.cantidad > 0))) {
        mostrarMensaje('La cantidad de cada ítem debe ser mayor a cero', 'error');
        return;
    }
    if (items.some((item) => item.porcentajeBonificacion < 0 || item.porcentajeBonificacion > 100)) {
        mostrarMensaje('La bonificación de cada ítem debe estar entre 0 y 100', 'error');
        return;
    }
    if (!selectCondicionIva.value) {
        mostrarMensaje('Seleccioná una condición de IVA', 'error');
        return;
    }
    if (cliente === null && esCondicionResponsableInscripto()) {
        mostrarMensaje('Un Consumidor Final no puede emitirse con condición IVA Responsable Inscripto', 'error');
        return;
    }
    const { totalFinal } = calcularTotales();
    const importeCobrado = Number(campoCobrado.value || 0);
    if (importeCobrado < 0) {
        mostrarMensaje('El importe cobrado no puede ser negativo', 'error');
        return;
    }
    if (importeCobrado > totalFinal) {
        mostrarMensaje('El importe cobrado no puede superar el total de la factura', 'error');
        return;
    }
    const observaciones = campoObservaciones.value.trim();
    const factura = {
        clienteId: cliente?.id,
        sinCliente: cliente === null,
        condicionIvaId: Number(selectCondicionIva.value),
        puntoVentaId: Number(selectPuntoVenta.value),
        tipoMonedaId: Number(selectTipoMoneda.value),
        fechaEmision: campoFecha.value,
        importeCobrado: Number(campoCobrado.value || 0),
        observaciones: facturaEditandoId === null ? observaciones || undefined : observaciones,
        detalles: items.map((item) => ({
            listaPrecioArticuloId: item.listaPrecioArticuloId,
            cantidad: item.cantidad,
            porcentajeBonificacion: item.porcentajeBonificacion,
        })),
    };
    try {
        const creando = facturaEditandoId === null;
        const resultado = creando
            ? await facturas.crearFactura(factura)
            : await facturas.actualizarFactura(facturaEditandoId, factura);
        mostrarMensaje(`Factura N° ${resultado.numeroFactura} ${creando ? 'creada' : 'actualizada'} por ${formatoMoneda.format(resultado.importeTotal)}`, 'ok');
        cerrarFormulario();
        if (creando)
            paginaActual = 0;
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
async function informarFactura(id) {
    if (!window.confirm('¿Informar la factura a ARCA? Una vez informada no podrá modificarse.'))
        return;
    try {
        const informada = await facturas.informarArca(id);
        mostrarMensaje(`Factura N° ${informada.numeroFactura} informada a ARCA`, 'ok');
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
async function anularFactura(id, estado, informada, numero) {
    if (estado === 'ANULADA') {
        mostrarMensaje(`La factura N° ${numero || id} ya está anulada`, 'error');
        return;
    }
    if (informada !== 'true') {
        mostrarMensaje('Solo se puede anular una factura informada a ARCA', 'error');
        return;
    }
    const confirmacion = numero
        ? `¿Anular la factura N° ${numero}? Esta acción no se puede deshacer.`
        : '¿Anular este comprobante? Esta acción no se puede deshacer.';
    if (!window.confirm(confirmacion))
        return;
    const motivo = window.prompt('Motivo de la anulación (opcional):');
    if (motivo === null)
        return;
    try {
        const anulada = await facturas.actualizarEstado(id, 'ANULADA', motivo.trim() || undefined);
        mostrarMensaje(`Factura N° ${anulada.numeroFactura} anulada`, 'ok');
        if (facturaEditandoId === id) {
            cerrarFormulario();
        }
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
        await facturas.eliminarFactura(id);
        mostrarMensaje('Factura eliminada', 'ok');
        if (facturaEditandoId === id) {
            cerrarFormulario();
        }
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(mensajeDe(error), 'error');
    }
}
function buscarConFiltros() {
    filtrosActivos = leerFiltros();
    paginaActual = 0;
    void cargarFacturas();
}
function inicializar() {
    reiniciarFormulario();
    formFiltros.addEventListener('submit', (evento) => {
        evento.preventDefault();
        buscarConFiltros();
    });
    porId('btn-limpiar').addEventListener('click', () => {
        formFiltros.reset();
        buscarConFiltros();
    });
    porId('btn-nueva-factura').addEventListener('click', nuevaFactura);
    porId('btn-reporte-pdf').addEventListener('click', () => {
        window.open(facturas.urlReporte('pdf', filtrosActivos), '_blank');
    });
    porId('btn-reporte-excel').addEventListener('click', () => {
        window.open(facturas.urlReporte('excel', filtrosActivos), '_blank');
    });
    selectCondicionIva.addEventListener('change', () => {
        actualizarTipoComprobante();
    });
    selectListaPrecio.addEventListener('change', () => {
        void sincronizarItemsConListaSeleccionada(true).catch((error) => mostrarMensaje(mensajeDe(error), 'error'));
    });
    porId('btn-cancelar-formulario').addEventListener('click', cerrarFormulario);
    porId('btn-buscar-cliente').addEventListener('click', () => void elegirCliente());
    porId('btn-sin-cliente').addEventListener('click', () => {
        fijarConsumidorFinal();
    });
    porId('btn-agregar-items').addEventListener('click', () => void agregarItems());
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
    formFactura.addEventListener('submit', (evento) => {
        void guardarFactura(evento);
    });
    botonAnterior.addEventListener('click', () => {
        paginaActual -= 1;
        void cargarFacturas();
    });
    botonSiguiente.addEventListener('click', () => {
        paginaActual += 1;
        void cargarFacturas();
    });
    facturasBody.addEventListener('click', (evento) => {
        const boton = evento.target.closest('button[data-accion]');
        if (!boton || boton.disabled)
            return;
        const id = Number(boton.dataset.id);
        switch (boton.dataset.accion) {
            case 'pdf':
                window.open(facturas.urlPdfFactura(id), '_blank');
                break;
            case 'editar':
                void editarFactura(id);
                break;
            case 'informar':
                void informarFactura(id);
                break;
            case 'anular':
                void anularFactura(id, boton.dataset.estado, boton.dataset.informada, boton.dataset.numero);
                break;
            case 'eliminar':
                void eliminarFactura(id);
                break;
        }
    });
    void cargarCatalogos().catch((error) => mostrarMensaje(mensajeDe(error), 'error'));
    void cargarFacturas();
}
inicializar();
