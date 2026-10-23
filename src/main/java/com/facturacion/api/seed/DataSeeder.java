package com.facturacion.api.seed;

import com.facturacion.api.dto.FacturaDetalleRequestDTO;
import com.facturacion.api.dto.FacturaReporteDTO;
import com.facturacion.api.dto.FacturaRequestDTO;
import com.facturacion.api.modelo.Articulo;
import com.facturacion.api.modelo.AuditoriaApp;
import com.facturacion.api.modelo.Cliente;
import com.facturacion.api.modelo.CondicionIva;
import com.facturacion.api.modelo.Contacto;
import com.facturacion.api.modelo.Domicilio;
import com.facturacion.api.modelo.ListaPrecio;
import com.facturacion.api.modelo.ListaPrecioArticulo;
import com.facturacion.api.modelo.Marca;
import com.facturacion.api.modelo.PuntoVenta;
import com.facturacion.api.modelo.Rubro;
import com.facturacion.api.modelo.TipoMoneda;
import com.facturacion.api.modelo.Usuario;
import com.facturacion.api.service.FacturaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final FacturaService facturaService;

    public DataSeeder(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long existentes = em.createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class).getSingleResult();
        if (existentes > 0) {
            System.out.println("DataSeeder: ya hay " + existentes + " facturas, no se vuelve a sembrar.");
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario("jperez");
        usuario.setClave("hash_no_real");
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        em.persist(usuario);

        PuntoVenta central = puntoVenta(usuario, 1, "Sucursal Central");
        PuntoVenta norte = puntoVenta(usuario, 2, "Sucursal Norte");
        CondicionIva responsable = condicionIva(usuario, 1, "IVA Responsable Inscripto");
        condicionIva(usuario, 5, "Consumidor Final");
        TipoMoneda peso = tipoMoneda(usuario, "PES", "Peso Argentino", "$");
        tipoMoneda(usuario, "DOL", "Dolar Estadounidense", "US$");

        Cliente tech = cliente(usuario, "20-12345678-9", "Empresa Tech S.A.",
                "empresa.tech@mail.com", "San Martin", "500");
        Cliente andina = cliente(usuario, "30-71234567-8", "Comercial Andina S.R.L.",
                "contacto@andina.com", "Belgrano", "120");

        Rubro rubro = new Rubro();
        rubro.setCodigo(10);
        rubro.setDenominacion("Tecnologia");
        auditar(rubro, usuario);

        Marca marca = new Marca();
        marca.setCodigo(55);
        marca.setDenominacion("Marca Generica");
        auditar(marca, usuario);

        ListaPrecio lista = new ListaPrecio();
        lista.setCodigo("LP-GEN");
        lista.setDenominacion("Lista General");
        auditar(lista, usuario);

        Articulo notebook = articulo(usuario, rubro, marca, lista, "ART-001", "Notebook 14 pulgadas", 500000.0);
        Articulo mouse = articulo(usuario, rubro, marca, lista, "ART-002", "Mouse inalambrico", 18000.0);
        Articulo teclado = articulo(usuario, rubro, marca, lista, "ART-003", "Teclado mecanico", 55000.0);

        facturaService.crear(solicitud(tech.getId(), central.getId(), responsable.getId(), peso.getId(),
                LocalDate.of(2026, 3, 15),
                detalle(notebook.getId(), 1.0, null),
                detalle(mouse.getId(), 2.0, null)));

        facturaService.crear(solicitud(null, central.getId(), responsable.getId(), peso.getId(),
                LocalDate.of(2026, 3, 20),
                detalle(mouse.getId(), 1.0, null)));

        FacturaReporteDTO aAnular = facturaService.crear(solicitud(andina.getId(), norte.getId(),
                responsable.getId(), peso.getId(),
                LocalDate.of(2026, 4, 2),
                detalle(teclado.getId(), 1.0, 10.0)));

        FacturaRequestDTO anulacion = new FacturaRequestDTO();
        anulacion.setEstado("ANULADA");
        anulacion.setObservaciones("Anulada por error de carga");
        facturaService.actualizar(aAnular.getId(), anulacion);

        System.out.println("DataSeeder: datos de prueba cargados (3 facturas, la tercera anulada).");
    }

    private <T extends AuditoriaApp> T auditar(T entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificacion(usuario);
        em.persist(entidad);
        return entidad;
    }

    private PuntoVenta puntoVenta(Usuario usuario, int numero, String descripcion) {
        PuntoVenta puntoVenta = new PuntoVenta();
        puntoVenta.setNumero(numero);
        puntoVenta.setDescripcion(descripcion);
        puntoVenta.setTipoEmision("Electronica");
        return auditar(puntoVenta, usuario);
    }

    private CondicionIva condicionIva(Usuario usuario, int codigoAfip, String denominacion) {
        CondicionIva condicion = new CondicionIva();
        condicion.setCodigoAfip(codigoAfip);
        condicion.setDenominacion(denominacion);
        return auditar(condicion, usuario);
    }

    private TipoMoneda tipoMoneda(Usuario usuario, String codigoAfip, String denominacion, String simbolo) {
        TipoMoneda moneda = new TipoMoneda();
        moneda.setCodigoAfip(codigoAfip);
        moneda.setDenominacion(denominacion);
        moneda.setSimbolo(simbolo);
        return auditar(moneda, usuario);
    }

    private Cliente cliente(Usuario usuario, String cuit, String denominacion, String email,
                            String calle, String numeroCalle) {
        Contacto contacto = new Contacto();
        contacto.setEmail(email);
        em.persist(contacto);

        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle(calle);
        domicilio.setNumeroCalle(numeroCalle);
        em.persist(domicilio);

        Cliente cliente = new Cliente();
        cliente.setCuitCuil(cuit);
        cliente.setDenominacion(denominacion);
        cliente.setContacto(contacto);
        cliente.setDomicilio(domicilio);
        return auditar(cliente, usuario);
    }

    private Articulo articulo(Usuario usuario, Rubro rubro, Marca marca, ListaPrecio lista,
                              String codigo, String denominacion, double precio) {
        Articulo articulo = new Articulo();
        articulo.setCodigo(codigo);
        articulo.setDenominacion(denominacion);
        articulo.setRubro(rubro);
        articulo.setMarca(marca);
        auditar(articulo, usuario);

        ListaPrecioArticulo precioArticulo = new ListaPrecioArticulo();
        precioArticulo.setListaPrecio(lista);
        precioArticulo.setArticulo(articulo);
        precioArticulo.setPrecioVenta(precio);
        auditar(precioArticulo, usuario);
        return articulo;
    }

    private FacturaDetalleRequestDTO detalle(Long articuloId, double cantidad, Double bonificacion) {
        return new FacturaDetalleRequestDTO(articuloId, null, cantidad, bonificacion, null);
    }

    private FacturaRequestDTO solicitud(Long clienteId, Long puntoVentaId, Long condicionIvaId,
                                        Long tipoMonedaId, LocalDate fecha,
                                        FacturaDetalleRequestDTO... detalles) {
        return new FacturaRequestDTO(clienteId, condicionIvaId, puntoVentaId, tipoMonedaId,
                fecha, null, null, null, List.of(detalles));
    }
}
