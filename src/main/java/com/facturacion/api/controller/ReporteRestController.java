package com.facturacion.api.controller;

import com.facturacion.api.reportes.ReporteService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reportes")
public class ReporteRestController {

    private final ReporteService reporteService;

    public ReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdf() throws IOException {
        byte[] pdf = reporteService.generarPdf();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte_ventas.pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel() {
        byte[] tsv = reporteService.generarExcelComoTsv();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte_ventas.xls.txt").build().toString())
                .body(tsv);
    }
}
