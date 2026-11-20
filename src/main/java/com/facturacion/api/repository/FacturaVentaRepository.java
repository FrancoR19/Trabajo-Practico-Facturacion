package com.facturacion.api.repository;

import com.facturacion.api.modelo.FacturaVenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT COALESCE(MAX(f.numero), 0L) FROM FacturaVenta f " +
            "WHERE f.puntoVenta.id = :puntoVentaId AND f.tipoComprobante = :tipoComprobante")
    Long maxNumero(@Param("puntoVentaId") Long puntoVentaId,
                   @Param("tipoComprobante") String tipoComprobante);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FacturaVenta f WHERE f.id = :id")
    Optional<FacturaVenta> findByIdConBloqueo(@Param("id") Long id);
}
