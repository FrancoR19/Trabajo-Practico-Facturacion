package com.facturacion.api.repository;

import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.PaginaDTO;
import com.facturacion.api.modelo.EstadoFactura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class FacturaReporteRepository {

    private static final String SELECT_BASE =
            "SELECT new com.facturacion.api.dto.FacturaReporteDTO(" +
                    "f.id, f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
                    "ci.denominacion, pv.descripcion, f.tipoComprobante, f.importeTotal, f.estado, " +
                    "f.informadaArca, COUNT(d), tm.codigoAfip) " +
                    "FROM FacturaVenta f " +
                    "LEFT JOIN f.cliente c " +
                    "JOIN f.condicionIva ci " +
                    "JOIN f.puntoVenta pv " +
                    "JOIN f.tipoMoneda tm " +
                    "LEFT JOIN f.detalles d ";

    private static final String GROUP_ORDER =
            "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, " +
                    "pv.descripcion, f.tipoComprobante, f.importeTotal, f.estado, f.informadaArca, tm.codigoAfip " +
                    "ORDER BY f.fechaEmision DESC, f.id DESC";

    private static final String COUNT_BASE =
            "SELECT COUNT(f) FROM FacturaVenta f LEFT JOIN f.cliente c ";

    @PersistenceContext
    private EntityManager em;

    public PaginaDTO<FacturaReporteDTO> buscarPagina(FacturaFiltroDTO filtro, int pagina, int tamanio) {
        Map<String, Object> params = new HashMap<>();
        String where = condiciones(filtro, params);

        TypedQuery<Long> conteo = em.createQuery(COUNT_BASE + where, Long.class);
        params.forEach(conteo::setParameter);
        long total = conteo.getSingleResult();

        TypedQuery<FacturaReporteDTO> consulta = em.createQuery(SELECT_BASE + where + GROUP_ORDER, FacturaReporteDTO.class);
        params.forEach(consulta::setParameter);
        consulta.setFirstResult(pagina * tamanio);
        consulta.setMaxResults(tamanio);

        return PaginaDTO.de(consulta.getResultList(), pagina, tamanio, total);
    }

    public List<FacturaReporteDTO> buscarTodas() {
        return buscarConFiltro(FacturaFiltroDTO.vacio());
    }

    public List<FacturaReporteDTO> buscarConFiltro(FacturaFiltroDTO filtro) {
        Map<String, Object> params = new HashMap<>();
        FacturaFiltroDTO filtroSeguro = filtro != null ? filtro : FacturaFiltroDTO.vacio();
        String where = condiciones(filtroSeguro, params);
        TypedQuery<FacturaReporteDTO> consulta = em.createQuery(SELECT_BASE + where + GROUP_ORDER, FacturaReporteDTO.class);
        params.forEach(consulta::setParameter);
        return consulta.getResultList();
    }

    private String condiciones(FacturaFiltroDTO filtro, Map<String, Object> params) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");

        if (filtro.numero() != null) {
            where.append("AND f.numero = :numero ");
            params.put("numero", filtro.numero());
        }
        if (filtro.fechaDesde() != null) {
            where.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", filtro.fechaDesde());
        }
        if (filtro.fechaHasta() != null) {
            where.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", filtro.fechaHasta());
        }
        if (hayTexto(filtro.cliente())) {
            where.append("AND (LOWER(c.denominacion) LIKE :cliente OR c.cuitCuil LIKE :cliente) ");
            params.put("cliente", "%" + filtro.cliente().trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (filtro.informadaArca() != null) {
            where.append("AND f.informadaArca = :informadaArca ");
            params.put("informadaArca", filtro.informadaArca());
        }
        if (hayTexto(filtro.estado())) {
            where.append("AND f.estado = :estado ");
            params.put("estado", estadoDe(filtro.estado()));
        }
        if (filtro.montoMinimo() != null) {
            where.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", filtro.montoMinimo());
        }
        return where.toString();
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private EstadoFactura estadoDe(String estado) {
        try {
            return EstadoFactura.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado invalido: " + estado + ". Valores permitidos: BORRADOR, PENDIENTE, EMITIDA, ANULADA");
        }
    }
}
