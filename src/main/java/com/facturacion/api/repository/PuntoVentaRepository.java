package com.facturacion.api.repository;

import com.facturacion.api.modelo.PuntoVenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PuntoVentaRepository extends JpaRepository<PuntoVenta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM PuntoVenta pv WHERE pv.id = :id")
    Optional<PuntoVenta> findByIdConBloqueo(@Param("id") Long id);
}
