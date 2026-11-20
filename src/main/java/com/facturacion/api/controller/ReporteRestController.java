package com.facturacion.api.controller;

import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.reportes.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
public class ReporteRestController {

    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) throws IOException {
        FacturaFiltroDTO filtro = new FacturaFiltroDTO(
                numero, fechaDesde, fechaHasta, cliente, informadaArca, estado, montoMinimo);
        byte[] pdf = reporteService.generarPdf(filtro);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte_ventas.pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/facturas/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdfFactura(@PathVariable Long id) throws IOException {
        byte[] pdf = reporteService.generarPdfFactura(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("factura_" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {
        FacturaFiltroDTO filtro = new FacturaFiltroDTO(
                numero, fechaDesde, fechaHasta, cliente, informadaArca, estado, montoMinimo);
        byte[] tsv = reporteService.generarExcelComoTsv(filtro);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte_ventas.xls.txt").build().toString())
                .body(tsv);
    }
}
