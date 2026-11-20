package com.facturacion.api.controller;

import com.facturacion.api.dto.CatalogoItemDTO;
import com.facturacion.api.dto.ClienteDTO;
import com.facturacion.api.dto.ListaPrecioArticuloDTO;
import com.facturacion.api.dto.PaginaDTO;
import com.facturacion.api.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoRestController {

    private final CatalogoService catalogoService;

    public CatalogoRestController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/clientes")
    public PaginaDTO<ClienteDTO> clientes(
            @RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return catalogoService.clientes(texto, page, size);
    }

    @GetMapping("/lista-precio-articulos")
    public PaginaDTO<ListaPrecioArticuloDTO> listasPrecioArticulo(
            @RequestParam(required = false) Long listaPrecioId,
            @RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return catalogoService.listasPrecioArticulo(listaPrecioId, texto, page, size);
    }

    @GetMapping("/lista-precio-articulos/por-codigos")
    public List<ListaPrecioArticuloDTO> preciosPorListaYCodigos(
            @RequestParam Long listaPrecioId,
            @RequestParam List<String> codigos) {
        return catalogoService.preciosPorListaYCodigos(listaPrecioId, codigos);
    }

    @GetMapping("/catalogos/listas-precios")
    public List<CatalogoItemDTO> listasPrecios() {
        return catalogoService.listasPrecios();
    }

    @GetMapping("/catalogos/puntos-venta")
    public List<CatalogoItemDTO> puntosVenta() {
        return catalogoService.puntosVenta();
    }

    @GetMapping("/catalogos/condiciones-iva")
    public List<CatalogoItemDTO> condicionesIva() {
        return catalogoService.condicionesIva();
    }

    @GetMapping("/catalogos/tipos-moneda")
    public List<CatalogoItemDTO> tiposMoneda() {
        return catalogoService.tiposMoneda();
    }
}
