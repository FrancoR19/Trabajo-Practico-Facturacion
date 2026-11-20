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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final int MIN_CLIENTES = 50;
    private static final int MIN_ARTICULOS = 100;

    private static final String[][] ARTICULOS_BASE = {
            {"ART-001", "Notebook 14 pulgadas", "500000"},
            {"ART-002", "Mouse inalambrico", "18000"},
            {"ART-003", "Teclado mecanico", "55000"},
            {"ART-004", "Monitor 24 pulgadas", "210000"},
            {"ART-005", "Auricular bluetooth", "42000"},
            {"ART-006", "Webcam HD", "35000"},
            {"ART-007", "Disco SSD 1TB", "95000"},
            {"ART-008", "Impresora laser", "180000"}
    };

    private static final String[] PRODUCTOS_CATALOGO = {
            "Notebook 15.6 pulgadas", "Monitor 27 pulgadas IPS", "Monitor Curvo 32 pulgadas",
            "Teclado ergonomico inalambrico", "Mouse gamer RGB", "Mouse vertical ergonomico",
            "Disco SSD NVMe 500GB", "Disco SSD NVMe 2TB", "Disco rigido externo 2TB",
            "Memoria RAM DDR4 16GB", "Memoria RAM DDR5 32GB", "Placa de video 8GB",
            "Procesador 8 nucleos", "Motherboard ATX", "Fuente certificada 750W",
            "Gabinete ATX vidriado", "Cooler CPU torre", "Pasta termica profesional",
            "Router Wi-Fi 6 dual band", "Switch Gigabit 8 puertos", "Switch Gigabit 16 puertos",
            "Access Point empresarial", "Cable de red Cat6 10m", "Adaptador USB-C a Ethernet",
            "Hub USB-C 7 en 1", "Docking station Thunderbolt", "Cable HDMI 2.1 3m",
            "Cable DisplayPort 1.4 2m", "Soporte articulado para monitor", "Base refrigerante notebook",
            "Mochila porta notebook 15.6", "Funda acolchada 14 pulgadas", "Impresora multifuncion Wi-Fi",
            "Toner laser negro", "Cartucho tinta tricolor", "Resma papel A4 75g",
            "Escaner de documentos duplex", "Lector codigo de barras USB", "Impresora termica tickets",
            "Estabilizador de tension 1200VA", "UPS interactiva 1000VA", "UPS online 2000VA",
            "Zapatilla protectora 6 tomas", "Microfono condensador USB", "Brazo articulado microfono",
            "Webcam Full HD 1080p", "Camara conferencia 4K", "Parlantes estereo escritorio",
            "Barra de sonido compacta", "Auricular vincha con microfono", "Auricular in-ear TWS",
            "Tablet 10.4 pulgadas 128GB", "Lapiz optico capacitivo", "Cargador rapido GaN 65W",
            "Power bank 20000mAh", "Pendrive USB 3.2 64GB", "Pendrive USB 3.2 128GB",
            "Tarjeta microSD 256GB", "Lector de tarjetas SD/microSD", "Proyector LED Full HD",
            "Pantalla retráctil 100 pulgadas", "Pizarra magnetica 120x90", "Silla ergonomica oficina",
            "Escritorio regulable en altura", "Lampara LED escritorio USB", "Destructora de papel",
            "Calculadoras cientifica", "Telefono IP escritorio", "Repetidor Wi-Fi mesh",
            "Antena Wi-Fi USB AC1300", "Placa red PCIe Gigabit", "Controlador RAID SATA",
            "Caja externa disco 2.5 USB 3.0", "Bandeja rack 19 pulgadas", "Patch panel 24 puertos",
            "Organizador de cables escritorio", "Kit destornilladores precision", "Soplador aire comprimido",
            "Limpiador de pantallas 250ml", "Alfombrilla mouse XL", "Reposamunecas gel teclado",
            "Cerradura seguridad notebook", "Filtro privacidad 14 pulgadas", "Filtro privacidad 24 pulgadas",
            "Adaptador DVI a HDMI", "Adaptador VGA a HDMI", "Conversor audio optico a RCA",
            "Extensor HDMI por UTP 60m", "Splitter HDMI 4 salidas", "KVM switch 2 puertos USB",
            "Etiquetadora termica portatil", "Rollo etiquetas termicas x1000"
    };

    private static final String[] CALLES = {
            "San Martin", "Belgrano", "Rivadavia", "Sarmiento", "Mitre",
            "Corrientes", "Cordoba", "Santa Fe", "Av. Colon", "Av. Pellegrini",
            "9 de Julio", "25 de Mayo", "Urquiza", "Alberdi", "Lavalle"
    };

    private static final String[] NOMBRES_EMPRESA = {
            "Soluciones Digitales", "Grupo Norte", "Distribuidora Sur", "Servicios Integrales",
            "Ingenieria Austral", "Comercial del Plata", "Logistica Express", "Sistemas Avanzados",
            "Consultora Pampa", "Industrias Cuyo", "Tecnologia Litoral", "Redes y Datos",
            "Equipamiento Moderno", "Insumos Corporativos", "Oficina Central", "Mercado Digital",
            "Desarrollo Urbano", "Energia Limpia", "Constructora Andina", "Alimentos del Valle",
            "Textil Patagonia", "Metalurgica Oeste", "Laboratorio Bio", "Transporte Federal",
            "Editorial Horizonte"
    };

    private static final String[] SUFIJOS_EMPRESA = {
            "S.A.", "S.R.L.", "S.A.S."
    };

    private static final String[] NOMBRES_PERSONA = {
            "Lucas", "Maria", "Carlos", "Lucia", "Martin", "Sofia", "Diego", "Valentina",
            "Mateo", "Camila", "Nicolas", "Paula", "Federico", "Julieta", "Tomas",
            "Agustina", "Santiago", "Florencia", "Joaquin", "Carolina", "Emiliano", "Antonella",
            "Facundo", "Micaela", "Gonzalo"
    };

    private static final String[] APELLIDOS_PERSONA = {
            "Gomez", "Rodriguez", "Fernandez", "Lopez", "Martinez", "Garcia", "Sanchez",
            "Romero", "Diaz", "Torres", "Alvarez", "Ruiz", "Ramirez", "Flores", "Acosta",
            "Benitez", "Medina", "Suarez", "Herrera", "Aguirre", "Pereyra", "Castro",
            "Molina", "Silva", "Rojas"
    };

    @PersistenceContext
    private EntityManager em;

    private final FacturaService facturaService;

    public DataSeeder(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        buscarOCrearUsuario("sistema", "Sistema", "Auditoria");
        Usuario jperez = buscarOCrearUsuario("jperez", "Juan", "Perez");

        PuntoVenta central = buscarOCrearPuntoVenta(jperez, 1, "Sucursal Central");
        PuntoVenta norte = buscarOCrearPuntoVenta(jperez, 2, "Sucursal Norte");
        CondicionIva responsable = buscarOCrearCondicionIva(jperez, 1, "IVA Responsable Inscripto");
        CondicionIva consumidorFinal = buscarOCrearCondicionIva(jperez, 5, "Consumidor Final");
        TipoMoneda peso = buscarOCrearTipoMoneda(jperez, "PES", "Peso Argentino", "$");
        buscarOCrearTipoMoneda(jperez, "DOL", "Dolar Estadounidense", "US$");

        ListaPrecio minorista = buscarOCrearListaPrecio(jperez, "LP-GEN", "Lista Minorista");
        ListaPrecio mayorista = buscarOCrearListaPrecio(jperez, "LP-MAY", "Lista Mayorista");

        List<Rubro> rubros = asegurarRubros(jperez);
        List<Marca> marcas = asegurarMarcas(jperez);

        List<Cliente> clientes = asegurarClientes(jperez, responsable, consumidorFinal, minorista, mayorista);
        Map<String, Double> preciosBase = new HashMap<>();
        List<Articulo> articulos = asegurarArticulos(jperez, rubros, marcas, preciosBase);

        List<Long> preciosMinoristaIds = asegurarListasPrecioArticulos(
                jperez, minorista, mayorista, articulos, preciosBase);

        Long facturasExistentes = em.createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class).getSingleResult();
        if (facturasExistentes == 0 && !preciosMinoristaIds.isEmpty()) {
            sembrarFacturas(clientes, central, norte, responsable, consumidorFinal, peso, preciosMinoristaIds);
            System.out.println("DataSeeder: 25 facturas de ejemplo creadas.");
        } else {
            System.out.println("DataSeeder: ya hay " + facturasExistentes + " facturas, no se recrean facturas.");
        }

        System.out.println("DataSeeder: estado final -> "
                + clientes.size() + " clientes, "
                + articulos.size() + " articulos, listas LP-GEN (Lista Minorista) y LP-MAY (Lista Mayorista).");
    }

    private Usuario buscarOCrearUsuario(String username, String nombre, String apellido) {
        List<Usuario> encontrados = em.createQuery(
                        "SELECT u FROM Usuario u WHERE u.usuario = :usuario", Usuario.class)
                .setParameter("usuario", username)
                .getResultList();
        if (!encontrados.isEmpty()) {
            return encontrados.get(0);
        }
        Usuario usuario = new Usuario();
        usuario.setUsuario(username);
        usuario.setClave("hash_no_real");
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        em.persist(usuario);
        return usuario;
    }

    private PuntoVenta buscarOCrearPuntoVenta(Usuario usuario, int numero, String descripcion) {
        List<PuntoVenta> encontrados = em.createQuery(
                        "SELECT p FROM PuntoVenta p WHERE p.numero = :numero", PuntoVenta.class)
                .setParameter("numero", numero)
                .getResultList();
        if (!encontrados.isEmpty()) {
            return encontrados.get(0);
        }
        PuntoVenta puntoVenta = new PuntoVenta();
        puntoVenta.setNumero(numero);
        puntoVenta.setDescripcion(descripcion);
        puntoVenta.setTipoEmision("Electronica");
        return auditar(puntoVenta, usuario);
    }

    private CondicionIva buscarOCrearCondicionIva(Usuario usuario, int codigoAfip, String denominacion) {
        List<CondicionIva> encontrados = em.createQuery(
                        "SELECT c FROM CondicionIva c WHERE c.codigoAfip = :codigo", CondicionIva.class)
                .setParameter("codigo", codigoAfip)
                .getResultList();
        if (!encontrados.isEmpty()) {
            return encontrados.get(0);
        }
        CondicionIva condicion = new CondicionIva();
        condicion.setCodigoAfip(codigoAfip);
        condicion.setDenominacion(denominacion);
        return auditar(condicion, usuario);
    }

    private TipoMoneda buscarOCrearTipoMoneda(Usuario usuario, String codigoAfip, String denominacion, String simbolo) {
        List<TipoMoneda> encontrados = em.createQuery(
                        "SELECT t FROM TipoMoneda t WHERE t.codigoAfip = :codigo", TipoMoneda.class)
                .setParameter("codigo", codigoAfip)
                .getResultList();
        if (!encontrados.isEmpty()) {
            return encontrados.get(0);
        }
        TipoMoneda moneda = new TipoMoneda();
        moneda.setCodigoAfip(codigoAfip);
        moneda.setDenominacion(denominacion);
        moneda.setSimbolo(simbolo);
        return auditar(moneda, usuario);
    }

    private ListaPrecio buscarOCrearListaPrecio(Usuario usuario, String codigo, String denominacion) {
        List<ListaPrecio> encontrados = em.createQuery(
                        "SELECT l FROM ListaPrecio l WHERE l.codigo = :codigo", ListaPrecio.class)
                .setParameter("codigo", codigo)
                .getResultList();
        if (!encontrados.isEmpty()) {
            ListaPrecio existente = encontrados.get(0);
            if (!denominacion.equals(existente.getDenominacion())) {
                existente.setDenominacion(denominacion);
                existente.setFechaModificacion(new Date());
                existente.setUsuarioModificacion(usuario);
            }
            return existente;
        }
        ListaPrecio lista = new ListaPrecio();
        lista.setCodigo(codigo);
        lista.setDenominacion(denominacion);
        return auditar(lista, usuario);
    }

    private List<Rubro> asegurarRubros(Usuario usuario) {
        Object[][] datos = {
                {10, "Tecnologia"},
                {20, "Perifericos"},
                {30, "Redes y Conectividad"},
                {40, "Impresion e Insumos"},
                {50, "Mobiliario y Oficina"}
        };
        List<Rubro> rubros = new ArrayList<>();
        for (Object[] d : datos) {
            int codigo = (int) d[0];
            String denom = (String) d[1];
            List<Rubro> encontrados = em.createQuery(
                            "SELECT r FROM Rubro r WHERE r.codigo = :codigo", Rubro.class)
                    .setParameter("codigo", codigo)
                    .getResultList();
            if (!encontrados.isEmpty()) {
                rubros.add(encontrados.get(0));
            } else {
                Rubro rubro = new Rubro();
                rubro.setCodigo(codigo);
                rubro.setDenominacion(denom);
                rubros.add(auditar(rubro, usuario));
            }
        }
        return rubros;
    }

    private List<Marca> asegurarMarcas(Usuario usuario) {
        Object[][] datos = {
                {55, "Marca Generica"},
                {56, "NovaTech"},
                {57, "HyperCore"},
                {58, "ProOffice"},
                {59, "NetLink"}
        };
        List<Marca> marcas = new ArrayList<>();
        for (Object[] d : datos) {
            int codigo = (int) d[0];
            String denom = (String) d[1];
            List<Marca> encontrados = em.createQuery(
                            "SELECT m FROM Marca m WHERE m.codigo = :codigo", Marca.class)
                    .setParameter("codigo", codigo)
                    .getResultList();
            if (!encontrados.isEmpty()) {
                marcas.add(encontrados.get(0));
            } else {
                Marca marca = new Marca();
                marca.setCodigo(codigo);
                marca.setDenominacion(denom);
                marcas.add(auditar(marca, usuario));
            }
        }
        return marcas;
    }

    private List<Cliente> asegurarClientes(Usuario usuario, CondicionIva responsable, CondicionIva consumidorFinal,
                                           ListaPrecio minorista, ListaPrecio mayorista) {
        List<Cliente> existentes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.id", Cliente.class)
                .getResultList();

        for (Cliente c : existentes) {
            boolean modificado = false;
            if (c.getCondicionIva() == null) {
                boolean esEmpresa = esClienteComercial(c);
                c.setCondicionIva(esEmpresa ? responsable : consumidorFinal);
                modificado = true;
            }
            if (c.getListaPrecio() == null) {
                c.setListaPrecio(esClienteComercial(c) ? mayorista : minorista);
                modificado = true;
            }
            if (modificado) {
                em.merge(c);
            }
        }

        if (existentes.size() >= MIN_CLIENTES) {
            return existentes;
        }

        Set<String> cuitsUsados = new HashSet<>();
        Set<String> emailsUsados = new HashSet<>();
        for (Cliente c : existentes) {
            cuitsUsados.add(c.getCuitCuil());
            if (c.getContacto() != null && c.getContacto().getEmail() != null) {
                emailsUsados.add(c.getContacto().getEmail().toLowerCase());
            }
        }

        List<Cliente> resultado = new ArrayList<>(existentes);
        long dniEmpresaBase = 71000001L;
        long dniPersonaBase = 25000001L;
        int indice = 0;

        while (resultado.size() < MIN_CLIENTES) {
            boolean esEmpresa = indice % 2 == 0;
            String cuit;
            if (esEmpresa) {
                do {
                    cuit = generarCuitValido("30", dniEmpresaBase++);
                } while (cuitsUsados.contains(cuit));
            } else {
                do {
                    cuit = generarCuitValido("20", dniPersonaBase++);
                } while (cuitsUsados.contains(cuit));
            }
            cuitsUsados.add(cuit);

            String denominacion;
            String emailBase;
            if (esEmpresa) {
                int empIdx = (indice / 2) % NOMBRES_EMPRESA.length;
                int sufIdx = (indice / 2) % SUFIJOS_EMPRESA.length;
                int ciclo = (indice / 2) / NOMBRES_EMPRESA.length;
                denominacion = NOMBRES_EMPRESA[empIdx]
                        + (ciclo > 0 ? " " + (ciclo + 1) : "")
                        + " " + SUFIJOS_EMPRESA[sufIdx];
                emailBase = "contacto.emp" + (indice + 1) + "@empresas.com.ar";
            } else {
                int nomIdx = (indice / 2) % NOMBRES_PERSONA.length;
                int apeIdx = ((indice / 2) + (indice / 10)) % APELLIDOS_PERSONA.length;
                denominacion = APELLIDOS_PERSONA[apeIdx] + " " + NOMBRES_PERSONA[nomIdx];
                emailBase = NOMBRES_PERSONA[nomIdx].toLowerCase() + "."
                        + APELLIDOS_PERSONA[apeIdx].toLowerCase() + (indice + 1) + "@mail.com";
            }

            String email = emailBase;
            int sufijoEmail = 1;
            while (emailsUsados.contains(email.toLowerCase())) {
                email = "cliente" + (indice + 1) + "_" + (sufijoEmail++) + "@mail.com";
            }
            emailsUsados.add(email.toLowerCase());

            String calle = CALLES[indice % CALLES.length];
            String numeroCalle = String.valueOf(100 + (indice * 37) % 4500);

            CondicionIva condIva = esEmpresa ? responsable : consumidorFinal;
            ListaPrecio listaPorDefecto = esEmpresa ? mayorista : minorista;
            resultado.add(crearCliente(usuario, cuit, denominacion, email, calle, numeroCalle, condIva, listaPorDefecto));
            indice++;
        }

        return resultado;
    }

    private boolean esClienteComercial(Cliente cliente) {
        if (cliente.getCondicionIva() != null && cliente.getCondicionIva().getCodigoAfip() == 1) {
            return true;
        }
        String cuitDigitos = cliente.getCuitCuil() != null
                ? cliente.getCuitCuil().replaceAll("\\D", "")
                : "";
        if (cuitDigitos.startsWith("30") || cuitDigitos.startsWith("33") || cuitDigitos.startsWith("34")) {
            return true;
        }
        String denom = cliente.getDenominacion() != null
                ? cliente.getDenominacion().toLowerCase(Locale.ROOT)
                : "";
        return denom.contains("s.r.l.") || denom.contains("s.a.") || denom.contains("s.a.s.")
                || denom.contains(" srl") || denom.contains(" sa");
    }

    private String generarCuitValido(String prefijo, long dniBase) {
        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        long candidato = dniBase;
        while (true) {
            String base10 = prefijo + String.format("%08d", candidato);
            int suma = 0;
            for (int i = 0; i < 10; i++) {
                suma += (base10.charAt(i) - '0') * pesos[i];
            }
            int resto = suma % 11;
            int dv = resto == 0 ? 0 : 11 - resto;
            if (dv != 10) {
                return prefijo + "-" + base10.substring(2) + "-" + dv;
            }
            candidato++;
        }
    }

    private List<Articulo> asegurarArticulos(Usuario usuario, List<Rubro> rubros, List<Marca> marcas,
                                             Map<String, Double> preciosBase) {
        List<Articulo> existentes = em.createQuery("SELECT a FROM Articulo a ORDER BY a.id", Articulo.class)
                .getResultList();

        Map<String, Articulo> porCodigo = new HashMap<>();
        for (Articulo a : existentes) {
            porCodigo.put(a.getCodigo(), a);
        }

        for (int i = 0; i < ARTICULOS_BASE.length; i++) {
            String codigo = ARTICULOS_BASE[i][0];
            String denom = ARTICULOS_BASE[i][1];
            double precio = Double.parseDouble(ARTICULOS_BASE[i][2]);
            preciosBase.put(codigo, precio);

            if (!porCodigo.containsKey(codigo)) {
                Articulo creado = crearArticulo(
                        usuario,
                        rubros.get(i % rubros.size()),
                        marcas.get(i % marcas.size()),
                        codigo,
                        denom);
                porCodigo.put(codigo, creado);
                existentes.add(creado);
            }
        }

        int nro = ARTICULOS_BASE.length + 1;
        while (existentes.size() < MIN_ARTICULOS || nro <= MIN_ARTICULOS) {
            String codigo = String.format("ART-%03d", nro);
            int prodIdx = (nro - ARTICULOS_BASE.length - 1);
            String denom = prodIdx < PRODUCTOS_CATALOGO.length
                    ? PRODUCTOS_CATALOGO[prodIdx]
                    : "Insumo Tecnologico Modelo " + nro;
            double precio = 12000.0 + ((nro * 17350) % 480000);
            preciosBase.put(codigo, precio);

            if (!porCodigo.containsKey(codigo)) {
                Articulo creado = crearArticulo(
                        usuario,
                        rubros.get(nro % rubros.size()),
                        marcas.get(nro % marcas.size()),
                        codigo,
                        denom);
                porCodigo.put(codigo, creado);
                existentes.add(creado);
            }
            nro++;
        }

        return existentes;
    }

    private List<Long> asegurarListasPrecioArticulos(Usuario usuario, ListaPrecio minorista, ListaPrecio mayorista,
                                                     List<Articulo> articulos, Map<String, Double> preciosBase) {
        List<ListaPrecioArticulo> todos = em.createQuery(
                        "SELECT lpa FROM ListaPrecioArticulo lpa", ListaPrecioArticulo.class)
                .getResultList();

        Map<Long, Map<Long, ListaPrecioArticulo>> mapa = new HashMap<>();
        for (ListaPrecioArticulo lpa : todos) {
            mapa.computeIfAbsent(lpa.getArticulo().getId(), k -> new HashMap<>())
                    .put(lpa.getListaPrecio().getId(), lpa);
        }

        List<Long> idsMinorista = new ArrayList<>();
        for (int i = 0; i < articulos.size(); i++) {
            Articulo art = articulos.get(i);
            Map<Long, ListaPrecioArticulo> porLista = mapa.computeIfAbsent(art.getId(), k -> new HashMap<>());

            ListaPrecioArticulo lpaMinorista = porLista.get(minorista.getId());
            if (lpaMinorista == null) {
                double precioMin = preciosBase.getOrDefault(art.getCodigo(), 25000.0 + ((i + 1) * 5000.0));
                lpaMinorista = crearPrecioArticulo(usuario, minorista, art, precioMin);
                porLista.put(minorista.getId(), lpaMinorista);
            }
            idsMinorista.add(lpaMinorista.getId());

            if (!porLista.containsKey(mayorista.getId())) {
                double precioMay = Math.round(lpaMinorista.getPrecioVenta() * 0.85 * 100.0) / 100.0;
                ListaPrecioArticulo lpaMayorista = crearPrecioArticulo(usuario, mayorista, art, precioMay);
                porLista.put(mayorista.getId(), lpaMayorista);
            }
        }
        return idsMinorista;
    }

    private void sembrarFacturas(List<Cliente> clientes, PuntoVenta central, PuntoVenta norte,
                                 CondicionIva responsable, CondicionIva consumidorFinal,
                                 TipoMoneda peso, List<Long> precios) {
        Cliente cliente1 = clientes.get(0);
        Cliente cliente2 = clientes.size() > 1 ? clientes.get(1) : cliente1;

        Long condCli1 = cliente1.getCondicionIva() != null ? cliente1.getCondicionIva().getId() : responsable.getId();
        FacturaReporteDTO primera = facturaService.crear(solicitud(cliente1.getId(), central.getId(),
                condCli1, peso.getId(), LocalDate.of(2026, 3, 15),
                detalle(precios.get(0), 1.0, null),
                detalle(precios.get(1 % precios.size()), 2.0, null)));
        facturaService.informarArca(primera.getId());

        facturaService.crear(solicitud(null, central.getId(), consumidorFinal.getId(), peso.getId(),
                LocalDate.of(2026, 3, 20),
                detalle(precios.get(2 % precios.size()), 1.0, null)));

        Long condCli2 = cliente2.getCondicionIva() != null ? cliente2.getCondicionIva().getId() : responsable.getId();
        FacturaReporteDTO aAnular = facturaService.crear(solicitud(cliente2.getId(), norte.getId(),
                condCli2, peso.getId(), LocalDate.of(2026, 4, 2),
                detalle(precios.get(3 % precios.size()), 1.0, 10.0)));
        FacturaRequestDTO anulacion = new FacturaRequestDTO();
        anulacion.setEstado("ANULADA");
        anulacion.setObservaciones("Anulada por error de carga");
        facturaService.actualizar(aAnular.getId(), anulacion);

        for (int i = 1; i <= 22; i++) {
            Cliente cli = i % 3 == 0 ? null : clientes.get(i % clientes.size());
            Long clienteId = cli != null ? cli.getId() : null;
            Long condicionId = cli == null
                    ? consumidorFinal.getId()
                    : (cli.getCondicionIva() != null ? cli.getCondicionIva().getId() : responsable.getId());
            PuntoVenta puntoVenta = i % 2 == 0 ? central : norte;
            LocalDate fecha = LocalDate.of(2026, 1 + (i % 9), 1 + (i * 3) % 27);

            FacturaReporteDTO factura = facturaService.crear(solicitud(clienteId, puntoVenta.getId(),
                    condicionId, peso.getId(), fecha,
                    detalle(precios.get(i % precios.size()), 1.0 + i % 3, null),
                    detalle(precios.get((i + 5) % precios.size()), 1.0, i % 4 == 0 ? 5.0 : null)));
            if (i % 4 == 0) {
                facturaService.informarArca(factura.getId());
            }
        }
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

    private Cliente crearCliente(Usuario usuario, String cuit, String denominacion, String email,
                                 String calle, String numeroCalle, CondicionIva condicionIva,
                                 ListaPrecio listaPrecio) {
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
        cliente.setCondicionIva(condicionIva);
        cliente.setListaPrecio(listaPrecio);
        return auditar(cliente, usuario);
    }

    private Articulo crearArticulo(Usuario usuario, Rubro rubro, Marca marca, String codigo, String denominacion) {
        Articulo articulo = new Articulo();
        articulo.setCodigo(codigo);
        articulo.setDenominacion(denominacion);
        articulo.setRubro(rubro);
        articulo.setMarca(marca);
        return auditar(articulo, usuario);
    }

    private ListaPrecioArticulo crearPrecioArticulo(Usuario usuario, ListaPrecio lista, Articulo articulo, double precio) {
        ListaPrecioArticulo precioArticulo = new ListaPrecioArticulo();
        precioArticulo.setListaPrecio(lista);
        precioArticulo.setArticulo(articulo);
        precioArticulo.setPrecioVenta(precio);
        return auditar(precioArticulo, usuario);
    }

    private FacturaDetalleRequestDTO detalle(Long listaPrecioArticuloId, double cantidad, Double bonificacion) {
        return new FacturaDetalleRequestDTO(listaPrecioArticuloId, cantidad, bonificacion, null);
    }

    private FacturaRequestDTO solicitud(Long clienteId, Long puntoVentaId, Long condicionIvaId,
                                        Long tipoMonedaId, LocalDate fecha,
                                        FacturaDetalleRequestDTO... detalles) {
        return new FacturaRequestDTO(clienteId, null, condicionIvaId, puntoVentaId, tipoMonedaId,
                fecha, null, null, null, List.of(detalles));
    }
}
