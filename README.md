# Facturación - CRUD de Facturas de Venta

API REST con Spring Boot y frontend en TypeScript + HTML5 para gestionar facturas de venta.
Reutiliza el modelo, los DTOs y la consulta JPQL de los TPs anteriores (JPA, JPQL, Lombok y DTOs, Spring Boot).

## Cómo ejecutarlo

1. Tener JDK 17 y MySQL en marcha.
2. Recrear la base para cargar el esquema y los datos de prueba nuevos:
   `DROP DATABASE IF EXISTS facturacion_crud_db; CREATE DATABASE facturacion_crud_db;`
3. Poner la contraseña de MySQL en `src/main/resources/application.properties`.
4. Ejecutar `FacturacionApiApplication`. `DataSeeder` crea las tablas y carga los datos de prueba si no hay facturas.
5. Abrir `http://localhost:8080/`.

El JavaScript compilado ya está en `src/main/resources/static/js`. Para modificar el TypeScript:

```
cd frontend
npm install
npm run build
```

## Correcciones realizadas

| Corrección pedida | Qué se hizo | Dónde |
|---|---|---|
| Filtros: N° de factura, fechas, cliente, informada a ARCA | Cinco filtros en el formulario y en la consulta JPQL. El cliente se busca por nombre o CUIT | `FacturaFiltroDTO`, `FacturaReporteRepository`, `index.html`, `app.ts` |
| Paginación resuelta en el backend | `GET /api/facturas` devuelve una `PaginaDTO` con `COUNT` y `setFirstResult/setMaxResults`. Parámetros `page` (0 por defecto) y `size` (10 por defecto, máximo 100). La grilla tiene Anterior y Siguiente | `PaginaDTO`, `FacturaReporteRepository.buscarPagina`, `app.ts` |
| Buscar cliente y buscar ListaPrecioArticulo como explorador, sin combos | Ventana con buscador, grilla paginada en el servidor y botón por fila. Una grilla para clientes (selección simple) y otra para listas de precios (selección múltiple) | `Explorador.ts`, `ExploradorRepository`, `CatalogoRestController` (`/api/clientes`, `/api/lista-precio-articulos`) |
| Bloquear la modificación una vez informada a ARCA | Campo `informadaArca` y `fechaInformeArca`. PUT y DELETE sobre una factura informada devuelven 409 y la interfaz deshabilita todos sus botones | `FacturaVenta`, `FacturaService.validarNoInformada` |
| Informar a ARCA: guardar y luego informar con un botón aparte | Se eligió la segunda opción. La factura se puede editar cuantas veces se quiera hasta que se informa | `POST /api/facturas/{id}/informar-arca`, botón Informar a ARCA |
| Botón de editar, impedido si ya fue informada | `GET /api/facturas/{id}` devuelve los datos para cargar el formulario y el guardado usa PUT. Editar queda deshabilitado si está informada o anulada | `FacturaEdicionDTO`, `FacturaService.obtenerParaEdicion`, `app.ts` |
| Botón que abre el formulario de creación | El formulario está oculto al entrar y se abre con Nueva factura | `index.html`, `app.ts` |
| Sin ingresar IDs | Condición de IVA, punto de venta y tipo de moneda se eligen por nombre desde listas cargadas del backend. Cliente y artículos se eligen en los exploradores | `CatalogoService`, `/api/catalogos/*` |
| Seleccionar artículos de la lista sin cargarlos uno por uno | El explorador de listas de precios permite marcar varios y agregarlos juntos | `Explorador.ts` con `multiple: true` |

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/facturas?numero&fechaDesde&fechaHasta&cliente&informadaArca&page&size` | 200 con `PaginaDTO<FacturaReporteDTO>` |
| GET | `/api/facturas/{id}` | 200 con `FacturaEdicionDTO` |
| POST | `/api/facturas` | 201 con `FacturaReporteDTO` |
| PUT | `/api/facturas/{id}` | 200 con `FacturaReporteDTO` |
| POST | `/api/facturas/{id}/informar-arca` | 200 con `FacturaReporteDTO` |
| DELETE | `/api/facturas/{id}` | 204 |
| GET | `/api/clientes?texto&page&size` | 200 con `PaginaDTO<ClienteDTO>` |
| GET | `/api/lista-precio-articulos?texto&page&size` | 200 con `PaginaDTO<ListaPrecioArticuloDTO>` |
| GET | `/api/catalogos/puntos-venta`, `/condiciones-iva`, `/tipos-moneda` | 200 con lista de `CatalogoItemDTO` |

Errores: 404 si no existe el recurso, 400 si los datos son inválidos, 409 si la factura ya fue informada o está anulada. El cuerpo es `{ "status": ..., "mensaje": "..." }`.
Las opciones `estado` y `montoMinimo` siguen disponibles en el GET, aunque la pantalla ya no las muestra.

Ejemplo de alta:

```json
POST /api/facturas
{
  "clienteId": 1,
  "condicionIvaId": 1,
  "puntoVentaId": 1,
  "tipoMonedaId": 1,
  "fechaEmision": "2026-10-20",
  "detalles": [
    { "listaPrecioArticuloId": 1, "cantidad": 1 },
    { "listaPrecioArticuloId": 3, "cantidad": 2, "porcentajeBonificacion": 10 }
  ]
}
```

Para dejar una factura sin cliente al editar se envía `"sinCliente": true`.

## Reglas de negocio

- Los ítems referencian una entrada de `ListaPrecioArticulo` (artículo dentro de una lista de precios). Se quitó `articuloId` del detalle.
- Cálculo por ítem: `neto = cantidad x precio x (1 - bonificación/100)`, `IVA = neto x alícuota`, `subtotal = neto + IVA`. El precio se toma de la lista vigente al guardar.
- Una factura informada a ARCA no se puede editar, anular ni eliminar.
- Una factura anulada no se puede editar ni informar. Si no está informada, se puede eliminar.
- Informar a ARCA por ahora solo marca la factura como informada y guarda la fecha. La conexión real con ARCA se implementa en la próxima etapa.
- El IVA (21%) y el usuario de auditoría (`jperez`) se configuran en `application.properties`.

## Datos de prueba

| Entidad | Contenido |
|---|---|
| Puntos de venta | 1 Sucursal Central, 2 Sucursal Norte |
| Condiciones de IVA | Responsable Inscripto, Consumidor Final |
| Clientes | Empresa Tech S.A., Comercial Andina S.R.L. |
| Listas de precios | General (8 artículos) y Mayorista (4 artículos con 10% menos) |
| Facturas | 25: la primera informada, la tercera anulada y 5 informadas más, para probar paginación, filtros y bloqueo |

## Estructura

```
src/main/java/com/facturacion/api
  config/       CorsConfig
  controller/   FacturaRestController, CatalogoRestController, ReporteRestController
  dto/          DTOs de entrada, de salida, de paginación y de catálogos
  exception/    RecursoNoEncontradoException, ConflictoNegocioException, GlobalExceptionHandler
  modelo/       entidades JPA con Lombok
  repository/   repositorios Spring Data, FacturaReporteRepository y ExploradorRepository
  reportes/     ReporteService (PDF y TSV)
  seed/         DataSeeder
  service/      FacturaService, CatalogoService
src/main/resources
  application.properties
  static/       index.html, css/, js/ (TypeScript compilado)
frontend/src    models/, services/, components/Explorador.ts, app.ts
```
