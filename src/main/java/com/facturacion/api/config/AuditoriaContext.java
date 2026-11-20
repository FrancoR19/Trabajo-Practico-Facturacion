package com.facturacion.api.config;

import com.facturacion.api.modelo.Usuario;

public class AuditoriaContext {

    private static final ThreadLocal<Usuario> usuarioActual = new ThreadLocal<>();

    public static void setUsuario(Usuario usuario) {
        usuarioActual.set(usuario);
    }

    public static Usuario getUsuario() {
        return usuarioActual.get();
    }

    public static void limpiar() {
        usuarioActual.remove();
    }
}
