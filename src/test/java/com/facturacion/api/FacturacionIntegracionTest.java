package com.facturacion.api;

import com.facturacion.api.config.AuditoriaContext;
import com.facturacion.api.dto.FacturaDetalleRequestDTO;
import com.facturacion.api.dto.FacturaFiltroDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.FacturaRequestDTO;
import com.facturacion.api.exception.ConflictoNegocioException;
import com.facturacion.api.exception.RecursoNoEncontradoException;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.facturacion.api.modelo.Articulo;
import com.facturacion.api.modelo.Cliente;
import com.facturacion.api.modelo.CondicionIva;
import com.facturacion.api.modelo.Contacto;
import com.facturacion.api.modelo.Domicilio;
import com.facturacion.api.modelo.EstadoFactura;
import com.facturacion.api.modelo.FacturaVenta;
import com.facturacion.api.modelo.ListaPrecio;
import com.facturacion.api.modelo.ListaPrecioArticulo;
import com.facturacion.api.modelo.PuntoVenta;
import com.facturacion.api.modelo.TipoComprobante;
import com.facturacion.api.modelo.TipoMoneda;
import com.facturacion.api.modelo.Usuario;
import com.facturacion.api.reportes.ReporteService;
import com.facturacion.api.repository.CondicionIvaRepository;
import com.facturacion.api.repository.FacturaVentaRepository;
import com.facturacion.api.repository.ListaPrecioArticuloRepository;
import com.facturacion.api.repository.PuntoVentaRepository;
import com.facturacion.api.repository.TipoMonedaRepository;
import com.facturacion.api.repository.UsuarioRepository;
import com.facturacion.api.service.CatalogoService;
import com.facturacion.api.service.FacturaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FacturacionIntegracionTest {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private FacturaVentaRepository facturaRepository;

    @Autowired
    private PuntoVentaRepository puntoVentaRepository;

    @Autowired
    private CondicionIvaRepository condicionIvaRepository;

    @Autowired
    private TipoMonedaRepository tipoMonedaRepository;

    @Autowired
    private ListaPrecioArticuloRepository listaPrecioArticuloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager em;

    private Usuario usuarioJperez;
    private Usuario usuarioSistema;
    private PuntoVenta puntoVenta;
    private CondicionIva condicionIva;
    private TipoMoneda monedaPes;
    private TipoMoneda monedaDol;
    private Articulo articuloBase;
    private ListaPrecio listaGeneral;
    private ListaPrecioArticulo precioArticulo;

    @BeforeEach
    void prepararDatosBase() {
        usuarioJperez = new Usuario();
        usuarioJperez.setUsuario("jperez");
        usuarioJperez.setClave("hash");
        usuarioJperez.setNombre("Juan");
        usuarioJperez.setApellido("Perez");
        usuarioRepository.save(usuarioJperez);

        usuarioSistema = new Usuario();
        usuarioSistema.setUsuario("sistema");
        usuarioSistema.setClave("hash");
        usuarioSistema.setNombre("Sistema");
        usuarioSistema.setApellido("Auditoria");
        usuarioRepository.save(usuarioSistema);

        Date ahora = new Date();

        puntoVenta = new PuntoVenta(1, "Sucursal Central", "ELECTRONICA", "San Martin 100");
        puntoVenta.setFechaAlta(ahora);
        puntoVenta.setFechaModificacion(ahora);
        puntoVenta.setUsuarioCarga(usuarioJperez);
        puntoVenta.setUsuarioModificacion(usuarioJperez);
        puntoVentaRepository.save(puntoVenta);

        condicionIva = new CondicionIva(5, "Consumidor Final");
        condicionIva.setFechaAlta(ahora);
        condicionIva.setFechaModificacion(ahora);
        condicionIva.setUsuarioCarga(usuarioJperez);
        condicionIva.setUsuarioModificacion(usuarioJperez);
        condicionIvaRepository.save(condicionIva);

        monedaPes = new TipoMoneda("PES", "Peso Argentino", "$");
        monedaPes.setFechaAlta(ahora);
        monedaPes.setFechaModificacion(ahora);
        monedaPes.setUsuarioCarga(usuarioJperez);
        monedaPes.setUsuarioModificacion(usuarioJperez);
        tipoMonedaRepository.save(monedaPes);

        monedaDol = new TipoMoneda("DOL", "Dolar Estadounidense", "US$");
        monedaDol.setFechaAlta(ahora);
        monedaDol.setFechaModificacion(ahora);
        monedaDol.setUsuarioCarga(usuarioJperez);
        monedaDol.setUsuarioModificacion(usuarioJperez);
        tipoMonedaRepository.save(monedaDol);

        articuloBase = new Articulo();
        articuloBase.setCodigo("ART-100");
        articuloBase.setDenominacion("Teclado Mecanico");
        articuloBase.setFechaAlta(ahora);
        articuloBase.setFechaModificacion(ahora);
        articuloBase.setUsuarioCarga(usuarioJperez);
        articuloBase.setUsuarioModificacion(usuarioJperez);
        em.persist(articuloBase);

        listaGeneral = new ListaPrecio();
        listaGeneral.setCodigo("LP-01");
        listaGeneral.setDenominacion("Lista General");
        listaGeneral.setFechaAlta(ahora);
        listaGeneral.setFechaModificacion(ahora);
        listaGeneral.setUsuarioCarga(usuarioJperez);
        listaGeneral.setUsuarioModificacion(usuarioJperez);
        em.persist(listaGeneral);

        precioArticulo = new ListaPrecioArticulo();
        precioArticulo.setArticulo(articuloBase);
        precioArticulo.setListaPrecio(listaGeneral);
        precioArticulo.setPrecioVenta(1000.0);
        precioArticulo.setFechaAlta(ahora);
        precioArticulo.setFechaModificacion(ahora);
        precioArticulo.setUsuarioCarga(usuarioJperez);
        precioArticulo.setUsuarioModificacion(usuarioJperez);
        listaPrecioArticuloRepository.save(precioArticulo);

        em.flush();
    }

    @AfterEach
    void limpiarContexto() {
        AuditoriaContext.limpiar();
    }

    @Test
    void debeCrearFacturaConDetallesYCalcularImportesYMoneda() {
        AuditoriaContext.setUsuario(usuarioSistema);

        FacturaRequestDTO req = crearSolicitud(monedaPes.getId(), "EMITIDA", 2.0, 10.0);
        FacturaReporteDTO reporte = facturaService.crear(req);

        // Neto = 2 * 1000 * 0.90 = 1800.00 | IVA 21% = 378.00 | Total = 2178.00
        assertNotNull(reporte.getId());
        assertEquals(2178.00, reporte.getImporteTotal(), 0.001);
        assertEquals("Consumidor Final", reporte.getClienteDenominacion());
        assertEquals("PES", reporte.getCodigoMoneda());

        FacturaVenta guardada = facturaRepository.findById(reporte.getId()).orElseThrow();
        assertEquals("sistema", guardada.getUsuarioCarga().getUsuario());
        assertEquals(1, guardada.getDetalles().size());
        assertEquals(1800.00, guardada.getDetalles().get(0).getImporteNeto(), 0.001);
        assertEquals(378.00, guardada.getDetalles().get(0).getImporteIva(), 0.001);
    }

    @Test
    void debeSepararAtributosDeCaeAlInformarArca() {
        FacturaRequestDTO req = crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0);
        FacturaReporteDTO creada = facturaService.crear(req);

        FacturaReporteDTO informada = facturaService.informarArca(creada.getId());
        assertTrue(informada.isInformadaArca());

        FacturaVenta entidad = facturaRepository.findById(creada.getId()).orElseThrow();
        assertEquals("74431234567890", entidad.getCae());
        assertEquals(14, entidad.getCae().length());
        assertNotNull(entidad.getCaeFechaVencimiento());
        assertEquals("FACTURA APROBADA", entidad.getResultadoAfip());
    }

    @Test
    void debePermitirEliminarNoInformadasEImpedirEliminarInformadas() {
        // 1. Factura EMITIDA no informada se puede eliminar
        FacturaReporteDTO emitidaNoInformada = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.eliminar(emitidaNoInformada.getId());
        assertTrue(facturaRepository.findById(emitidaNoInformada.getId()).isEmpty());

        // 2. Factura PENDIENTE / BORRADOR no informada también se puede eliminar físicamente junto a sus detalles
        FacturaReporteDTO pendiente = facturaService.crear(crearSolicitud(monedaPes.getId(), "PENDIENTE", 1.0, 0.0));
        facturaService.eliminar(pendiente.getId());
        assertTrue(facturaRepository.findById(pendiente.getId()).isEmpty());

        // 3. Factura informada a ARCA NO se puede eliminar
        FacturaReporteDTO emitida = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.informarArca(emitida.getId());
        assertThrows(ConflictoNegocioException.class, () -> facturaService.eliminar(emitida.getId()));

        // El catálogo sigue intacto
        assertTrue(listaPrecioArticuloRepository.findById(precioArticulo.getId()).isPresent());
    }

    @Test
    void debePermitirAnularFacturaInformadaPorCambiarEstadoYActualizarSoloAnulacion() {
        // 1. Anular vía cambiarEstado sobre factura informada
        FacturaReporteDTO f1 = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.informarArca(f1.getId());

        FacturaReporteDTO anulada1 = facturaService.cambiarEstado(f1.getId());
        assertEquals("ANULADA", anulada1.getEstado());
        FacturaVenta entidad1 = facturaRepository.findById(f1.getId()).orElseThrow();
        assertNotNull(entidad1.getFechaAnulacion());
        assertNotNull(entidad1.getFechaBaja());
        assertNotNull(entidad1.getUsuarioBaja());

        // 2. En actualizar sobre factura informada, rechaza cambios de datos pero permite solo estado=ANULADA + observaciones
        FacturaReporteDTO f2 = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.informarArca(f2.getId());

        FacturaRequestDTO intentoEdicion = crearSolicitud(monedaPes.getId(), "ANULADA", 2.0, 0.0);
        assertThrows(ConflictoNegocioException.class, () -> facturaService.actualizar(f2.getId(), intentoEdicion));

        FacturaRequestDTO soloAnulacion = new FacturaRequestDTO();
        soloAnulacion.setEstado("ANULADA");
        soloAnulacion.setObservaciones("Anulada por error de carga");
        FacturaReporteDTO anulada2 = facturaService.actualizar(f2.getId(), soloAnulacion);
        assertEquals("ANULADA", anulada2.getEstado());
        FacturaVenta entidad2 = facturaRepository.findById(f2.getId()).orElseThrow();
        assertEquals("Anulada por error de carga", entidad2.getObservaciones());
        assertNotNull(entidad2.getFechaAnulacion());
    }

    @Test
    void debeGenerarReportesPdfYTsvConMonedaVisual() throws Exception {
        facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.crear(crearSolicitud(monedaDol.getId(), "EMITIDA", 2.0, 0.0));

        byte[] pdf = reporteService.generarPdf(FacturaFiltroDTO.vacio());
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);

        byte[] tsvBytes = reporteService.generarExcelComoTsv(FacturaFiltroDTO.vacio());
        String tsv = new String(tsvBytes, StandardCharsets.UTF_8);
        assertTrue(tsv.contains("Moneda"));
        assertTrue(tsv.contains("ARS"));
        assertTrue(tsv.contains("USD"));
        assertFalse(tsv.contains("\tPES"));
        assertFalse(tsv.contains("\tDOL"));
    }

    @Test
    void debeGenerarPdfIndividualConLeyendaNoValidoYConQrArcaCuandoInformada() throws Exception {
        FacturaReporteDTO noInformada = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));

        byte[] pdfNoInformada = reporteService.generarPdfFactura(noInformada.getId());
        assertNotNull(pdfNoInformada);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfNoInformada)))) {
            String texto = PdfTextExtractor.getTextFromPage(doc.getFirstPage());
            assertTrue(texto.contains("COMPROBANTE NO VÁLIDO COMO FACTURA - PENDIENTE DE INFORMAR A ARCA"));
            assertTrue(texto.contains("20-47196790-5"));
            assertTrue(texto.contains("ARS ($)"));
            assertTrue(texto.contains("ART-100"));
        }

        facturaService.informarArca(noInformada.getId());
        byte[] pdfInformada = reporteService.generarPdfFactura(noInformada.getId());
        assertNotNull(pdfInformada);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfInformada)))) {
            String texto = PdfTextExtractor.getTextFromPage(doc.getFirstPage());
            assertFalse(texto.contains("PENDIENTE DE INFORMAR A ARCA"));
            assertTrue(texto.contains("74431234567890"));
            assertTrue(texto.contains("FACTURA APROBADA"));
        }

        assertThrows(RecursoNoEncontradoException.class, () -> reporteService.generarPdfFactura(999999L));
    }

    @Test
    void debeFiltrarArticulosPorListaDePrecios() {
        var listas = catalogoService.listasPrecios();
        assertFalse(listas.isEmpty());
        Long listaId = listas.get(0).id();

        var paginaFiltrada = catalogoService.listasPrecioArticulo(listaId, "", 0, 10);
        assertEquals(1, paginaFiltrada.contenido().size());
        assertEquals("Lista General", paginaFiltrada.contenido().get(0).listaPrecio());

        var paginaInexistente = catalogoService.listasPrecioArticulo(999999L, "", 0, 10);
        assertTrue(paginaInexistente.contenido().isEmpty());
    }

    @Test
    void debeExponerListaPrecioPorDefectoEnClientes() {
        ListaPrecio mayorista = crearListaPrecio("LP-MAY", "Lista Mayorista");
        ListaPrecio minorista = crearListaPrecio("LP-MIN", "Lista Minorista");

        Cliente comercial = crearClienteConLista("30-71000001-3", "Comercial Sur S.R.L.", "comercial@sur.com",
                condicionIva, mayorista);
        Cliente particular = crearClienteConLista("20-30111222-3", "Perez Ana", "ana@mail.com",
                condicionIva, minorista);
        em.flush();

        var paginaComercial = catalogoService.clientes("Comercial Sur", 0, 10);
        assertEquals(1, paginaComercial.contenido().size());
        assertEquals(comercial.getId(), paginaComercial.contenido().get(0).id());
        assertEquals(mayorista.getId(), paginaComercial.contenido().get(0).listaPrecioId());
        assertEquals("Lista Mayorista", paginaComercial.contenido().get(0).listaPrecioDenominacion());

        var paginaParticular = catalogoService.clientes("Perez Ana", 0, 10);
        assertEquals(1, paginaParticular.contenido().size());
        assertEquals(particular.getId(), paginaParticular.contenido().get(0).id());
        assertEquals(minorista.getId(), paginaParticular.contenido().get(0).listaPrecioId());
        assertEquals("Lista Minorista", paginaParticular.contenido().get(0).listaPrecioDenominacion());
    }

    @Test
    void debeResolverPreciosPorListaYCodigosYOmitirFaltantes() {
        ListaPrecio mayorista = crearListaPrecio("LP-MAY", "Lista Mayorista");
        ListaPrecioArticulo precioMayoristaArt100 = crearPrecioParaLista(mayorista, articuloBase, 800.0);

        Articulo art200 = new Articulo();
        art200.setCodigo("ART-200");
        art200.setDenominacion("Mouse Gamer");
        art200.setFechaAlta(new Date());
        art200.setFechaModificacion(new Date());
        art200.setUsuarioCarga(usuarioJperez);
        art200.setUsuarioModificacion(usuarioJperez);
        em.persist(art200);
        crearPrecioParaLista(listaGeneral, art200, 500.0);
        em.flush();

        var encontrados = catalogoService.preciosPorListaYCodigos(mayorista.getId(), List.of("ART-100", "ART-200"));
        assertEquals(1, encontrados.size());
        assertEquals("ART-100", encontrados.get(0).articuloCodigo());
        assertEquals(precioMayoristaArt100.getId(), encontrados.get(0).id());
        assertEquals(800.0, encontrados.get(0).precioVenta(), 0.001);
        assertEquals("Lista Mayorista", encontrados.get(0).listaPrecio());

        assertTrue(catalogoService.preciosPorListaYCodigos(mayorista.getId(), List.of()).isEmpty());
    }

    @Test
    void debeRechazarMezclaDeListasDePreciosYDevolverListaEnEdicion() {
        ListaPrecio mayorista = crearListaPrecio("LP-MAY", "Lista Mayorista");
        ListaPrecioArticulo precioMayorista = crearPrecioParaLista(mayorista, articuloBase, 750.0);
        em.flush();

        FacturaRequestDTO mezclada = new FacturaRequestDTO();
        mezclada.setSinCliente(true);
        mezclada.setPuntoVentaId(puntoVenta.getId());
        mezclada.setCondicionIvaId(condicionIva.getId());
        mezclada.setTipoMonedaId(monedaPes.getId());
        mezclada.setFechaEmision(LocalDate.now());
        mezclada.setDetalles(List.of(
                new FacturaDetalleRequestDTO(precioArticulo.getId(), 1.0, 0.0, null),
                new FacturaDetalleRequestDTO(precioMayorista.getId(), 1.0, 0.0, null)
        ));
        assertThrows(IllegalArgumentException.class, () -> facturaService.crear(mezclada));

        FacturaReporteDTO creada = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        var edicion = facturaService.obtenerParaEdicion(creada.getId());
        assertEquals(listaGeneral.getId(), edicion.listaPrecioId());
    }

    @Test
    void debeGenerarQrOficialSoloParaEmitidaEInformadaConCaeYVencimiento() throws Exception {
        // 1. Borrador / no informada -> sin QR
        FacturaReporteDTO borrador = facturaService.crear(crearSolicitud(monedaPes.getId(), "BORRADOR", 1.0, 0.0));
        assertEquals(0, contarXObjectsPdf(reporteService.generarPdfFactura(borrador.getId())));

        // 2. Informada pero sin fecha de vencimiento de CAE -> sin QR
        FacturaReporteDTO emitida = facturaService.crear(crearSolicitud(monedaPes.getId(), "EMITIDA", 1.0, 0.0));
        facturaService.informarArca(emitida.getId());
        FacturaVenta entidad = facturaRepository.findById(emitida.getId()).orElseThrow();
        Date vencimientoOriginal = entidad.getCaeFechaVencimiento();
        entidad.setCaeFechaVencimiento(null);
        em.flush();
        assertEquals(0, contarXObjectsPdf(reporteService.generarPdfFactura(emitida.getId())));

        // 3. Informada, EMITIDA, con CAE y vencimiento -> genera QR debajo de los datos del CAE y URL oficial AFIP
        entidad.setCaeFechaVencimiento(vencimientoOriginal);
        em.flush();

        String qrUrl = reporteService.construirUrlQrArca(
                entidad, TipoComprobante.codigoAfip(entidad.getTipoComprobante()), "PES");
        assertTrue(qrUrl.startsWith("https://www.afip.gob.ar/fe/qr/?p="));
        String jsonQr = new String(
                Base64.getDecoder().decode(qrUrl.substring("https://www.afip.gob.ar/fe/qr/?p=".length())),
                StandardCharsets.UTF_8);
        assertTrue(jsonQr.contains("\"ver\":1"));
        assertTrue(jsonQr.contains("\"fecha\":\"" + entidad.getFechaEmision() + "\""));
        assertTrue(jsonQr.contains("\"cuit\":20471967905"));
        assertTrue(jsonQr.contains("\"ptoVta\":1"));
        assertTrue(jsonQr.contains("\"tipoCmp\":6"));
        assertTrue(jsonQr.contains("\"nroCmp\":" + entidad.getNumero()));
        assertTrue(jsonQr.contains("\"importe\":1210.00"));
        assertTrue(jsonQr.contains("\"moneda\":\"PES\""));
        assertTrue(jsonQr.contains("\"ctz\":1"));
        assertTrue(jsonQr.contains("\"tipoDocRec\":99"));
        assertTrue(jsonQr.contains("\"nroDocRec\":0"));
        assertTrue(jsonQr.contains("\"tipoCodAut\":\"E\""));
        assertTrue(jsonQr.contains("\"codAut\":74431234567890"));

        // Triangulacion: cliente con CUIT (tipoDocRec:80) y Factura A (tipoCmp:1)
        Cliente empresa = crearClienteConLista("30-71000001-3", "Empresa Prueba S.A.",
                "empresa@prueba.com", condicionIva, listaGeneral);
        entidad.setCliente(empresa);
        String qrUrlConCuit = reporteService.construirUrlQrArca(entidad, 1, "PES");
        String jsonQrConCuit = new String(
                Base64.getDecoder().decode(qrUrlConCuit.substring("https://www.afip.gob.ar/fe/qr/?p=".length())),
                StandardCharsets.UTF_8);
        assertTrue(jsonQrConCuit.contains("\"tipoCmp\":1"));
        assertTrue(jsonQrConCuit.contains("\"tipoDocRec\":80"));
        assertTrue(jsonQrConCuit.contains("\"nroDocRec\":30710000013"));
        entidad.setCliente(null);

        byte[] pdfConQr = reporteService.generarPdfFactura(emitida.getId());
        assertTrue(contarXObjectsPdf(pdfConQr) > 0);
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfConQr)))) {
            String texto = PdfTextExtractor.getTextFromPage(doc.getFirstPage());
            int idxCae = texto.indexOf("CAE: 74431234567890");
            int idxQrLabel = texto.indexOf("Código QR de validación fiscal ARCA / AFIP");
            assertTrue(idxCae >= 0 && idxQrLabel > idxCae,
                    "El bloque QR debe renderizarse debajo de los datos del CAE");
        }

        // 4. Anulada (incluso habiendo sido informada) -> sin QR
        entidad.setEstado(EstadoFactura.ANULADA);
        em.flush();
        byte[] pdfAnulada = reporteService.generarPdfFactura(emitida.getId());
        assertEquals(0, contarXObjectsPdf(pdfAnulada));
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfAnulada)))) {
            String texto = PdfTextExtractor.getTextFromPage(doc.getFirstPage());
            assertTrue(texto.contains("COMPROBANTE ANULADO"));
        }
    }

    private int contarXObjectsPdf(byte[] pdfBytes) throws Exception {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            PdfDictionary resources = doc.getFirstPage().getPdfObject().getAsDictionary(PdfName.Resources);
            if (resources == null) {
                return 0;
            }
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
            return xObjects != null ? xObjects.size() : 0;
        }
    }

    private ListaPrecio crearListaPrecio(String codigo, String denominacion) {
        Date ahora = new Date();
        ListaPrecio lista = new ListaPrecio();
        lista.setCodigo(codigo);
        lista.setDenominacion(denominacion);
        lista.setFechaAlta(ahora);
        lista.setFechaModificacion(ahora);
        lista.setUsuarioCarga(usuarioJperez);
        lista.setUsuarioModificacion(usuarioJperez);
        em.persist(lista);
        return lista;
    }

    private ListaPrecioArticulo crearPrecioParaLista(ListaPrecio lista, Articulo articulo, double precioVenta) {
        Date ahora = new Date();
        ListaPrecioArticulo lpa = new ListaPrecioArticulo();
        lpa.setListaPrecio(lista);
        lpa.setArticulo(articulo);
        lpa.setPrecioVenta(precioVenta);
        lpa.setFechaAlta(ahora);
        lpa.setFechaModificacion(ahora);
        lpa.setUsuarioCarga(usuarioJperez);
        lpa.setUsuarioModificacion(usuarioJperez);
        return listaPrecioArticuloRepository.save(lpa);
    }

    private Cliente crearClienteConLista(String cuit, String denominacion, String email,
                                         CondicionIva condIva, ListaPrecio listaPrecio) {
        Contacto contacto = new Contacto();
        contacto.setEmail(email);
        em.persist(contacto);

        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle("San Martin");
        domicilio.setNumeroCalle("100");
        em.persist(domicilio);

        Date ahora = new Date();
        Cliente c = new Cliente();
        c.setCuitCuil(cuit);
        c.setDenominacion(denominacion);
        c.setContacto(contacto);
        c.setDomicilio(domicilio);
        c.setCondicionIva(condIva);
        c.setListaPrecio(listaPrecio);
        c.setFechaAlta(ahora);
        c.setFechaModificacion(ahora);
        c.setUsuarioCarga(usuarioJperez);
        c.setUsuarioModificacion(usuarioJperez);
        em.persist(c);
        return c;
    }

    private FacturaRequestDTO crearSolicitud(Long monedaId, String estado, double cantidad, double bonificacion) {
        FacturaDetalleRequestDTO det = new FacturaDetalleRequestDTO(
                precioArticulo.getId(), cantidad, bonificacion, "Item de prueba");
        FacturaRequestDTO req = new FacturaRequestDTO();
        req.setSinCliente(true);
        req.setPuntoVentaId(puntoVenta.getId());
        req.setCondicionIvaId(condicionIva.getId());
        req.setTipoMonedaId(monedaId);
        req.setFechaEmision(LocalDate.now());
        req.setEstado(estado);
        req.setImporteCobrado(0.0);
        req.setObservaciones("Observacion test");
        req.setDetalles(List.of(det));
        return req;
    }
}
