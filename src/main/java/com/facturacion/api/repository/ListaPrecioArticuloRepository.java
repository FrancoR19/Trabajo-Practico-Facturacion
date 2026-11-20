package com.facturacion.api.repository;

import com.facturacion.api.dto.ListaPrecioArticuloDTO;
import com.facturacion.api.modelo.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    @Query("SELECT new com.facturacion.api.dto.ListaPrecioArticuloDTO(" +
            "lpa.id, l.denominacion, a.codigo, a.denominacion, lpa.precioVenta) " +
            "FROM ListaPrecioArticulo lpa JOIN lpa.listaPrecio l JOIN lpa.articulo a " +
            "WHERE l.id = :listaPrecioId AND a.codigo IN :codigos")
    List<ListaPrecioArticuloDTO> buscarPorListaYCodigos(
            @Param("listaPrecioId") Long listaPrecioId,
            @Param("codigos") List<String> codigos);
}
