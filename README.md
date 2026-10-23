# Facturación - CRUD de Facturas de Venta

API REST con Spring Boot (POST, PUT, DELETE) y frontend en TypeScript + HTML5 para gestionar facturas de venta.
Reutiliza el modelo, los DTOs y la consulta JPQL de los TPs anteriores (JPA, JPQL, Lombok y DTOs, Spring Boot).

## Cómo ejecutarlo

1. Tener Java 17 o superior y MySQL en marcha.
2. Crear la base vacía: `CREATE DATABASE facturacion_crud_db;`
3. Poner la contraseña de MySQL en `src/main/resources/application.properties` (`spring.datasource.password`).
4. Ejecutar `FacturacionApiApplication`. Al arrancar, `DataSeeder` crea las tablas y carga datos de prueba (solo si no hay facturas).
5. Abrir `http://localhost:8080/` para usar el frontend.

El JavaScript compilado ya está en `src/main/resources/static/js`. Para modificar el TypeScript:

```
cd frontend
npm install
npm run build
```

## Partes del TP realizadas

| Parte | Consigna | Dónde está |
|---|---|---|
| 1 | DTOs de entrada | `dto/FacturaRequestDTO.java`, `dto/FacturaDetalleRequestDTO.java` |
| 2 | Lógica en `FacturaService` | `service/FacturaService.java` |
| 2.a | `@Transactional` en las escrituras | `crear`, `actualizar`, `eliminar` |
| 2.b | Crear: buscar entidades por ID, calcular importes, guardar en cascada | `FacturaService.crear` |
| 2.c | Actualizar cabecera e ítems | `FacturaService.actualizar` |
| 2.d | Eliminar / anular | `FacturaService.eliminar` (física) y `actualizar` con estado `ANULADA` (lógica) |
| 3 | Endpoints REST | `controller/FacturaRestController.java` |
| 4 | Frontend CRUD | `frontend/` y `src/main/resources/static/` |
| 5 | Interfaces TypeScript espejo de los DTOs | `frontend/src/models/factura.dto.ts` |
| 6 | Capa de servicio HTTP | `frontend/src/services/FacturaService.ts` |
| 7 | Interfaz HTML y lógica de UI | `static/index.html`, `frontend/src/app.ts` |

### Parte 1: DTOs de entrada

- `FacturaRequestDTO`: `clienteId`, `condicionIvaId`, `puntoVentaId`, `tipoMonedaId`, `fechaEmision`, `estado`, `importeCobrado`, `observaciones` y `detalles`.
- `FacturaDetalleRequestDTO`: `articuloId` o `listaPrecioArticuloId`, `cantidad`, `porcentajeBonificacion` y `descripcion`.
- Las relaciones se envían solo con IDs, nunca con entidades anidadas.

### Parte 2: servicio

- Crear: valida los datos, busca `PuntoVenta`, `CondicionIva`, `TipoMoneda`, `Cliente` y `ListaPrecioArticulo` por ID (lanza `RecursoNoEncontradoException` si no existen), calcula importes, asigna el número de factura y guarda en cascada los detalles.
- Cálculo por ítem: `neto = cantidad x precio x (1 - bonificación/100)`, `IVA = neto x alícuota`, `subtotal = neto + IVA`. La factura suma los subtotales y calcula `importeSaldo = total - cobrado`.
- Actualizar: PUT parcial. Solo se modifican los campos enviados. Si llegan `detalles`, se reemplazan todos los ítems y se recalculan los importes. Si llega `estado` `ANULADA`, se guarda `fechaAnulacion = new Date()`.
- Eliminar: borrado físico de la factura y sus detalles (cascada).
- Errores: `exception/GlobalExceptionHandler.java` los convierte en 404, 400 y 409 con el cuerpo `{ "status": ..., "mensaje": "..." }`.

### Parte 3: endpoints (`/api/facturas`)

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/facturas?fechaDesde&fechaHasta&estado&montoMinimo` | 200 con la lista de `FacturaReporteDTO` |
| POST | `/api/facturas` | 201 con el `FacturaReporteDTO` creado |
| PUT | `/api/facturas/{id}` | 200 con el `FacturaReporteDTO` actualizado |
| DELETE | `/api/facturas/{id}` | 204 sin cuerpo |

Otros códigos: 404 si la factura o una entidad asociada no existe, 400 si los datos son inválidos, 409 si se intenta modificar una factura anulada.

Ejemplos de cuerpo:

```json
POST /api/facturas
{
  "clienteId": 1,
  "condicionIvaId": 1,
  "puntoVentaId": 1,
  "tipoMonedaId": 1,
  "fechaEmision": "2026-10-20",
  "detalles": [
    { "articuloId": 1, "cantidad": 1 },
    { "articuloId": 2, "cantidad": 2, "porcentajeBonificacion": 10 }
  ]
}
```

```json
PUT /api/facturas/1
{ "estado": "ANULADA", "observaciones": "Error de carga" }
```

```json
PUT /api/facturas/1
{ "detalles": [ { "articuloId": 2, "cantidad": 3 } ] }
```

### Partes 4 a 7: frontend

- `factura.dto.ts`: `FacturaReporteDTO`, `FacturaRequestDTO`, `FacturaDetalleRequestDTO` y `FacturaFiltrosDTO`, con los mismos nombres de campo que los DTOs de Java (`Long` y `double` son `number`, `String` y `LocalDate` son `string`).
- `FacturaService.ts`: `obtenerFacturas`, `crearFactura`, `actualizarEstado` y `eliminarFactura`, con `async/await` y `fetch` sobre `http://localhost:8080/api/facturas`.
- `index.html` tiene tres secciones: búsqueda con filtros, alta de factura con tabla dinámica de ítems y listado de comprobantes con botones Anular y Eliminar por fila.
- `app.ts` mantiene los ítems en memoria, manipula el DOM y conecta los eventos con el servicio.

## Datos de prueba (base nueva)

| Entidad | IDs |
|---|---|
| Puntos de venta | 1 Sucursal Central, 2 Sucursal Norte |
| Condiciones de IVA | 1 Responsable Inscripto, 2 Consumidor Final |
| Tipos de moneda | 1 Peso, 2 Dólar |
| Clientes | 1 Empresa Tech S.A., 2 Comercial Andina S.R.L. |
| Artículos | 1 Notebook ($500.000), 2 Mouse ($18.000), 3 Teclado ($55.000) |

Se cargan 3 facturas: la 1 con cliente, la 2 a Consumidor Final y la 3 anulada. La numeración es por punto de venta.

## Decisiones y supuestos

- El detalle acepta `articuloId` (usa la primera lista de precios cargada para ese artículo) o `listaPrecioArticuloId` (usa exactamente esa entrada de precio). Con ambos, se usa `listaPrecioArticuloId`.
- El IVA es 21% y está en `facturacion.iva-alicuota` (`application.properties`), porque el modelo no guarda la alícuota por artículo.
- Los campos de auditoría usan el usuario `facturacion.usuario-auditoria` (`jperez`), porque el TP no incluye autenticación.
- `FacturaReporteDTO` agrega `id` y `estado` respecto de los TPs anteriores, necesarios para que el frontend pueda anular y eliminar por fila.
- `fechaEmision` pasó de `Date` a `LocalDate`, para que el JSON sea `yyyy-MM-dd` sin depender de la zona horaria.
- Una factura anulada no puede modificarse (409).
- Limitaciones: por PUT no se puede volver a dejar una factura sin cliente (un campo ausente significa "sin cambios"), y el número de factura no está protegido contra creaciones simultáneas.
- Los endpoints de reportes (`/api/reportes/pdf` y `/api/reportes/excel`) vienen del TP anterior y siguen funcionando.
- `CorsConfig` permite abrir el frontend desde otro puerto de `localhost`.

## Estructura

```
src/main/java/com/facturacion/api
  config/       CorsConfig
  controller/   FacturaRestController, ReporteRestController
  dto/          FacturaRequestDTO, FacturaDetalleRequestDTO, FacturaReporteDTO, ErrorDTO
  exception/    RecursoNoEncontradoException, ConflictoNegocioException, GlobalExceptionHandler
  modelo/       entidades JPA con Lombok
  repository/   repositorios Spring Data y FacturaReporteRepository (JPQL dinámico)
  reportes/     ReporteService (PDF y TSV)
  seed/         DataSeeder
  service/      FacturaService
src/main/resources
  application.properties
  static/       index.html, css/, js/ (TypeScript compilado)
frontend/       código fuente TypeScript y tsconfig
```
