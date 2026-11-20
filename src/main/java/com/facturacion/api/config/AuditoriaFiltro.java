package com.facturacion.api.config;

import com.facturacion.api.modelo.Usuario;
import com.facturacion.api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuditoriaFiltro extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public AuditoriaFiltro(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String usernameHeader = request.getHeader("X-Usuario");

            Usuario usuario = null;
            if (usernameHeader != null && !usernameHeader.trim().isEmpty()) {
                usuario = usuarioRepository.findByUsuario(usernameHeader.trim()).orElse(null);
            }

            if (usuario == null) {
                usuario = usuarioRepository.findByUsuario("sistema")
                        .orElseGet(() -> usuarioRepository.findByUsuario("jperez").orElse(null));
            }

            AuditoriaContext.setUsuario(usuario);
            filterChain.doFilter(request, response);
        } finally {
            AuditoriaContext.limpiar();
        }
    }
}
