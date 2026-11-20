package com.facturacion.api;

import com.facturacion.api.service.AfipWsaaClient;
import com.facturacion.api.service.AfipWsfeClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FacturacionApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacturacionApiApplication.class, args);

    }
}



