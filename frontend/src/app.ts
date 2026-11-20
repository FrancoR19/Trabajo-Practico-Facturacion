import { Explorador } from './components/Explorador.js';
import { CatalogoService } from './services/CatalogoService.js';
import { FacturaService } from './services/FacturaService.js';
import type {
  CatalogoItemDTO,
  ClienteDTO,
  FacturaFiltrosDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
  ListaPrecioArticuloDTO,
  PaginaDTO,
} from './models/factura.dto.js';

interface ItemFormulario {
  listaPrecioArticuloId: number;
  listaPrecio: string;
  articuloCodigo: string;
  articuloDenominacion: string;
  precioUnitario: number;
  cantidad: number;
  porcentajeBonificacion: number;
}

interface ClienteElegido {
  id: number;
  denominacion: string;
}

const TAMANIO_PAGINA = 5;

const facturas = new FacturaService();
const catalogos = new CatalogoService();
const formatoMoneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const formatoDolar = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'USD' });

function formatearImporte(valor: number, codigoMoneda = 'PES'): string {
  if (codigoMoneda === 'DOL' || codigoMoneda === 'USD') return formatoDolar.format(valor);
  if (codigoMoneda === 'PES' || codigoMoneda === 'ARS') return formatoMoneda.format(valor);
  return `${codigoMoneda} ${valor.toFixed(2)}`;
}

function etiquetaMoneda(codigo: string): string {
  if (codigo === 'PES') return 'ARS';
  if (codigo === 'DOL') return 'USD';
  return codigo;
}

function etiquetaTipo(factura: FacturaReporteDTO): string {
  const tipo = (factura.tipoComprobante || '').trim().toUpperCase();
  const etiquetas: Record<string, string> = {
    FA: 'Factura A',
    FB: 'Factura B',
    NCA: 'Nota de crédito A',
    NCB: 'Nota de crédito B',
    NDA: 'Nota de débito A',
    NDB: 'Nota de débito B',
  };
  if (etiquetas[tipo]) return etiquetas[tipo];
  if (tipo) return tipo;
  return factura.condicionIva === 'Responsable Inscripto' ? 'Factura A' : 'Factura B';
}

let items: ItemFormulario[] = [];
let cliente: ClienteElegido | null = null;
let facturaEditandoId: number | null = null;
let paginaActual = 0;
let filtrosActivos: FacturaFiltrosDTO = {};
let listasPreciosCatalogo: CatalogoItemDTO[] = [];

function porId<T extends HTMLElement>(id: string): T {
  const elemento = document.getElementById(id);
  if (!elemento) throw new Error(`No existe el elemento #${id}`);
  return elemento as T;
}

const formFiltros = porId<HTMLFormElement>('form-filtros');
const seccionFormulario = porId<HTMLElement>('seccion-formulario');
const tituloFormulario = porId<HTMLElement>('titulo-formulario');
const formFactura = porId<HTMLFormElement>('form-factura');
const botonGuardar = porId<HTMLButtonElement>('btn-guardar');
const campoCliente = porId<HTMLInputElement>('cliente-texto');
const selectCondicionIva = porId<HTMLSelectElement>('condicion-iva');
const selectPuntoVenta = porId<HTMLSelectElement>('punto-venta');
const selectTipoMoneda = porId<HTMLSelectElement>('tipo-moneda');
const selectListaPrecio = porId<HTMLSelectElement>('lista-precio');
const campoFecha = porId<HTMLInputElement>('fecha-emision');
const campoCobrado = porId<HTMLInputElement>('importe-cobrado');
const campoObservaciones = porId<HTMLInputElement>('observaciones');
const itemsBody = porId<HTMLTableSectionElement>('items-body');
const totalEstimado = porId<HTMLElement>('total-estimado');
const facturasBody = porId<HTMLTableSectionElement>('facturas-body');
const botonAnterior = porId<HTMLButtonElement>('btn-anterior');
const botonSiguiente = porId<HTMLButtonElement>('btn-siguiente');
const infoPaginacion = porId<HTMLElement>('info-paginacion');
const mensaje = porId<HTMLParagraphElement>('mensaje');

const exploradorClientes = new Explorador<ClienteDTO>({
  titulo: 'Buscar cliente',
  columnas: [
    { titulo: 'CUIT/CUIL', valor: (c) => c.cuitCuil },
    { titulo: 'Denominación', valor: (c) => c.denominacion },
  ],
  buscar: (texto, pagina) => catalogos.buscarClientes(texto, pagina),
  id: (c) => c.id,
  multiple: false,
});

const exploradorPrecios: Explorador<ListaPrecioArticuloDTO> = new Explorador<ListaPrecioArticuloDTO>({
  titulo: 'Buscar artículos en listas de precios',
  selector: {
    titulo: 'Lista de precios',
    opciones: () => listasPreciosCatalogo.map((lista) => ({ valor: String(lista.id), texto: lista.descripcion })),
    valorInicial: () => selectListaPrecio.value,
  },
  columnas: [
    { titulo: 'Lista', valor: (p) => p.listaPrecio },
    { titulo: 'Código', valor: (p) => p.articuloCodigo },
    { titulo: 'Artículo', valor: (p) => p.articuloDenominacion },
    { titulo: 'Precio', valor: (p) => formatoMoneda.format(p.precioVenta) },
  ],
  buscar: (texto, pagina) => {
    const valor = exploradorPrecios.valorSelector();
    const listaPrecioId = valor ? Number(valor) : undefined;
    return catalogos.buscarListasPrecioArticulo(texto, pagina, listaPrecioId);
  },
  id: (p) => p.id,
  multiple: true,
});

function mostrarMensaje(texto: string, tipo: 'ok' | 'error'): void {
  mensaje.textContent = texto;
  mensaje.className = `mensaje ${tipo}`;
  mensaje.hidden = false;
  mensaje.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function mensajeDe(error: unknown): string {
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado';
}

function hoy(): string {
  return new Date().toLocaleDateString('en-CA');
}

function formatearFecha(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
}

function crearCelda(texto: string): HTMLTableCellElement {
  const celda = document.createElement('td');
  celda.textContent = texto;
  return celda;
}

function crearBoton(
  texto: string,
  clase: string,
  accion: string,
  id: number,
  deshabilitado: boolean,
  motivo: string,
): HTMLButtonElement {
  const boton = document.createElement('button');
  boton.type = 'button';
  boton.textContent = texto;
  boton.className = clase;
  boton.dataset.accion = accion;
  boton.dataset.id = String(id);
  boton.disabled = deshabilitado;
  if (deshabilitado) boton.title = motivo;
  return boton;
}

function llenarSelect(select: HTMLSelectElement, opciones: CatalogoItemDTO[]): void {
  select.replaceChildren();
  for (const opcion of opciones) {
    const elemento = document.createElement('option');
    elemento.value = String(opcion.id);
    elemento.textContent = opcion.descripcion;
    select.append(elemento);
  }
}

async function cargarCatalogos(): Promise<void> {
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
  listasPreciosCatalogo = listasPrecios;
}

function leerFiltros(): FacturaFiltrosDTO {
  const valor = (id: string): string => porId<HTMLInputElement>(id).value.trim();
  const arca = porId<HTMLSelectElement>('filtro-arca').value;
  return {
    numero: valor('filtro-numero') === '' ? undefined : Number(valor('filtro-numero')),
    fechaDesde: valor('filtro-desde') || undefined,
    fechaHasta: valor('filtro-hasta') || undefined,
    cliente: valor('filtro-cliente') || undefined,
    informadaArca: arca === '' ? undefined : arca === 'true',
  };
}

function renderFacturas(pagina: PaginaDTO<FacturaReporteDTO>): void {
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
    acciones.append(
      crearBoton('PDF', 'secundario', 'pdf', factura.id, false, ''),
      crearBoton('Editar', 'secundario', 'editar', factura.id, informada || anulada, informada ? motivoInformada : motivoAnulada),
      crearBoton('Informar a ARCA', '', 'informar', factura.id, informada || anulada, informada ? motivoInformada : motivoAnulada),
      botonAnular,
      crearBoton('Eliminar', 'peligro', 'eliminar', factura.id, informada, motivoNoEliminable),
    );

    const fila = document.createElement('tr');
    if (facturaEditandoId !== null && factura.id === facturaEditandoId) {
      fila.classList.add('editando');
    }
    fila.append(
      crearCelda(String(factura.numeroFactura ?? '-')),
      crearCelda(etiquetaTipo(factura)),
      crearCelda(formatearFecha(factura.fechaEmision)),
      crearCelda(factura.clienteDenominacion),
      crearCelda(factura.condicionIva),
      crearCelda(factura.puntoVentaDescripcion),
      crearCelda(formatearImporte(factura.importeTotal, moneda)),
      crearCelda(etiquetaMoneda(moneda)),
      crearCelda(factura.estado),
      estadoArca,
      crearCelda(String(factura.cantidadItems)),
      acciones,
    );
    facturasBody.append(fila);
  }

  infoPaginacion.textContent =
    `Página ${pagina.pagina + 1} de ${Math.max(pagina.totalPaginas, 1)} (${pagina.totalElementos} comprobantes)`;
  botonAnterior.disabled = pagina.pagina <= 0;
  botonSiguiente.disabled = pagina.pagina + 1 >= pagina.totalPaginas;
}

async function cargarFacturas(): Promise<void> {
  try {
    const pagina = await facturas.obtenerFacturas(filtrosActivos, paginaActual, TAMANIO_PAGINA);
    if (pagina.contenido.length === 0 && paginaActual > 0) {
      paginaActual = Math.max(pagina.totalPaginas - 1, 0);
      await cargarFacturas();
      return;
    }
    renderFacturas(pagina);
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

function subtotalNeto(item: ItemFormulario): number {
  return item.cantidad * item.precioUnitario * (1 - item.porcentajeBonificacion / 100);
}

function actualizarTotalEstimado(): void {
  const total = items.reduce((suma, item) => suma + subtotalNeto(item), 0);
  totalEstimado.textContent = `Total neto estimado (sin IVA): ${formatoMoneda.format(total)}`;
}

function crearInputItem(campo: string, indice: number, valor: number, atributos: Record<string, string>): HTMLTableCellElement {
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

function renderItems(): void {
  itemsBody.replaceChildren();

  if (items.length === 0) {
    const fila = document.createElement('tr');
    const celda = crearCelda('Todavía no hay ítems. Usá «Agregar ítems» para elegirlos de las listas de precios.');
    celda.colSpan = 8;
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
    fila.append(
      crearCelda(item.listaPrecio),
      crearCelda(item.articuloCodigo),
      crearCelda(item.articuloDenominacion),
      crearCelda(formatoMoneda.format(item.precioUnitario)),
      crearInputItem('cantidad', indice, item.cantidad, { min: '0.01', step: 'any', required: 'true' }),
      crearInputItem('porcentajeBonificacion', indice, item.porcentajeBonificacion, { min: '0', max: '100', step: 'any' }),
      subtotal,
      celdaQuitar,
    );
    itemsBody.append(fila);
  });

  actualizarTotalEstimado();
}

function renderCliente(): void {
  campoCliente.value = cliente ? cliente.denominacion : 'Consumidor Final';
}

function reiniciarFormulario(): void {
  formFactura.reset();
  items = [];
  cliente = null;
  facturaEditandoId = null;
  campoFecha.value = hoy();
  campoCobrado.value = '0';
  renderCliente();
  renderItems();
}

function abrirFormulario(titulo: string, editando = false): void {
  tituloFormulario.textContent = titulo;
  botonGuardar.textContent = editando ? 'Guardar cambios' : 'Guardar factura';
  seccionFormulario.classList.toggle('modo-edicion', editando);
  seccionFormulario.hidden = false;
  seccionFormulario.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function cerrarFormulario(): void {
  seccionFormulario.hidden = true;
  seccionFormulario.classList.remove('modo-edicion');
  reiniciarFormulario();
}

function nuevaFactura(): void {
  reiniciarFormulario();
  abrirFormulario('Nueva factura');
}

async function editarFactura(id: number): Promise<void> {
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
    selectCondicionIva.value = String(factura.condicionIvaId);
    selectPuntoVenta.value = String(factura.puntoVentaId);
    selectTipoMoneda.value = String(factura.tipoMonedaId);
    campoFecha.value = factura.fechaEmision;
    campoCobrado.value = String(factura.importeCobrado);
    campoObservaciones.value = factura.observaciones ?? '';
    items = factura.detalles.map((detalle) => ({ ...detalle }));

    renderCliente();
    renderItems();
    abrirFormulario(`EDITAR FACTURA — N° ${factura.numero}`, true);
    void cargarFacturas();
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

async function elegirCliente(): Promise<void> {
  const [elegido] = await exploradorClientes.abrir();
  if (elegido) {
    cliente = { id: elegido.id, denominacion: elegido.denominacion };
    renderCliente();
  }
}

async function agregarItems(): Promise<void> {
  const elegidos = await exploradorPrecios.abrir();
  for (const precio of elegidos) {
    const existente = items.find((item) => item.listaPrecioArticuloId === precio.id);
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

function actualizarItem(input: HTMLInputElement): void {
  const item = items[Number(input.dataset.indice)];
  if (!item) return;

  if (input.dataset.campo === 'cantidad') item.cantidad = Number(input.value);
  if (input.dataset.campo === 'porcentajeBonificacion') item.porcentajeBonificacion = Number(input.value);

  const celda = input.closest('tr')?.querySelector<HTMLElement>('[data-subtotal]');
  if (celda) celda.textContent = formatoMoneda.format(subtotalNeto(item));
  actualizarTotalEstimado();
}

async function guardarFactura(evento: SubmitEvent): Promise<void> {
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

  const observaciones = campoObservaciones.value.trim();
  const factura: FacturaRequestDTO = {
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
      : await facturas.actualizarFactura(facturaEditandoId as number, factura);

    mostrarMensaje(
      `Factura N° ${resultado.numeroFactura} ${creando ? 'creada' : 'actualizada'} por ${formatoMoneda.format(resultado.importeTotal)}`,
      'ok',
    );
    cerrarFormulario();
    if (creando) paginaActual = 0;
    await cargarFacturas();
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

async function informarFactura(id: number): Promise<void> {
  if (!window.confirm('¿Informar la factura a ARCA? Una vez informada no podrá modificarse.')) return;

  try {
    const informada = await facturas.informarArca(id);
    mostrarMensaje(`Factura N° ${informada.numeroFactura} informada a ARCA`, 'ok');
    await cargarFacturas();
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

async function anularFactura(id: number, estado?: string, informada?: string, numero?: string): Promise<void> {
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
  if (!window.confirm(confirmacion)) return;

  const motivo = window.prompt('Motivo de la anulación (opcional):');
  if (motivo === null) return;

  try {
    const anulada = await facturas.actualizarEstado(id, 'ANULADA', motivo.trim() || undefined);
    mostrarMensaje(`Factura N° ${anulada.numeroFactura} anulada`, 'ok');
    await cargarFacturas();
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

async function eliminarFactura(id: number): Promise<void> {
  if (!window.confirm('¿Eliminar definitivamente esta factura?')) return;

  try {
    await facturas.eliminarFactura(id);
    mostrarMensaje('Factura eliminada', 'ok');
    await cargarFacturas();
  } catch (error) {
    mostrarMensaje(mensajeDe(error), 'error');
  }
}

function buscarConFiltros(): void {
  filtrosActivos = leerFiltros();
  paginaActual = 0;
  void cargarFacturas();
}

function inicializar(): void {
  reiniciarFormulario();

  formFiltros.addEventListener('submit', (evento) => {
    evento.preventDefault();
    buscarConFiltros();
  });

  porId<HTMLButtonElement>('btn-limpiar').addEventListener('click', () => {
    formFiltros.reset();
    buscarConFiltros();
  });

  porId<HTMLButtonElement>('btn-nueva-factura').addEventListener('click', nuevaFactura);
  porId<HTMLButtonElement>('btn-reporte-pdf').addEventListener('click', () => {
    window.open(facturas.urlReporte('pdf', filtrosActivos), '_blank');
  });
  porId<HTMLButtonElement>('btn-reporte-excel').addEventListener('click', () => {
    window.open(facturas.urlReporte('excel', filtrosActivos), '_blank');
  });
  porId<HTMLButtonElement>('btn-cancelar-formulario').addEventListener('click', cerrarFormulario);
  porId<HTMLButtonElement>('btn-buscar-cliente').addEventListener('click', () => void elegirCliente());
  porId<HTMLButtonElement>('btn-sin-cliente').addEventListener('click', () => {
    cliente = null;
    renderCliente();
  });
  porId<HTMLButtonElement>('btn-agregar-items').addEventListener('click', () => void agregarItems());

  itemsBody.addEventListener('input', (evento) => {
    if (evento.target instanceof HTMLInputElement) actualizarItem(evento.target);
  });

  itemsBody.addEventListener('click', (evento) => {
    const boton = (evento.target as HTMLElement).closest<HTMLButtonElement>('button[data-accion="quitar"]');
    if (!boton) return;
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
    const boton = (evento.target as HTMLElement).closest<HTMLButtonElement>('button[data-accion]');
    if (!boton || boton.disabled) return;
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
