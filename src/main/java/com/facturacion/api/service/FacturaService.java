package com.facturacion.api.service;

import com.facturacion.api.config.AuditoriaContext;
import com.facturacion.api.dto.FacturaDetalleRequestDTO;
import com.facturacion.api.dto.FacturaEdicionDTO;
import com.facturacion.api.dto.FacturaEdicionDetalleDTO;
import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.FacturaRequestDTO;
import com.facturacion.api.dto.PaginaDTO;
import com.facturacion.api.exception.ConflictoNegocioException;
import com.facturacion.api.exception.RecursoNoEncontradoException;
import com.facturacion.api.modelo.Cliente;
import com.facturacion.api.modelo.CondicionIva;
import com.facturacion.api.modelo.EstadoFactura;
import com.facturacion.api.modelo.FacturaVenta;
import com.facturacion.api.modelo.FacturaVentaDetalle;
import com.facturacion.api.modelo.ListaPrecioArticulo;
import com.facturacion.api.modelo.PuntoVenta;
import com.facturacion.api.modelo.TipoComprobante;
import com.facturacion.api.modelo.TipoMoneda;
import com.facturacion.api.modelo.Usuario;
import com.facturacion.api.repository.ClienteRepository;
import com.facturacion.api.repository.CondicionIvaRepository;
import com.facturacion.api.repository.FacturaReporteRepository;
import com.facturacion.api.repository.FacturaVentaRepository;
import com.facturacion.api.repository.ListaPrecioArticuloRepository;
import com.facturacion.api.repository.PuntoVentaRepository;
import com.facturacion.api.repository.TipoMonedaRepository;
import com.facturacion.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private static final EstadoFactura BORRADOR = EstadoFactura.BORRADOR;
    private static final EstadoFactura PENDIENTE = EstadoFactura.PENDIENTE;
    private static final EstadoFactura EMITIDA = EstadoFactura.EMITIDA;
    private static final EstadoFactura ANULADA = EstadoFactura.ANULADA;
    private static final String CONSUMIDOR_FINAL = "Consumidor Final";

    private final FacturaVentaRepository facturaRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ClienteRepository clienteRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;
    private final UsuarioRepository usuarioRepository;
    private final FacturaReporteRepository reporteRepository;
    private final String usuarioAuditoria;
    private final double ivaAlicuota;
    private final AfipWsaaService wsaaService;
    private final Long cuitEmisor;
    private final boolean afipEnabled;

    public FacturaService(FacturaVentaRepository facturaRepository,
                          PuntoVentaRepository puntoVentaRepository,
                          CondicionIvaRepository condicionIvaRepository,
                          TipoMonedaRepository tipoMonedaRepository,
                          ClienteRepository clienteRepository,
                          ListaPrecioArticuloRepository listaPrecioArticuloRepository,
                          UsuarioRepository usuarioRepository,
                          FacturaReporteRepository reporteRepository,
                          @Value("${facturacion.usuario-auditoria}") String usuarioAuditoria,
                          @Value("${facturacion.iva-alicuota}") double ivaAlicuota,
                          AfipWsaaService wsaaService,
                          @Value("${afip.wsaa.cuit-emisor}") Long cuitEmisor,
                          @Value("${afip.enabled:false}") boolean afipEnabled) {
        this.facturaRepository = facturaRepository;
        this.puntoVentaRepository = puntoVentaRepository;
        this.condicionIvaRepository = condicionIvaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
        this.clienteRepository = clienteRepository;
        this.listaPrecioArticuloRepository = listaPrecioArticuloRepository;
        this.usuarioRepository = usuarioRepository;
        this.reporteRepository = reporteRepository;
        this.usuarioAuditoria = usuarioAuditoria;
        this.ivaAlicuota = ivaAlicuota;
        this.wsaaService = wsaaService;
        this.cuitEmisor = cuitEmisor;
        this.afipEnabled = afipEnabled;
    }

    @Transactional(readOnly = true)
    public PaginaDTO<FacturaReporteDTO> buscar(FacturaFiltroDTO filtro, int pagina, int tamanio) {
        return reporteRepository.buscarPagina(filtro,
                PaginaDTO.normalizarPagina(pagina), PaginaDTO.normalizarTamanio(tamanio));
    }

    @Transactional(readOnly = true)
    public FacturaEdicionDTO obtenerParaEdicion(Long id) {
        FacturaVenta factura = buscarFactura(id);
        Cliente cliente = factura.getCliente();

        List<FacturaEdicionDetalleDTO> detalles = new ArrayList<>();
        for (FacturaVentaDetalle detalle : factura.getDetalles()) {
            ListaPrecioArticulo precio = detalle.getListaPrecioArticulo();
            detalles.add(new FacturaEdicionDetalleDTO(
                    precio.getId(),
                    precio.getListaPrecio().getDenominacion(),
                    precio.getArticulo().getCodigo(),
                    precio.getArticulo().getDenominacion(),
                    precio.getPrecioVenta(),
                    detalle.getCantidad(),
                    detalle.getPorcentajeBonificacion()));
        }

        return new FacturaEdicionDTO(
                factura.getId(),
                factura.getNumero(),
                factura.getFechaEmision(),
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getDenominacion() : null,
                factura.getCondicionIva().getId(),
                factura.getPuntoVenta().getId(),
                factura.getTipoMoneda().getId(),
                factura.getImporteCobrado(),
                factura.getObservaciones(),
                factura.getEstado().name(),
                factura.isInformadaArca(),
                detalles);
    }

    @Transactional
    public FacturaReporteDTO informarArca(Long id) {
        // 1. VALIDACIONES DE NEGOCIO CON BLOQUEO PESIMISTA
        FacturaVenta factura = buscarFacturaConBloqueo(id);
        validarNoInformada(factura);
        if (ANULADA.equals(factura.getEstado())) {
            throw new ConflictoNegocioException("La factura " + factura.getNumero() + " esta anulada y no puede informarse");
        }

        Usuario auditor = resolverUsuarioAuditoria();
        asegurarTipoComprobante(factura);

        // MODO SIMULACIÓN (Si afip.enabled=false en application.properties)
        if (!afipEnabled) {
            Date ahora = new Date();
            Date vencimiento = Date.from(LocalDate.now().plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
            if (factura.getNumero() == null) {
                factura.setNumero(siguienteNumero(factura.getPuntoVenta(), factura.getTipoComprobante()));
            }
            factura.setCae("74431234567890");
            factura.setCaeFechaVencimiento(vencimiento);
            factura.setResultadoAfip("FACTURA APROBADA");
            factura.setMotivoRechazo(null);
            factura.setEstado(EMITIDA);
            factura.setInformadaArca(true);
            factura.setFechaInformeArca(ahora);
            factura.setFechaModificacion(ahora);
            factura.setUsuarioModificacion(auditor);
            return aReporte(facturaRepository.save(factura));
        }

        // 2. OBTENER CONDICIÓN IVA DEL CLIENTE Y TIPO DE COMPROBANTE
        int condicionIvaReceptor = 5; // Default: Consumidor Final
        if (factura.getCondicionIva() != null) {
            condicionIvaReceptor = factura.getCondicionIva().getCodigoAfip();
        }

        int cbteTipo = TipoComprobante.codigoAfip(factura.getTipoComprobante());

        // 3. LÓGICA DE IMPORTES (Redondeo exacto a 2 decimales para AFIP)
        double total = factura.getImporteTotal();
        double impNeto = Math.round((total / 1.21) * 100.0) / 100.0;
        double impIva = Math.round((total - impNeto) * 100.0) / 100.0;

        // 4. IDENTIFICACIÓN DEL CLIENTE Y VALIDACIÓN DE CUIT
        long docNro = 0L;
        if (factura.getCliente() != null && factura.getCliente().getCuitCuil() != null) {
            String cuitSoloNumeros = factura.getCliente().getCuitCuil().replaceAll("\\D", "");
            if (!cuitSoloNumeros.isEmpty()) {
                docNro = Long.parseLong(cuitSoloNumeros);
            }
        }

        int docTipo;
        if (cbteTipo == 1) {
            docTipo = 80; // Factura A exige CUIT (80) obligatoriamente
            if (docNro == 0L) {
                throw new ConflictoNegocioException("Para emitir Factura A el cliente debe tener CUIT registrado.");
            }
        } else {
            docTipo = (docNro > 0L) ? 80 : 99; // Factura B: CUIT si tiene, o Consumidor Final (99)
        }

        // 5. LLAMADA A AFIP / ARCA
        int ptoVta = factura.getPuntoVenta().getNumero();

        String[] auth = wsaaService.obtenerTokenYSignValidos();
        String token = auth[0];
        String sign = auth[1];

        // Obtener el número correlativo estricto
        int ultimoNroAfip = AfipWsfeClient.obtenerUltimoComprobante(token, sign, cuitEmisor, ptoVta, cbteTipo);
        long nuevoNro = ultimoNroAfip + 1;

        AfipWsfeClient wsfe = new AfipWsfeClient();
        AfipWsfeClient.ResultadoCae resultadoCae = wsfe.solicitarCaeDetallado(
                token, sign,
                this.cuitEmisor,
                ptoVta,
                nuevoNro,
                cbteTipo, docTipo, docNro,
                total, impNeto, impIva, 0.00,
                condicionIvaReceptor
        );

        // 6. ACTUALIZACIÓN DE ENTIDAD Y AUDITORÍA CON ATRIBUTOS SEPARADOS
        Date ahora = new Date();
        factura.setNumero(nuevoNro);
        factura.setCae(resultadoCae.cae());
        factura.setCaeFechaVencimiento(resultadoCae.fechaVencimiento());
        factura.setResultadoAfip(resultadoCae.resultadoAfip());
        factura.setMotivoRechazo(null);
        factura.setEstado(EMITIDA);
        factura.setInformadaArca(true);
        factura.setFechaInformeArca(ahora);
        factura.setFechaModificacion(ahora);
        factura.setUsuarioModificacion(auditor);

        return aReporte(facturaRepository.save(factura));
    }

    private void validarNoInformada(FacturaVenta factura) {
        if (factura.isInformadaArca()) {
            throw new ConflictoNegocioException(
                    "La factura " + factura.getNumero() + " ya fue informada a ARCA y no puede modificarse");
        }
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO dto) {
        validarCreacion(dto);

        PuntoVenta puntoVenta = buscarPuntoVentaConBloqueo(dto.getPuntoVentaId());
        CondicionIva condicionIva = buscarCondicionIva(dto.getCondicionIvaId());
        boolean esSinCliente = dto.getClienteId() == null || Boolean.TRUE.equals(dto.getSinCliente());
        if (esSinCliente && condicionIva.getCodigoAfip() == 1) {
            throw new IllegalArgumentException("Un consumidor final no puede tener condicion IVA Responsable Inscripto");
        }
        FacturaVenta factura = new FacturaVenta();
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(buscarTipoMoneda(dto.getTipoMonedaId()));
        factura.setCliente(dto.getClienteId() == null || Boolean.TRUE.equals(dto.getSinCliente())
                ? null
                : buscarCliente(dto.getClienteId()));
        factura.setTipoComprobante(TipoComprobante.porCondicionIva(condicionIva));
        factura.setNumero(siguienteNumero(puntoVenta, factura.getTipoComprobante()));
        factura.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now());
        factura.setEstado(dto.getEstado() != null ? normalizarEstado(dto.getEstado()) : EMITIDA);
        factura.setObservaciones(dto.getObservaciones());
        factura.setImporteCobrado(dto.getImporteCobrado() != null ? dto.getImporteCobrado() : 0.0);

        Usuario usuario = resolverUsuarioAuditoria();
        Date ahora = new Date();
        factura.setFechaAlta(ahora);
        factura.setFechaModificacion(ahora);
        factura.setUsuarioCarga(usuario);
        factura.setUsuarioModificacion(usuario);

        cargarDetalles(factura, dto.getDetalles());
        recalcularImportes(factura);

        return aReporte(facturaRepository.save(factura));
    }

    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = buscarFacturaConBloqueo(id);

        if (ANULADA.equals(factura.getEstado())) {
            throw new ConflictoNegocioException("La factura " + factura.getNumero() + " esta anulada y no puede modificarse");
        }

        if (factura.isInformadaArca()) {
            if (!esSoloAnulacion(dto)) {
                validarNoInformada(factura);
            }
            if (dto.getObservaciones() != null) {
                validarLongitudObservaciones(dto.getObservaciones());
                factura.setObservaciones(dto.getObservaciones());
            }
            aplicarEstado(factura, ANULADA);
            factura.setFechaModificacion(new Date());
            factura.setUsuarioModificacion(resolverUsuarioAuditoria());
            return aReporte(facturaRepository.save(factura));
        }

        if (dto.getPuntoVentaId() != null) {
            factura.setPuntoVenta(buscarPuntoVentaConBloqueo(dto.getPuntoVentaId()));
        }
        if (dto.getCondicionIvaId() != null) {
            CondicionIva nuevaCondicion = buscarCondicionIva(dto.getCondicionIvaId());
            boolean esSinCliente = (dto.getSinCliente() != null && dto.getSinCliente())
                    || (dto.getClienteId() == null && factura.getCliente() == null);
            if (esSinCliente && nuevaCondicion.getCodigoAfip() == 1) {
                throw new IllegalArgumentException("Un consumidor final no puede tener condicion IVA Responsable Inscripto");
            }
            factura.setCondicionIva(nuevaCondicion);
        }
        factura.setTipoComprobante(TipoComprobante.porCondicionIva(factura.getCondicionIva()));
        if (dto.getTipoMonedaId() != null) {
            factura.setTipoMoneda(buscarTipoMoneda(dto.getTipoMonedaId()));
        }
        if (Boolean.TRUE.equals(dto.getSinCliente())) {
            factura.setCliente(null);
        } else if (dto.getClienteId() != null) {
            factura.setCliente(buscarCliente(dto.getClienteId()));
        }
        if (dto.getFechaEmision() != null) {
            factura.setFechaEmision(dto.getFechaEmision());
        }
        if (dto.getObservaciones() != null) {
            validarLongitudObservaciones(dto.getObservaciones());
            factura.setObservaciones(dto.getObservaciones());
        }
        if (dto.getImporteCobrado() != null) {
            validarImporteCobrado(dto.getImporteCobrado());
            factura.setImporteCobrado(dto.getImporteCobrado());
        }
        if (dto.getDetalles() != null) {
            validarDetalles(dto.getDetalles());
            factura.getDetalles().clear();
            cargarDetalles(factura, dto.getDetalles());
        }

        recalcularImportes(factura);

        if (dto.getEstado() != null) {
            aplicarEstado(factura, dto.getEstado());
        }

        factura.setFechaModificacion(new Date());
        factura.setUsuarioModificacion(resolverUsuarioAuditoria());

        return aReporte(facturaRepository.save(factura));
    }

    @Transactional
    public FacturaReporteDTO cambiarEstado(Long id) {
        FacturaVenta factura = buscarFacturaConBloqueo(id);
        if (ANULADA.equals(factura.getEstado())) {
            throw new ConflictoNegocioException("La factura " + factura.getNumero() + " ya está anulada");
        }
        if (EMITIDA.equals(factura.getEstado()) || factura.isInformadaArca()) {
            aplicarEstado(factura, ANULADA);
        } else if (PENDIENTE.equals(factura.getEstado()) || BORRADOR.equals(factura.getEstado())) {
            aplicarEstado(factura, EMITIDA);
        }
        factura.setFechaModificacion(new Date());
        factura.setUsuarioModificacion(resolverUsuarioAuditoria());
        return aReporte(facturaRepository.save(factura));
    }

    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura = buscarFacturaConBloqueo(id);
        validarNoInformada(factura);
        facturaRepository.delete(factura);
    }

    private boolean esSoloAnulacion(FacturaRequestDTO dto) {
        if (dto == null || dto.getEstado() == null || !ANULADA.equals(normalizarEstado(dto.getEstado()))) {
            return false;
        }
        return dto.getClienteId() == null
                && dto.getSinCliente() == null
                && dto.getCondicionIvaId() == null
                && dto.getPuntoVentaId() == null
                && dto.getTipoMonedaId() == null
                && dto.getFechaEmision() == null
                && dto.getImporteCobrado() == null
                && dto.getDetalles() == null;
    }

    private void validarCreacion(FacturaRequestDTO dto) {
        requerido(dto.getPuntoVentaId(), "puntoVentaId");
        requerido(dto.getCondicionIvaId(), "condicionIvaId");
        requerido(dto.getTipoMonedaId(), "tipoMonedaId");
        if (dto.getEstado() != null && ANULADA.equals(normalizarEstado(dto.getEstado()))) {
            throw new IllegalArgumentException("No se puede crear una factura ya anulada");
        }
        validarLongitudObservaciones(dto.getObservaciones());
        validarImporteCobrado(dto.getImporteCobrado());
        validarDetalles(dto.getDetalles());
    }

    private void validarLongitudObservaciones(String observaciones) {
        if (observaciones != null && observaciones.length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden superar los 500 caracteres");
        }
    }

    private void requerido(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
    }

    private void validarImporteCobrado(Double importeCobrado) {
        if (importeCobrado != null && importeCobrado < 0) {
            throw new IllegalArgumentException("El importe cobrado no puede ser negativo");
        }
    }

    private void validarDetalles(List<FacturaDetalleRequestDTO> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un detalle");
        }
        for (FacturaDetalleRequestDTO detalle : detalles) {
            if (detalle == null) {
                throw new IllegalArgumentException("Los detalles de la factura no pueden ser nulos");
            }
            if (detalle.getListaPrecioArticuloId() == null) {
                throw new IllegalArgumentException("Cada detalle requiere listaPrecioArticuloId");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada detalle debe ser mayor a cero");
            }
            Double bonificacion = detalle.getPorcentajeBonificacion();
            if (bonificacion != null && (bonificacion < 0 || bonificacion > 100)) {
                throw new IllegalArgumentException("El porcentaje de bonificacion debe estar entre 0 y 100");
            }
            if (detalle.getDescripcion() != null && detalle.getDescripcion().length() > 255) {
                throw new IllegalArgumentException("La descripcion del detalle no puede superar los 255 caracteres");
            }
        }
    }

    private void cargarDetalles(FacturaVenta factura, List<FacturaDetalleRequestDTO> detalles) {
        BigDecimal alicuotaBd = BigDecimal.valueOf(ivaAlicuota);
        BigDecimal cien = BigDecimal.valueOf(100);

        for (FacturaDetalleRequestDTO dto : detalles) {
            ListaPrecioArticulo precio = resolverPrecio(dto);
            BigDecimal cantidadBd = BigDecimal.valueOf(dto.getCantidad());
            BigDecimal bonificacionBd = dto.getPorcentajeBonificacion() != null
                    ? BigDecimal.valueOf(dto.getPorcentajeBonificacion())
                    : BigDecimal.ZERO;
            BigDecimal precioUnitarioBd = BigDecimal.valueOf(precio.getPrecioVenta());

            BigDecimal factorDescuento = BigDecimal.ONE.subtract(bonificacionBd.divide(cien, 6, RoundingMode.HALF_UP));
            BigDecimal netoSinRedondear = cantidadBd.multiply(precioUnitarioBd).multiply(factorDescuento);
            BigDecimal netoBd = netoSinRedondear.setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaBd = netoSinRedondear.multiply(alicuotaBd).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalBd = netoBd.add(ivaBd).setScale(2, RoundingMode.HALF_UP);

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(dto.getDescripcion() != null && !dto.getDescripcion().isBlank()
                    ? dto.getDescripcion()
                    : precio.getArticulo().getDenominacion());
            detalle.setCantidad(dto.getCantidad());
            detalle.setPrecioUnitario(precio.getPrecioVenta());
            detalle.setPorcentajeBonificacion(bonificacionBd.doubleValue());
            detalle.setImporteNeto(netoBd.doubleValue());
            detalle.setImporteIva(ivaBd.doubleValue());
            detalle.setImporteSubtotal(subtotalBd.doubleValue());
            factura.agregarDetalle(detalle);
        }
    }

    private ListaPrecioArticulo resolverPrecio(FacturaDetalleRequestDTO dto) {
        return listaPrecioArticuloRepository.findById(dto.getListaPrecioArticuloId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "ListaPrecioArticulo", dto.getListaPrecioArticuloId()));
    }

    private void recalcularImportes(FacturaVenta factura) {
        BigDecimal totalBd = BigDecimal.ZERO;
        for (FacturaVentaDetalle detalle : factura.getDetalles()) {
            totalBd = totalBd.add(BigDecimal.valueOf(detalle.getImporteSubtotal()));
        }
        totalBd = totalBd.setScale(2, RoundingMode.HALF_UP);
        double total = totalBd.doubleValue();
        if (factura.getImporteCobrado() > total) {
            throw new IllegalArgumentException("El importe cobrado no puede superar el total de la factura");
        }
        factura.setImporteTotal(total);
        factura.setImporteSaldo(totalBd.subtract(BigDecimal.valueOf(factura.getImporteCobrado()))
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private void aplicarEstado(FacturaVenta factura, String estadoSolicitado) {
        aplicarEstado(factura, normalizarEstado(estadoSolicitado));
    }

    private void aplicarEstado(FacturaVenta factura, EstadoFactura estado) {
        factura.setEstado(estado);
        if (ANULADA.equals(estado)) {
            Date ahora = new Date();
            factura.setFechaAnulacion(ahora);
            factura.setFechaBaja(ahora);
            factura.setUsuarioBaja(resolverUsuarioAuditoria());
        }
    }

    private EstadoFactura normalizarEstado(String estado) {
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        try {
            return EstadoFactura.valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado invalido: " + estado + ". Valores permitidos: BORRADOR, PENDIENTE, EMITIDA, ANULADA");
        }
    }

    private void asegurarTipoComprobante(FacturaVenta factura) {
        String normalizado = TipoComprobante.normalizar(factura.getTipoComprobante());
        factura.setTipoComprobante(normalizado != null && TipoComprobante.esValido(normalizado)
                ? normalizado
                : TipoComprobante.porCondicionIva(factura.getCondicionIva()));
    }

    private long siguienteNumero(PuntoVenta puntoVenta, String tipoComprobante) {
        long maxLocal = facturaRepository.maxNumero(puntoVenta.getId(), tipoComprobante);
        long siguiente = maxLocal + 1;
        if (afipEnabled) {
            try {
                String[] auth = wsaaService.obtenerTokenYSignValidos();
                int ultimoAutorizado = AfipWsfeClient.obtenerUltimoComprobante(
                        auth[0], auth[1], cuitEmisor, puntoVenta.getNumero(),
                        TipoComprobante.codigoAfip(tipoComprobante));
                if (ultimoAutorizado >= siguiente) {
                    siguiente = ultimoAutorizado + 1L;
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo consultar FECompUltimoAutorizado a ARCA para PV {} y tipo {}: {}. "
                                + "Se usa la numeracion local MAX(numero) + 1.",
                        puntoVenta.getNumero(), tipoComprobante, e.getMessage());
            }
        }
        return siguiente;
    }

    private FacturaReporteDTO aReporte(FacturaVenta factura) {
        String cliente = factura.getCliente() != null
                ? factura.getCliente().getDenominacion()
                : CONSUMIDOR_FINAL;
        String codigoMoneda = factura.getTipoMoneda() != null
                ? factura.getTipoMoneda().getCodigoAfip()
                : "PES";
        return new FacturaReporteDTO(
                factura.getId(),
                factura.getNumero(),
                factura.getFechaEmision(),
                cliente,
                factura.getCondicionIva().getDenominacion(),
                factura.getPuntoVenta().getDescripcion(),
                factura.getTipoComprobante(),
                factura.getImporteTotal(),
                factura.getEstado(),
                factura.isInformadaArca(),
                factura.getDetalles().size(),
                codigoMoneda);
    }

    private FacturaVenta buscarFactura(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
    }

    private FacturaVenta buscarFacturaConBloqueo(Long id) {
        return facturaRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
    }

    private PuntoVenta buscarPuntoVentaConBloqueo(Long id) {
        return puntoVentaRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PuntoVenta", id));
    }

    private CondicionIva buscarCondicionIva(Long id) {
        return condicionIvaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("CondicionIva", id));
    }

    private TipoMoneda buscarTipoMoneda(Long id) {
        return tipoMonedaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("TipoMoneda", id));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private Usuario resolverUsuarioAuditoria() {
        Usuario enContexto = AuditoriaContext.getUsuario();
        if (enContexto != null && enContexto.getId() != null) {
            return enContexto;
        }
        return buscarUsuarioAuditoria();
    }

    private Usuario buscarUsuarioAuditoria() {
        return usuarioRepository.findByUsuario(usuarioAuditoria)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario de auditoria configurado: " + usuarioAuditoria));
    }
}
