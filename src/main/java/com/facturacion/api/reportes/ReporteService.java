package com.facturacion.api.reportes;

import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.exception.RecursoNoEncontradoException;
import com.facturacion.api.modelo.Cliente;
import com.facturacion.api.modelo.EstadoFactura;
import com.facturacion.api.modelo.FacturaVenta;
import com.facturacion.api.modelo.FacturaVentaDetalle;
import com.facturacion.api.modelo.PuntoVenta;
import com.facturacion.api.modelo.TipoComprobante;
import com.facturacion.api.repository.FacturaReporteRepository;
import com.facturacion.api.repository.FacturaVentaRepository;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_GENERACION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String URL_QR_AFIP = "https://www.afip.gob.ar/fe/qr/?p=";

    private final FacturaReporteRepository reporteRepository;
    private final FacturaVentaRepository facturaRepository;
    private final Long cuitEmisor;

    public ReporteService(FacturaReporteRepository reporteRepository,
                          FacturaVentaRepository facturaRepository,
                          @Value("${afip.wsaa.cuit-emisor}") Long cuitEmisor) {
        this.reporteRepository = reporteRepository;
        this.facturaRepository = facturaRepository;
        this.cuitEmisor = cuitEmisor;
    }

    public List<FacturaReporteDTO> obtenerReporte() {
        return reporteRepository.buscarTodas();
    }

    public List<FacturaReporteDTO> obtenerReporte(FacturaFiltroDTO filtro) {
        return reporteRepository.buscarConFiltro(filtro);
    }

    public byte[] generarPdf() throws IOException {
        return generarPdf(FacturaFiltroDTO.vacio());
    }

    public byte[] generarPdf(FacturaFiltroDTO filtro) throws IOException {
        List<FacturaReporteDTO> datos = obtenerReporte(filtro);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(salida);
             PdfDocument pdf = new PdfDocument(writer);
             Document documento = new Document(pdf, PageSize.A4.rotate())) {

            documento.add(new Paragraph("Reporte Ejecutivo de Ventas").setBold().setFontSize(16));
            documento.add(new Paragraph("Fecha de generación: " + LocalDateTime.now().format(FORMATO_GENERACION)
                    + " | Cantidad de registros: " + datos.size()).setFontSize(10));

            if (datos.isEmpty()) {
                documento.add(new Paragraph("No se encontraron comprobantes para los criterios seleccionados.")
                        .setItalic().setFontSize(11));
                return salida.toByteArray();
            }

            Table tabla = new Table(UnitValue.createPercentArray(new float[]{0.9f, 1.2f, 2.2f, 1.8f, 1.8f, 0.8f, 1.4f, 1.1f, 0.9f}))
                    .useAllAvailableWidth();

            String[] encabezados = {
                    "N° Factura", "Fecha", "Cliente", "Cond. IVA",
                    "Punto de Venta", "Ítems", "Importe Total", "Estado", "Moneda"
            };
            for (String encabezado : encabezados) {
                tabla.addHeaderCell(new Cell()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .add(new Paragraph(encabezado).setBold().setFontSize(10)));
            }

            Map<String, BigDecimal> totalesEmitidasPorMoneda = new LinkedHashMap<>();

            for (FacturaReporteDTO dto : datos) {
                String moneda = dto.getCodigoMoneda() != null ? dto.getCodigoMoneda() : "PES";
                totalesEmitidasPorMoneda.putIfAbsent(moneda, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

                if ("EMITIDA".equalsIgnoreCase(dto.getEstado())) {
                    BigDecimal actual = totalesEmitidasPorMoneda.get(moneda);
                    BigDecimal suma = actual.add(BigDecimal.valueOf(dto.getImporteTotal())).setScale(2, RoundingMode.HALF_UP);
                    totalesEmitidasPorMoneda.put(moneda, suma);
                }

                String nroTexto = dto.getNumeroFactura() != null ? String.valueOf(dto.getNumeroFactura()) : "-";
                String simbolo = simboloMoneda(moneda);
                String monedaVisual = codigoMonedaVisual(moneda);

                tabla.addCell(new Paragraph(nroTexto).setFontSize(9));
                tabla.addCell(new Paragraph(dto.getFechaEmision().format(FORMATO_FECHA)).setFontSize(9));
                tabla.addCell(new Paragraph(dto.getClienteDenominacion()).setFontSize(9));
                tabla.addCell(new Paragraph(dto.getCondicionIva()).setFontSize(9));
                tabla.addCell(new Paragraph(dto.getPuntoVentaDescripcion()).setFontSize(9));
                tabla.addCell(new Paragraph(String.valueOf(dto.getCantidadItems())).setFontSize(9));
                tabla.addCell(new Paragraph(String.format(Locale.US, "%s %.2f", simbolo, dto.getImporteTotal())).setFontSize(9));
                tabla.addCell(new Paragraph(dto.getEstado()).setFontSize(9));
                tabla.addCell(new Paragraph(monedaVisual).setFontSize(9));
            }

            documento.add(tabla);

            documento.add(new Paragraph("\nTotales Facturados (únicamente estado EMITIDA por moneda):")
                    .setBold().setFontSize(11));
            for (Map.Entry<String, BigDecimal> entry : totalesEmitidasPorMoneda.entrySet()) {
                String moneda = entry.getKey();
                String simbolo = simboloMoneda(moneda);
                documento.add(new Paragraph(String.format(Locale.US, "• %s (%s): %.2f",
                        codigoMonedaVisual(moneda), simbolo, entry.getValue().doubleValue())).setFontSize(10));
            }
        }
        return salida.toByteArray();
    }

    public byte[] generarExcelComoTsv() {
        return generarExcelComoTsv(FacturaFiltroDTO.vacio());
    }

    public byte[] generarExcelComoTsv(FacturaFiltroDTO filtro) {
        StringBuilder sb = new StringBuilder();
        sb.append("N° Factura\tFecha\tCliente\tCond. IVA\tPunto de Venta\tItems\tImporte Total\tEstado\tMoneda\n");

        for (FacturaReporteDTO dto : obtenerReporte(filtro)) {
            String moneda = dto.getCodigoMoneda() != null ? dto.getCodigoMoneda() : "PES";
            String nro = dto.getNumeroFactura() != null ? String.valueOf(dto.getNumeroFactura()) : "";

            sb.append(escaparTsv(nro)).append('\t');
            sb.append(dto.getFechaEmision().toString()).append('\t');
            sb.append(escaparTsv(dto.getClienteDenominacion())).append('\t');
            sb.append(escaparTsv(dto.getCondicionIva())).append('\t');
            sb.append(escaparTsv(dto.getPuntoVentaDescripcion())).append('\t');
            sb.append(dto.getCantidadItems()).append('\t');
            sb.append(String.format(Locale.US, "%.2f", dto.getImporteTotal())).append('\t');
            sb.append(escaparTsv(dto.getEstado())).append('\t');
            sb.append(escaparTsv(codigoMonedaVisual(moneda))).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfFactura(Long facturaId) throws IOException {
        FacturaVenta factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura", facturaId));

        String tipoComprobante = TipoComprobante.descripcion(factura.getTipoComprobante());
        int tipoCmpAfip = TipoComprobante.codigoAfip(factura.getTipoComprobante());
        String codigoMonedaAfip = factura.getTipoMoneda() != null && factura.getTipoMoneda().getCodigoAfip() != null
                ? factura.getTipoMoneda().getCodigoAfip()
                : "PES";
        String monedaVisualCompleta = monedaVisualConSimbolo(codigoMonedaAfip);
        String simbolo = simboloMoneda(codigoMonedaAfip);

        boolean conCae = factura.getCae() != null && !factura.getCae().isBlank();
        boolean conVencimientoCae = factura.getCaeFechaVencimiento() != null;
        boolean esAnulada = EstadoFactura.ANULADA.equals(factura.getEstado());
        boolean emitidaEInformada = factura.isInformadaArca()
                && EstadoFactura.EMITIDA.equals(factura.getEstado())
                && conCae
                && conVencimientoCae;

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(salida);
             PdfDocument pdf = new PdfDocument(writer);
             Document documento = new Document(pdf, PageSize.A4)) {

            if (esAnulada) {
                Table avisoAnulada = new Table(1).useAllAvailableWidth().setMarginBottom(12);
                avisoAnulada.addCell(new Cell()
                        .setBackgroundColor(new DeviceRgb(254, 226, 226))
                        .setBorder(new SolidBorder(new DeviceRgb(153, 27, 27), 1.5f))
                        .setPadding(8)
                        .add(new Paragraph("COMPROBANTE ANULADO - NO VÁLIDO COMO FACTURA")
                                .setBold()
                                .setFontSize(10)
                                .setFontColor(new DeviceRgb(153, 27, 27))
                                .setTextAlignment(TextAlignment.CENTER)));
                documento.add(avisoAnulada);
            } else if (!emitidaEInformada) {
                Table aviso = new Table(1).useAllAvailableWidth().setMarginBottom(12);
                aviso.addCell(new Cell()
                        .setBackgroundColor(new DeviceRgb(255, 243, 205))
                        .setBorder(new SolidBorder(new DeviceRgb(180, 83, 9), 1.5f))
                        .setPadding(8)
                        .add(new Paragraph("COMPROBANTE NO VÁLIDO COMO FACTURA - PENDIENTE DE INFORMAR A ARCA")
                                .setBold()
                                .setFontSize(10)
                                .setFontColor(new DeviceRgb(146, 64, 14))
                                .setTextAlignment(TextAlignment.CENTER)));
                documento.add(aviso);
            }

            PuntoVenta pv = factura.getPuntoVenta();
            String pvTexto = pv != null
                    ? String.format("%04d - %s", pv.getNumero(), pv.getDescripcion() != null ? pv.getDescripcion() : "")
                    : "-";
            String nroFactura = factura.getNumero() != null ? String.format("%08d", factura.getNumero()) : "-";
            String fechaEmision = factura.getFechaEmision() != null ? factura.getFechaEmision().format(FORMATO_FECHA) : "-";

            Table encabezado = new Table(UnitValue.createPercentArray(new float[]{1.2f, 1f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(12);
            encabezado.addCell(new Cell()
                    .setPadding(8)
                    .add(new Paragraph(tipoComprobante).setBold().setFontSize(18))
                    .add(new Paragraph("CUIT Emisor: " + formatearCuit(cuitEmisor)).setFontSize(10))
                    .add(new Paragraph("Punto de Venta: " + pvTexto).setFontSize(10)));
            encabezado.addCell(new Cell()
                    .setPadding(8)
                    .add(new Paragraph("N° Factura: " + nroFactura).setBold().setFontSize(12))
                    .add(new Paragraph("Fecha de Emisión: " + fechaEmision).setFontSize(10))
                    .add(new Paragraph("Estado: " + factura.getEstado()).setFontSize(10))
                    .add(new Paragraph("Moneda: " + monedaVisualCompleta).setFontSize(10)));
            documento.add(encabezado);

            Cliente cliente = factura.getCliente();
            String clienteNombre = cliente != null ? cliente.getDenominacion() : "Consumidor Final";
            String condIvaNombre = factura.getCondicionIva() != null ? factura.getCondicionIva().getDenominacion() : "-";

            Table datosCliente = new Table(1).useAllAvailableWidth().setMarginBottom(12);
            Cell cellCliente = new Cell().setPadding(8)
                    .add(new Paragraph("Datos del Cliente").setBold().setFontSize(11))
                    .add(new Paragraph("Denominación: " + clienteNombre).setFontSize(10))
                    .add(new Paragraph("Condición frente al IVA: " + condIvaNombre).setFontSize(10));
            if (cliente != null) {
                if (cliente.getCuitCuil() != null && !cliente.getCuitCuil().isBlank()) {
                    cellCliente.add(new Paragraph("CUIT/CUIL: " + cliente.getCuitCuil()).setFontSize(10));
                }
                if (cliente.getDomicilio() != null) {
                    String calle = cliente.getDomicilio().getNombreCalle() != null ? cliente.getDomicilio().getNombreCalle() : "";
                    String altura = cliente.getDomicilio().getNumeroCalle() != null ? cliente.getDomicilio().getNumeroCalle() : "";
                    String domicilio = (calle + " " + altura).trim();
                    if (!domicilio.isEmpty()) {
                        cellCliente.add(new Paragraph("Domicilio: " + domicilio).setFontSize(10));
                    }
                }
                if (cliente.getContacto() != null && cliente.getContacto().getEmail() != null
                        && !cliente.getContacto().getEmail().isBlank()) {
                    cellCliente.add(new Paragraph("Email: " + cliente.getContacto().getEmail()).setFontSize(10));
                }
            }
            datosCliente.addCell(cellCliente);
            documento.add(datosCliente);

            Table tablaItems = new Table(UnitValue.createPercentArray(new float[]{1.1f, 2.4f, 0.8f, 1.1f, 0.8f, 1.1f, 1.0f, 1.2f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(12);
            String[] cols = {"Código", "Descripción", "Cant.", "P. Unit.", "% Bonif.", "Neto", "IVA", "Subtotal"};
            for (String col : cols) {
                tablaItems.addHeaderCell(new Cell()
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .add(new Paragraph(col).setBold().setFontSize(9)));
            }

            BigDecimal netoTotal = BigDecimal.ZERO;
            BigDecimal ivaTotal = BigDecimal.ZERO;

            for (FacturaVentaDetalle det : factura.getDetalles()) {
                String codigoArt = (det.getListaPrecioArticulo() != null && det.getListaPrecioArticulo().getArticulo() != null)
                        ? det.getListaPrecioArticulo().getArticulo().getCodigo()
                        : "-";
                String desc = det.getDescripcion() != null ? det.getDescripcion() : "-";

                netoTotal = netoTotal.add(BigDecimal.valueOf(det.getImporteNeto()));
                ivaTotal = ivaTotal.add(BigDecimal.valueOf(det.getImporteIva()));

                tablaItems.addCell(new Paragraph(codigoArt).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(desc).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%.2f", det.getCantidad())).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%s %.2f", simbolo, det.getPrecioUnitario())).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%.2f%%", det.getPorcentajeBonificacion())).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%s %.2f", simbolo, det.getImporteNeto())).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%s %.2f", simbolo, det.getImporteIva())).setFontSize(8.5f));
                tablaItems.addCell(new Paragraph(String.format(Locale.US, "%s %.2f", simbolo, det.getImporteSubtotal())).setFontSize(8.5f));
            }
            documento.add(tablaItems);

            Table totales = new Table(UnitValue.createPercentArray(new float[]{1.6f, 1f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(12);
            String obsText = (factura.getObservaciones() != null && !factura.getObservaciones().isBlank())
                    ? factura.getObservaciones()
                    : "Sin observaciones.";
            totales.addCell(new Cell()
                    .setPadding(8)
                    .add(new Paragraph("Observaciones").setBold().setFontSize(10))
                    .add(new Paragraph(obsText).setFontSize(9.5f)));
            totales.addCell(new Cell()
                    .setPadding(8)
                    .add(new Paragraph(String.format(Locale.US, "Neto Total: %s %.2f",
                            simbolo, netoTotal.setScale(2, RoundingMode.HALF_UP).doubleValue())).setFontSize(10))
                    .add(new Paragraph(String.format(Locale.US, "IVA Total: %s %.2f",
                            simbolo, ivaTotal.setScale(2, RoundingMode.HALF_UP).doubleValue())).setFontSize(10))
                    .add(new Paragraph(String.format(Locale.US, "Importe Total: %s %.2f",
                            simbolo, factura.getImporteTotal())).setBold().setFontSize(11))
                    .add(new Paragraph(String.format(Locale.US, "Importe Cobrado: %s %.2f",
                            simbolo, factura.getImporteCobrado())).setFontSize(10))
                    .add(new Paragraph(String.format(Locale.US, "Saldo: %s %.2f",
                            simbolo, factura.getImporteSaldo())).setFontSize(10)));
            documento.add(totales);

            if (emitidaEInformada) {
                String vencimientoTexto = formatearFecha(factura.getCaeFechaVencimiento());
                String qrUrl = construirUrlQrArca(factura, tipoCmpAfip, codigoMonedaAfip);
                BarcodeQRCode qrCode = new BarcodeQRCode(qrUrl);
                Image qrImage = new Image(qrCode.createFormXObject(pdf))
                        .setWidth(110)
                        .setHeight(110)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                Table datosCae = new Table(1).useAllAvailableWidth().setMarginBottom(14);
                datosCae.addCell(new Cell()
                        .setPadding(8)
                        .add(new Paragraph("Comprobante Autorizado por ARCA / AFIP").setBold().setFontSize(11))
                        .add(new Paragraph("CAE: " + factura.getCae()).setFontSize(10))
                        .add(new Paragraph("Vencimiento CAE: " + vencimientoTexto).setFontSize(10))
                        .add(new Paragraph("Resultado AFIP: "
                                + (factura.getResultadoAfip() != null ? factura.getResultadoAfip() : "-")).setFontSize(10)));
                documento.add(datosCae);

                Div bloqueQr = new Div()
                        .setKeepTogether(true)
                        .setMarginTop(6)
                        .setPaddingTop(8);
                bloqueQr.add(qrImage);
                bloqueQr.add(new Paragraph("Código QR de validación fiscal ARCA / AFIP")
                        .setFontSize(8.5f)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(4));
                documento.add(bloqueQr);
            } else if (esAnulada) {
                Table anulada = new Table(1).useAllAvailableWidth();
                Cell cellAnulada = new Cell()
                        .setBackgroundColor(new DeviceRgb(254, 226, 226))
                        .setBorder(new SolidBorder(new DeviceRgb(153, 27, 27), 1.5f))
                        .setPadding(8)
                        .add(new Paragraph("COMPROBANTE ANULADO - SIN VALIDACION QR")
                                .setBold()
                                .setFontSize(11)
                                .setFontColor(new DeviceRgb(153, 27, 27))
                                .setTextAlignment(TextAlignment.CENTER));
                if (conCae) {
                    cellAnulada.add(new Paragraph("CAE original: " + factura.getCae()
                            + (conVencimientoCae ? " | Vto.: " + formatearFecha(factura.getCaeFechaVencimiento()) : ""))
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER));
                }
                anulada.addCell(cellAnulada);
                documento.add(anulada);
            } else {
                Table pendiente = new Table(1).useAllAvailableWidth();
                pendiente.addCell(new Cell()
                        .setBackgroundColor(new DeviceRgb(255, 243, 205))
                        .setBorder(new SolidBorder(new DeviceRgb(180, 83, 9), 1.5f))
                        .setPadding(8)
                        .add(new Paragraph("COMPROBANTE PENDIENTE DE AUTORIZACION - SIN CAE ASIGNADO")
                                .setBold()
                                .setFontSize(11)
                                .setFontColor(new DeviceRgb(146, 64, 14))
                                .setTextAlignment(TextAlignment.CENTER))
                        .add(new Paragraph("El comprobante no tiene CAE ni codigo QR de ARCA porque aun "
                                + "no fue autorizado por el organismo.")
                                .setFontSize(9)
                                .setTextAlignment(TextAlignment.CENTER)));
                documento.add(pendiente);
            }
        }
        return salida.toByteArray();
    }

    public String construirUrlQrArca(FacturaVenta factura, int tipoCmp, String codigoMonedaAfip) {
        String fecha = factura.getFechaEmision() != null
                ? factura.getFechaEmision().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : "";
        long cuit = cuitEmisor != null ? cuitEmisor : 0L;
        int ptoVta = factura.getPuntoVenta() != null ? factura.getPuntoVenta().getNumero() : 0;
        long nroCmp = factura.getNumero() != null ? factura.getNumero() : 0L;
        BigDecimal importe = BigDecimal.valueOf(factura.getImporteTotal()).setScale(2, RoundingMode.HALF_UP);

        long nroDocRec = 0L;
        if (factura.getCliente() != null && factura.getCliente().getCuitCuil() != null) {
            String digitos = factura.getCliente().getCuitCuil().replaceAll("\\D", "");
            if (!digitos.isEmpty()) {
                nroDocRec = Long.parseLong(digitos);
            }
        }
        int tipoDocRec = nroDocRec > 0L ? 80 : 99;

        String caeStr = factura.getCae() != null ? factura.getCae().trim() : "0";
        String codAutJson = caeStr.matches("\\d+") ? caeStr : "\"" + caeStr + "\"";

        String json = String.format(Locale.US,
                "{\"ver\":1,\"fecha\":\"%s\",\"cuit\":%d,\"ptoVta\":%d,\"tipoCmp\":%d,\"nroCmp\":%d,"
                        + "\"importe\":%s,\"moneda\":\"%s\",\"ctz\":1,\"tipoDocRec\":%d,\"nroDocRec\":%d,"
                        + "\"tipoCodAut\":\"E\",\"codAut\":%s}",
                fecha, cuit, ptoVta, tipoCmp, nroCmp,
                importe.toPlainString(), codigoMonedaAfip, tipoDocRec, nroDocRec, codAutJson);

        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return URL_QR_AFIP + base64;
    }

    private String formatearCuit(Long cuit) {
        if (cuit == null) {
            return "-";
        }
        String s = String.valueOf(cuit);
        if (s.length() == 11) {
            return s.substring(0, 2) + "-" + s.substring(2, 10) + "-" + s.substring(10);
        }
        return s;
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "-";
        }
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(FORMATO_FECHA);
    }

    private String codigoMonedaVisual(String codigoMoneda) {
        if ("DOL".equalsIgnoreCase(codigoMoneda) || "USD".equalsIgnoreCase(codigoMoneda)) {
            return "USD";
        }
        if ("PES".equalsIgnoreCase(codigoMoneda) || "ARS".equalsIgnoreCase(codigoMoneda)) {
            return "ARS";
        }
        return codigoMoneda != null ? codigoMoneda : "ARS";
    }

    private String monedaVisualConSimbolo(String codigoMoneda) {
        String visual = codigoMonedaVisual(codigoMoneda);
        return visual + " (" + simboloMoneda(codigoMoneda) + ")";
    }

    public static String escaparTsv(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String simboloMoneda(String codigoMoneda) {
        if ("DOL".equalsIgnoreCase(codigoMoneda) || "USD".equalsIgnoreCase(codigoMoneda)) {
            return "US$";
        }
        if ("PES".equalsIgnoreCase(codigoMoneda) || "ARS".equalsIgnoreCase(codigoMoneda)) {
            return "$";
        }
        return codigoMoneda;
    }
}
