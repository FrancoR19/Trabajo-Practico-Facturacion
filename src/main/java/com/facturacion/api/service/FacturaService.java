package com.facturacion.api.service;

import com.facturacion.api.dto.FacturaDetalleRequestDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.FacturaRequestDTO;
import com.facturacion.api.exception.ConflictoNegocioException;
import com.facturacion.api.exception.RecursoNoEncontradoException;
import com.facturacion.api.modelo.Cliente;
import com.facturacion.api.modelo.CondicionIva;
import com.facturacion.api.modelo.FacturaVenta;
import com.facturacion.api.modelo.FacturaVentaDetalle;
import com.facturacion.api.modelo.ListaPrecioArticulo;
import com.facturacion.api.modelo.PuntoVenta;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FacturaService {

    private static final String EMITIDA = "EMITIDA";
    private static final String ANULADA = "ANULADA";
    private static final Set<String> ESTADOS = Set.of(EMITIDA, "PENDIENTE", ANULADA);
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

    public FacturaService(FacturaVentaRepository facturaRepository,
                          PuntoVentaRepository puntoVentaRepository,
                          CondicionIvaRepository condicionIvaRepository,
                          TipoMonedaRepository tipoMonedaRepository,
                          ClienteRepository clienteRepository,
                          ListaPrecioArticuloRepository listaPrecioArticuloRepository,
                          UsuarioRepository usuarioRepository,
                          FacturaReporteRepository reporteRepository,
                          @Value("${facturacion.usuario-auditoria}") String usuarioAuditoria,
                          @Value("${facturacion.iva-alicuota}") double ivaAlicuota) {
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
    }

    @Transactional(readOnly = true)
    public List<FacturaReporteDTO> buscar(LocalDate fechaDesde, LocalDate fechaHasta,
                                          String estado, Double montoMinimo) {
        return reporteRepository.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO dto) {
        validarCreacion(dto);

        PuntoVenta puntoVenta = buscarPuntoVenta(dto.getPuntoVentaId());
        FacturaVenta factura = new FacturaVenta();
        factura.setPuntoVenta(puntoVenta);
        factura.setCondicionIva(buscarCondicionIva(dto.getCondicionIvaId()));
        factura.setTipoMoneda(buscarTipoMoneda(dto.getTipoMonedaId()));
        factura.setCliente(dto.getClienteId() == null ? null : buscarCliente(dto.getClienteId()));
        factura.setNumero(facturaRepository.maxNumero(puntoVenta.getId()) + 1);
        factura.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now());
        factura.setEstado(dto.getEstado() != null ? normalizarEstado(dto.getEstado()) : EMITIDA);
        factura.setObservaciones(dto.getObservaciones());
        factura.setImporteCobrado(dto.getImporteCobrado() != null ? dto.getImporteCobrado() : 0.0);

        Usuario usuario = buscarUsuarioAuditoria();
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
        FacturaVenta factura = buscarFactura(id);

        if (ANULADA.equals(factura.getEstado())) {
            throw new ConflictoNegocioException("La factura " + factura.getNumero() + " esta anulada y no puede modificarse");
        }

        if (dto.getPuntoVentaId() != null) {
            factura.setPuntoVenta(buscarPuntoVenta(dto.getPuntoVentaId()));
        }
        if (dto.getCondicionIvaId() != null) {
            factura.setCondicionIva(buscarCondicionIva(dto.getCondicionIvaId()));
        }
        if (dto.getTipoMonedaId() != null) {
            factura.setTipoMoneda(buscarTipoMoneda(dto.getTipoMonedaId()));
        }
        if (dto.getClienteId() != null) {
            factura.setCliente(buscarCliente(dto.getClienteId()));
        }
        if (dto.getFechaEmision() != null) {
            factura.setFechaEmision(dto.getFechaEmision());
        }
        if (dto.getObservaciones() != null) {
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
        factura.setUsuarioModificacion(buscarUsuarioAuditoria());

        return aReporte(facturaRepository.save(factura));
    }

    @Transactional
    public void eliminar(Long id) {
        facturaRepository.delete(buscarFactura(id));
    }

    private void validarCreacion(FacturaRequestDTO dto) {
        requerido(dto.getPuntoVentaId(), "puntoVentaId");
        requerido(dto.getCondicionIvaId(), "condicionIvaId");
        requerido(dto.getTipoMonedaId(), "tipoMonedaId");
        if (dto.getEstado() != null && ANULADA.equals(normalizarEstado(dto.getEstado()))) {
            throw new IllegalArgumentException("No se puede crear una factura ya anulada");
        }
        validarImporteCobrado(dto.getImporteCobrado());
        validarDetalles(dto.getDetalles());
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
            if (detalle.getListaPrecioArticuloId() == null && detalle.getArticuloId() == null) {
                throw new IllegalArgumentException("Cada detalle requiere articuloId o listaPrecioArticuloId");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada detalle debe ser mayor a cero");
            }
            Double bonificacion = detalle.getPorcentajeBonificacion();
            if (bonificacion != null && (bonificacion < 0 || bonificacion > 100)) {
                throw new IllegalArgumentException("El porcentaje de bonificacion debe estar entre 0 y 100");
            }
        }
    }

    private void cargarDetalles(FacturaVenta factura, List<FacturaDetalleRequestDTO> detalles) {
        for (FacturaDetalleRequestDTO dto : detalles) {
            ListaPrecioArticulo precio = resolverPrecio(dto);
            double cantidad = dto.getCantidad();
            double bonificacion = dto.getPorcentajeBonificacion() != null ? dto.getPorcentajeBonificacion() : 0.0;
            double precioUnitario = precio.getPrecioVenta();
            double neto = redondear(cantidad * precioUnitario * (1 - bonificacion / 100));
            double iva = redondear(neto * ivaAlicuota);

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(dto.getDescripcion() != null && !dto.getDescripcion().isBlank()
                    ? dto.getDescripcion()
                    : precio.getArticulo().getDenominacion());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setPorcentajeBonificacion(bonificacion);
            detalle.setImporteNeto(neto);
            detalle.setImporteIva(iva);
            detalle.setImporteSubtotal(redondear(neto + iva));
            factura.agregarDetalle(detalle);
        }
    }

    private ListaPrecioArticulo resolverPrecio(FacturaDetalleRequestDTO dto) {
        if (dto.getListaPrecioArticuloId() != null) {
            return listaPrecioArticuloRepository.findById(dto.getListaPrecioArticuloId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "ListaPrecioArticulo", dto.getListaPrecioArticuloId()));
        }
        return listaPrecioArticuloRepository.findFirstByArticuloIdOrderByIdAsc(dto.getArticuloId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Lista de precios del articulo", dto.getArticuloId()));
    }

    private void recalcularImportes(FacturaVenta factura) {
        double total = 0;
        for (FacturaVentaDetalle detalle : factura.getDetalles()) {
            total += detalle.getImporteSubtotal();
        }
        total = redondear(total);
        if (factura.getImporteCobrado() > total) {
            throw new IllegalArgumentException("El importe cobrado no puede superar el total de la factura");
        }
        factura.setImporteTotal(total);
        factura.setImporteSaldo(redondear(total - factura.getImporteCobrado()));
    }

    private void aplicarEstado(FacturaVenta factura, String estadoSolicitado) {
        String estado = normalizarEstado(estadoSolicitado);
        factura.setEstado(estado);
        if (ANULADA.equals(estado)) {
            factura.setFechaAnulacion(new Date());
        }
    }

    private String normalizarEstado(String estado) {
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "Estado invalido: " + estado + ". Valores permitidos: EMITIDA, PENDIENTE, ANULADA");
        }
        return normalizado;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private FacturaReporteDTO aReporte(FacturaVenta factura) {
        String cliente = factura.getCliente() != null
                ? factura.getCliente().getDenominacion()
                : CONSUMIDOR_FINAL;
        return new FacturaReporteDTO(
                factura.getId(),
                factura.getNumero(),
                factura.getFechaEmision(),
                cliente,
                factura.getCondicionIva().getDenominacion(),
                factura.getPuntoVenta().getDescripcion(),
                factura.getImporteTotal(),
                factura.getEstado(),
                factura.getDetalles().size());
    }

    private FacturaVenta buscarFactura(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", id));
    }

    private PuntoVenta buscarPuntoVenta(Long id) {
        return puntoVentaRepository.findById(id)
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

    private Usuario buscarUsuarioAuditoria() {
        return usuarioRepository.findByUsuario(usuarioAuditoria)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario de auditoria configurado: " + usuarioAuditoria));
    }
}
