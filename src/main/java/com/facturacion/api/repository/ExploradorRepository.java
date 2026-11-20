package com.facturacion.api.repository;

import com.facturacion.api.dto.ClienteDTO;
import com.facturacion.api.dto.ListaPrecioArticuloDTO;
import com.facturacion.api.dto.PaginaDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class ExploradorRepository {

    @PersistenceContext
    private EntityManager em;

    public PaginaDTO<ClienteDTO> clientes(String texto, int pagina, int tamanio) {
        String filtro = "WHERE LOWER(c.denominacion) LIKE :texto OR c.cuitCuil LIKE :texto ";
        return paginar(
                "SELECT new com.facturacion.api.dto.ClienteDTO(c.id, c.cuitCuil, c.denominacion) " +
                        "FROM Cliente c " + filtro + "ORDER BY c.denominacion",
                "SELECT COUNT(c) FROM Cliente c " + filtro,
                ClienteDTO.class, texto, pagina, tamanio);
    }

    public PaginaDTO<ListaPrecioArticuloDTO> listasPrecioArticulo(String texto, int pagina, int tamanio) {
        return listasPrecioArticulo(null, texto, pagina, tamanio);
    }

    public PaginaDTO<ListaPrecioArticuloDTO> listasPrecioArticulo(Long listaPrecioId, String texto, int pagina, int tamanio) {
        String desde = "FROM ListaPrecioArticulo lpa JOIN lpa.listaPrecio l JOIN lpa.articulo a ";
        String filtro = "WHERE (LOWER(a.denominacion) LIKE :texto OR LOWER(a.codigo) LIKE :texto " +
                "OR LOWER(l.denominacion) LIKE :texto) " +
                (listaPrecioId != null ? "AND l.id = :listaPrecioId " : "");
        return paginar(
                "SELECT new com.facturacion.api.dto.ListaPrecioArticuloDTO(" +
                        "lpa.id, l.denominacion, a.codigo, a.denominacion, lpa.precioVenta) " +
                        desde + filtro + "ORDER BY a.denominacion, l.denominacion",
                "SELECT COUNT(lpa) " + desde + filtro,
                ListaPrecioArticuloDTO.class, texto, listaPrecioId, pagina, tamanio);
    }

    private <T> PaginaDTO<T> paginar(String jpql, String jpqlConteo, Class<T> tipo,
                                     String texto, int pagina, int tamanio) {
        return paginar(jpql, jpqlConteo, tipo, texto, null, pagina, tamanio);
    }

    private <T> PaginaDTO<T> paginar(String jpql, String jpqlConteo, Class<T> tipo,
                                     String texto, Long listaPrecioId, int pagina, int tamanio) {
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";

        TypedQuery<Long> conteoQuery = em.createQuery(jpqlConteo, Long.class)
                .setParameter("texto", patron);
        TypedQuery<T> contenidoQuery = em.createQuery(jpql, tipo)
                .setParameter("texto", patron);

        if (listaPrecioId != null) {
            conteoQuery.setParameter("listaPrecioId", listaPrecioId);
            contenidoQuery.setParameter("listaPrecioId", listaPrecioId);
        }

        long total = conteoQuery.getSingleResult();
        List<T> contenido = contenidoQuery
                .setFirstResult(pagina * tamanio)
                .setMaxResults(tamanio)
                .getResultList();

        return PaginaDTO.de(contenido, pagina, tamanio, total);
    }
}
