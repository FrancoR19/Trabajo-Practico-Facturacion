package com.facturacion.api.reportes;

import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.repository.FacturaReporteRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FacturaReporteRepository reporteRepository;

    public ReporteService(FacturaReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    public List<FacturaReporteDTO> obtenerReporte() {
        return reporteRepository.buscarFacturasFiltradas(null, null, null, null);
    }

    public byte[] generarPdf() throws IOException {
        List<FacturaReporteDTO> datos = obtenerReporte();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(salida);
             PdfDocument pdf = new PdfDocument(writer);
             Document documento = new Document(pdf)) {

            documento.add(new Paragraph("Reporte Ejecutivo de Ventas").setBold().setFontSize(16));

            Table tabla = new Table(UnitValue.createPercentArray(new float[]{1, 1.3f, 2, 2, 2, 1.4f, 1.2f, 0.8f}))
                    .useAllAvailableWidth();

            String[] encabezados = {"N° Factura", "Fecha", "Cliente", "Cond. IVA", "Punto de Venta",
                    "Importe Total", "Estado", "Items"};
            for (String encabezado : encabezados) {
                tabla.addHeaderCell(new Cell().add(new Paragraph(encabezado).setBold()));
            }

            for (FacturaReporteDTO dto : datos) {
                tabla.addCell(String.valueOf(dto.getNumeroFactura()));
                tabla.addCell(dto.getFechaEmision().format(FORMATO_FECHA));
                tabla.addCell(dto.getClienteDenominacion());
                tabla.addCell(dto.getCondicionIva());
                tabla.addCell(dto.getPuntoVentaDescripcion());
                tabla.addCell(String.format("$ %.2f", dto.getImporteTotal()));
                tabla.addCell(dto.getEstado());
                tabla.addCell(String.valueOf(dto.getCantidadItems()));
            }

            documento.add(tabla);
        }
        return salida.toByteArray();
    }

    public byte[] generarExcelComoTsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("N° Factura\tFecha\tCliente\tCond. IVA\tPunto de Venta\tImporte Total\tEstado\tItems\n");

        for (FacturaReporteDTO dto : obtenerReporte()) {
            sb.append(dto.getNumeroFactura()).append('\t');
            sb.append(dto.getFechaEmision().format(FORMATO_FECHA)).append('\t');
            sb.append(dto.getClienteDenominacion()).append('\t');
            sb.append(dto.getCondicionIva()).append('\t');
            sb.append(dto.getPuntoVentaDescripcion()).append('\t');
            sb.append(String.format("%.2f", dto.getImporteTotal())).append('\t');
            sb.append(dto.getEstado()).append('\t');
            sb.append(dto.getCantidadItems()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
