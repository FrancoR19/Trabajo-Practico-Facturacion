package com.facturacion.api.repository;

import com.facturacion.api.modelo.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT COALESCE(MAX(f.numero), 0L) FROM FacturaVenta f WHERE f.puntoVenta.id = :puntoVentaId")
    Long maxNumero(@Param("puntoVentaId") Long puntoVentaId);
}
