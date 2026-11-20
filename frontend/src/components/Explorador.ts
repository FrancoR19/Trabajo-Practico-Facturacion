import type { PaginaDTO } from '../models/factura.dto.js';

export interface ColumnaExplorador<T> {
  titulo: string;
  valor: (fila: T) => string;
}

export interface OpcionSelector {
  valor: string;
  texto: string;
}

export interface SelectorExplorador {
  titulo: string;
  opciones: () => OpcionSelector[];
  valorInicial: () => string;
}

export interface ConfiguracionExplorador<T> {
  titulo: string;
  columnas: ColumnaExplorador<T>[];
  buscar: (texto: string, pagina: number) => Promise<PaginaDTO<T>>;
  id: (fila: T) => number;
  multiple: boolean;
  selector?: SelectorExplorador;
}

function crearElemento<K extends keyof HTMLElementTagNameMap>(
  etiqueta: K,
  texto = '',
  clase = '',
): HTMLElementTagNameMap[K] {
  const elemento = document.createElement(etiqueta);
  elemento.textContent = texto;
  if (clase) elemento.className = clase;
  return elemento;
}

export class Explorador<T> {
  private readonly dialogo = crearElemento('dialog', '', 'explorador');
  private readonly campoTexto = crearElemento('input');
  private readonly campoSelector = crearElemento('select');
  private readonly cuerpo = crearElemento('tbody');
  private readonly info = crearElemento('span');
  private readonly anterior = crearElemento('button', 'Anterior', 'secundario');
  private readonly siguiente = crearElemento('button', 'Siguiente', 'secundario');
  private readonly confirmar = crearElemento('button');
  private readonly error = crearElemento('p', '', 'mensaje error');
  private readonly seleccion = new Map<number, T>();
  private pagina = 0;
  private totalPaginas = 0;
  private resolver: ((filas: T[]) => void) | null = null;

  constructor(private readonly config: ConfiguracionExplorador<T>) {
    this.construir();
  }

  abrir(): Promise<T[]> {
    this.seleccion.clear();
    this.campoTexto.value = '';
    this.pagina = 0;
    this.prepararSelector();
    this.actualizarConfirmar();
    const resultado = new Promise<T[]>((resolver) => {
      this.resolver = resolver;
    });
    this.dialogo.showModal();
    void this.cargar();
    return resultado;
  }

  private construir(): void {
    this.campoTexto.type = 'search';
    this.campoTexto.placeholder = 'Escribí para buscar...';
    this.campoTexto.addEventListener('keydown', (evento) => {
      if (evento.key === 'Enter') {
        evento.preventDefault();
        this.buscarDesdeInicio();
      }
    });

    const botonBuscar = crearElemento('button', 'Buscar');
    botonBuscar.type = 'button';
    botonBuscar.addEventListener('click', () => this.buscarDesdeInicio());

    const barra = crearElemento('div', '', 'barra-busqueda');
    barra.append(this.campoTexto, botonBuscar);

    const grupoSelector = crearElemento('div', '', 'barra-selector');
    if (this.config.selector) {
      const etiqueta = crearElemento('label', this.config.selector.titulo, 'barra-selector-etiqueta');
      etiqueta.append(this.campoSelector);
      this.campoSelector.addEventListener('change', () => this.buscarDesdeInicio());
      grupoSelector.append(etiqueta);
    } else {
      grupoSelector.hidden = true;
    }

    const encabezado = crearElemento('tr');
    encabezado.append(crearElemento('th', this.config.multiple ? 'Sel.' : ''));
    for (const columna of this.config.columnas) {
      encabezado.append(crearElemento('th', columna.titulo));
    }
    const cabecera = crearElemento('thead');
    cabecera.append(encabezado);
    const tabla = crearElemento('table');
    tabla.append(cabecera, this.cuerpo);
    const contenedorTabla = crearElemento('div', '', 'tabla-scroll');
    contenedorTabla.append(tabla);

    this.anterior.type = 'button';
    this.anterior.addEventListener('click', () => {
      this.pagina -= 1;
      void this.cargar();
    });
    this.siguiente.type = 'button';
    this.siguiente.addEventListener('click', () => {
      this.pagina += 1;
      void this.cargar();
    });
    const paginacion = crearElemento('div', '', 'paginacion');
    paginacion.append(this.anterior, this.info, this.siguiente);

    this.confirmar.type = 'button';
    this.confirmar.addEventListener('click', () => this.terminar([...this.seleccion.values()]));
    const cancelar = crearElemento('button', 'Cancelar', 'secundario');
    cancelar.type = 'button';
    cancelar.addEventListener('click', () => this.terminar([]));
    const pie = crearElemento('div', '', 'acciones');
    if (this.config.multiple) pie.append(this.confirmar);
    pie.append(cancelar);

    this.error.hidden = true;
    this.dialogo.append(crearElemento('h3', this.config.titulo), barra, grupoSelector, this.error, contenedorTabla, paginacion, pie);
    this.dialogo.addEventListener('close', () => this.terminar([]));
    document.body.append(this.dialogo);
  }

  private buscarDesdeInicio(): void {
    this.pagina = 0;
    void this.cargar();
  }

  valorSelector(): string {
    return this.config.selector ? this.campoSelector.value : '';
  }

  private prepararSelector(): void {
    const selector = this.config.selector;
    if (!selector) return;

    this.campoSelector.replaceChildren();
    for (const opcion of selector.opciones()) {
      const elemento = document.createElement('option');
      elemento.value = opcion.valor;
      elemento.textContent = opcion.texto;
      this.campoSelector.append(elemento);
    }

    const inicial = selector.valorInicial();
    if (inicial !== '') this.campoSelector.value = inicial;
    if (this.campoSelector.value === '' && this.campoSelector.options.length > 0) {
      this.campoSelector.selectedIndex = 0;
    }
  }

  private async cargar(): Promise<void> {
    try {
      this.error.hidden = true;
      const resultado = await this.config.buscar(this.campoTexto.value.trim(), this.pagina);
      this.totalPaginas = resultado.totalPaginas;
      this.renderizar(resultado);
    } catch (error) {
      this.error.textContent = error instanceof Error ? error.message : 'No se pudo cargar la información';
      this.error.hidden = false;
    }
  }

  private renderizar(resultado: PaginaDTO<T>): void {
    this.cuerpo.replaceChildren();

    if (resultado.contenido.length === 0) {
      const fila = crearElemento('tr');
      const celda = crearElemento('td', 'Sin resultados');
      celda.colSpan = this.config.columnas.length + 1;
      fila.append(celda);
      this.cuerpo.append(fila);
    }

    for (const dato of resultado.contenido) {
      const fila = crearElemento('tr');
      const celdaControl = crearElemento('td');
      const id = this.config.id(dato);

      if (this.config.multiple) {
        const casilla = crearElemento('input');
        casilla.type = 'checkbox';
        casilla.checked = this.seleccion.has(id);
        casilla.addEventListener('change', () => {
          if (casilla.checked) this.seleccion.set(id, dato);
          else this.seleccion.delete(id);
          this.actualizarConfirmar();
        });
        celdaControl.append(casilla);
      } else {
        const elegir = crearElemento('button', 'Elegir');
        elegir.type = 'button';
        elegir.addEventListener('click', () => this.terminar([dato]));
        celdaControl.append(elegir);
      }

      fila.append(celdaControl);
      for (const columna of this.config.columnas) {
        fila.append(crearElemento('td', columna.valor(dato)));
      }
      this.cuerpo.append(fila);
    }

    this.info.textContent =
      `Página ${resultado.pagina + 1} de ${Math.max(resultado.totalPaginas, 1)} (${resultado.totalElementos} resultados)`;
    this.anterior.disabled = resultado.pagina <= 0;
    this.siguiente.disabled = resultado.pagina + 1 >= this.totalPaginas;
  }

  private actualizarConfirmar(): void {
    this.confirmar.textContent = `Agregar seleccionados (${this.seleccion.size})`;
    this.confirmar.disabled = this.seleccion.size === 0;
  }

  private terminar(filas: T[]): void {
    const resolver = this.resolver;
    this.resolver = null;
    if (this.dialogo.open) this.dialogo.close();
    if (resolver) resolver(filas);
  }
}
