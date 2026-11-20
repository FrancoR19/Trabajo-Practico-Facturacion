package com.facturacion.api.service;

import com.facturacion.api.dto.CatalogoItemDTO;
import com.facturacion.api.dto.ClienteDTO;
import com.facturacion.api.dto.ListaPrecioArticuloDTO;
import com.facturacion.api.dto.PaginaDTO;
import com.facturacion.api.repository.CondicionIvaRepository;
import com.facturacion.api.repository.ExploradorRepository;
import com.facturacion.api.repository.ListaPrecioArticuloRepository;
import com.facturacion.api.repository.ListaPrecioRepository;
import com.facturacion.api.repository.PuntoVentaRepository;
import com.facturacion.api.repository.TipoMonedaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ExploradorRepository exploradorRepository;
    private final PuntoVentaRepository puntoVentaRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final TipoMonedaRepository tipoMonedaRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final ListaPrecioArticuloRepository listaPrecioArticuloRepository;

    public CatalogoService(ExploradorRepository exploradorRepository,
                           PuntoVentaRepository puntoVentaRepository,
                           CondicionIvaRepository condicionIvaRepository,
                           TipoMonedaRepository tipoMonedaRepository,
                           ListaPrecioRepository listaPrecioRepository,
                           ListaPrecioArticuloRepository listaPrecioArticuloRepository) {
        this.exploradorRepository = exploradorRepository;
        this.puntoVentaRepository = puntoVentaRepository;
        this.condicionIvaRepository = condicionIvaRepository;
        this.tipoMonedaRepository = tipoMonedaRepository;
        this.listaPrecioRepository = listaPrecioRepository;
        this.listaPrecioArticuloRepository = listaPrecioArticuloRepository;
    }

    public PaginaDTO<ClienteDTO> clientes(String texto, int pagina, int tamanio) {
        return exploradorRepository.clientes(texto,
                PaginaDTO.normalizarPagina(pagina), PaginaDTO.normalizarTamanio(tamanio));
    }

    public PaginaDTO<ListaPrecioArticuloDTO> listasPrecioArticulo(String texto, int pagina, int tamanio) {
        return listasPrecioArticulo(null, texto, pagina, tamanio);
    }

    public PaginaDTO<ListaPrecioArticuloDTO> listasPrecioArticulo(Long listaPrecioId, String texto, int pagina, int tamanio) {
        return exploradorRepository.listasPrecioArticulo(listaPrecioId, texto,
                PaginaDTO.normalizarPagina(pagina), PaginaDTO.normalizarTamanio(tamanio));
    }

    public List<ListaPrecioArticuloDTO> preciosPorListaYCodigos(Long listaPrecioId, List<String> codigos) {
        if (listaPrecioId == null || codigos == null || codigos.isEmpty()) {
            return List.of();
        }
        return listaPrecioArticuloRepository.buscarPorListaYCodigos(listaPrecioId, codigos);
    }

    public List<CatalogoItemDTO> listasPrecios() {
        return listaPrecioRepository.findAll().stream()
                .map(l -> new CatalogoItemDTO(l.getId(), l.getDenominacion()))
                .toList();
    }

    public List<CatalogoItemDTO> puntosVenta() {
        return puntoVentaRepository.findAll().stream()
                .map(p -> new CatalogoItemDTO(p.getId(), p.getNumero() + " - " + p.getDescripcion()))
                .toList();
    }

    public List<CatalogoItemDTO> condicionesIva() {
        return condicionIvaRepository.findAll().stream()
                .map(c -> new CatalogoItemDTO(c.getId(), c.getDenominacion()))
                .toList();
    }

    public List<CatalogoItemDTO> tiposMoneda() {
        return tipoMonedaRepository.findAll().stream()
                .map(t -> new CatalogoItemDTO(t.getId(), t.getDenominacion()))
                .toList();
    }
}
