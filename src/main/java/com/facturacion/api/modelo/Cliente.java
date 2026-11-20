package com.facturacion.api.modelo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"condicionIva", "listaPrecio"})
@ToString(callSuper = true, exclude = {"condicionIva", "listaPrecio"})
public class Cliente extends AuditoriaApp {

    @Column(nullable = false, unique = true)
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    @OneToOne
    @JoinColumn(name = "contacto_id", nullable = false, unique = true)
    private Contacto contacto;

    @OneToOne
    @JoinColumn(name = "domicilio_id", nullable = false, unique = true)
    private Domicilio domicilio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_iva_id")
    private CondicionIva condicionIva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id")
    private ListaPrecio listaPrecio;
}
