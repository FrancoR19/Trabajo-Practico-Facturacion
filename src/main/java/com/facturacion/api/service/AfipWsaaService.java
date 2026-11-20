package com.facturacion.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Properties;

@Service
public class AfipWsaaService {

    private String token;
    private String sign;
    private LocalDateTime fechaVencimiento;

    @Value("${afip.wsaa.pfx-path}")
    private String pfxPath;

    @Value("${afip.wsaa.pfx-password}")
    private String pfxPassword;
    private final File cacheFile = new File(System.getProperty("user.home"), "afip_ta_cache.properties");

    public synchronized String[] obtenerTokenYSignValidos() {
        // 1. Intentar cargar desde archivo si la memoria está vacía
        if (token == null) {
            cargarDesdeCache();
        }

        // 2. Si sigue nulo o venció, solicitamos uno nuevo a AFIP
        if (token == null || fechaVencimiento == null || LocalDateTime.now().isAfter(fechaVencimiento)) {
            renovarCredenciales();
        }

        return new String[]{token, sign};
    }

    private void renovarCredenciales() {
        try {
            String[] auth = AfipWsaaClient.obtenerTokenYSign(pfxPath, pfxPassword);
            this.token = auth[0];
            this.sign = auth[1];

            // Establecemos vencimiento local en 11 horas (dura 12h en AFIP)
            this.fechaVencimiento = LocalDateTime.now().plusHours(11);

            // Guardar en disco para sobrevivir a reinicios de Spring Boot
            guardarEnCache();
            System.out.println("Token y Sign renovados con éxito en WSAA y guardados en cache.");
        } catch (Exception e) {
            throw new RuntimeException("Error al autenticar con el WSAA de AFIP: " + e.getMessage(), e);
        }
    }

    private void guardarEnCache() {
        try (OutputStream out = new FileOutputStream(cacheFile)) {
            Properties props = new Properties();
            props.setProperty("token", token);
            props.setProperty("sign", sign);
            props.setProperty("vencimiento", fechaVencimiento.toString());
            props.store(out, "AFIP TA Cache");
        } catch (IOException e) {
            System.err.println("No se pudo guardar la cache del TA de AFIP: " + e.getMessage());
        }
    }

    private void cargarDesdeCache() {
        if (!cacheFile.exists()) return;

        try (InputStream in = new FileInputStream(cacheFile)) {
            Properties props = new Properties();
            props.load(in);

            String tokenStr = props.getProperty("token");
            String signStr = props.getProperty("sign");
            String vtoStr = props.getProperty("vencimiento");

            if (tokenStr != null && signStr != null && vtoStr != null) {
                LocalDateTime vto = LocalDateTime.parse(vtoStr);
                if (LocalDateTime.now().isBefore(vto)) {
                    this.token = tokenStr;
                    this.sign = signStr;
                    this.fechaVencimiento = vto;
                    System.out.println("Token y Sign reutilizados desde cache local.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al leer cache de AFIP TA: " + e.getMessage());
        }
    }
}