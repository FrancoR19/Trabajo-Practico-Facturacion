package com.facturacion.api.modelo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@EqualsAndHashCode(callSuper = true, exclude = {"usuarioCarga", "usuarioBaja", "usuarioModificacion"})

@ToString(exclude = {"usuarioCarga", "usuarioBaja", "usuarioModificacion"})
public abstract class AuditoriaApp extends EntityId {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_alta", nullable = false)
    protected Date fechaAlta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_baja")
    protected Date fechaBaja;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_modificacion", nullable = false)
    protected Date fechaModificacion;

    @ManyToOne
    @JoinColumn(name = "usuario_carga_id", nullable = false)
    protected Usuario usuarioCarga;

    @ManyToOne
    @JoinColumn(name = "usuario_baja_id")
    protected Usuario usuarioBaja;

    @ManyToOne
    @JoinColumn(name = "usuario_modificacion_id", nullable = false)
    protected Usuario usuarioModificacion;
}
