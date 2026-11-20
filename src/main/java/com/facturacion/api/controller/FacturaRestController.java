package com.facturacion.api.controller;

import com.facturacion.api.dto.FacturaEdicionDTO;
import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.FacturaRequestDTO;
import com.facturacion.api.dto.PaginaDTO;
import com.facturacion.api.reportes.ReporteService;
import com.facturacion.api.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private final FacturaService facturaService;
    private final ReporteService reporteService;

    public FacturaRestController(FacturaService facturaService, ReporteService reporteService) {
        this.facturaService = facturaService;
        this.reporteService = reporteService;
    }

    @GetMapping
    public PaginaDTO<FacturaReporteDTO> buscar(
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        FacturaFiltroDTO filtro = new FacturaFiltroDTO(
                numero, fechaDesde, fechaHasta, cliente, informadaArca, estado, montoMinimo);
        return facturaService.buscar(filtro, page, size);
    }

    @GetMapping("/{id}")
    public FacturaEdicionDTO obtener(@PathVariable Long id) {
        return facturaService.obtenerParaEdicion(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) throws IOException {
        byte[] pdf = reporteService.generarPdfFactura(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("factura_" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crear(@Valid @RequestBody FacturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizar(@PathVariable Long id, @Valid @RequestBody FacturaRequestDTO dto) {
        return ResponseEntity.ok(facturaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<FacturaReporteDTO> cambiarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.cambiarEstado(id));
    }

    @PostMapping("/{id}/informar-arca")
    public ResponseEntity<FacturaReporteDTO> informarArca(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.informarArca(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
