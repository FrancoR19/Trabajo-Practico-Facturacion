package com.facturacion.api.repository;

import com.facturacion.api.modelo.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListaPrecioArticuloRepository extends JpaRepository<ListaPrecioArticulo, Long> {

    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdAsc(Long articuloId);
}
