package com.facturacion.api.modelo;

import java.util.Map;

public final class TipoComprobante {

    public static final String FACTURA_A = "FA";
    public static final String FACTURA_B = "FB";
    public static final String DEFAULT = FACTURA_B;

    private static final Map<String, Integer> CODIGOS_AFIP = Map.of(
            "FA", 1,
            "NDA", 2,
            "NCA", 3,
            "FB", 6,
            "NDB", 7,
            "NCB", 8);

    private static final Map<String, String> DESCRIPCIONES = Map.of(
            "FA", "FACTURA A",
            "NDA", "NOTA DE DEBITO A",
            "NCA", "NOTA DE CREDITO A",
            "FB", "FACTURA B",
            "NDB", "NOTA DE DEBITO B",
            "NCB", "NOTA DE CREDITO B");

    private TipoComprobante() {
    }

    public static String porCondicionIva(CondicionIva condicionIva) {
        return condicionIva != null && condicionIva.getCodigoAfip() == 1 ? FACTURA_A : FACTURA_B;
    }

    public static String normalizar(String tipoComprobante) {
        if (tipoComprobante == null || tipoComprobante.isBlank()) {
            return null;
        }
        return tipoComprobante.trim().toUpperCase();
    }

    public static boolean esValido(String tipoComprobante) {
        return CODIGOS_AFIP.containsKey(normalizar(tipoComprobante));
    }

    public static int codigoAfip(String tipoComprobante) {
        String normalizado = normalizar(tipoComprobante);
        if (normalizado == null) {
            return CODIGOS_AFIP.get(DEFAULT);
        }
        return CODIGOS_AFIP.getOrDefault(normalizado, CODIGOS_AFIP.get(DEFAULT));
    }

    public static String descripcion(String tipoComprobante) {
        String normalizado = normalizar(tipoComprobante);
        if (normalizado == null) {
            return DESCRIPCIONES.get(DEFAULT);
        }
        return DESCRIPCIONES.getOrDefault(normalizado, normalizado);
    }
}
