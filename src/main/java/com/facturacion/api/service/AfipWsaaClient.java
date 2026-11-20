package com.facturacion.api.service;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Base64;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.operator.ContentSigner;

@Service
public class AfipWsaaClient {

    private static final String URL_WSAA_HOMO = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";

    private String token;
    private String sign;
    private LocalDateTime fechaVencimiento;

    public static String[] obtenerTokenYSign(String pfxPath, String pfxPassword) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 1. Cargar el almacén PKCS12 (.pfx / .p12) de forma tolerante a rutas
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream fis = abrirCertificado(pfxPath)) {
            keystore.load(fis, pfxPassword.toCharArray());
        }

        // 2. Obtener el alias de la clave privada
        String alias = null;
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String al = aliases.nextElement();
            if (keystore.isKeyEntry(al)) {
                alias = al;
                break;
            }
        }
        if (alias == null) {
            alias = keystore.aliases().nextElement();
        }

        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, pfxPassword.toCharArray());
        X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);

        if (privateKey == null) {
            throw new RuntimeException("No se encontró la clave privada en el keystore con el alias: " + alias + ". Verifica la contraseña.");
        }

        // 3. Fechas sin nanosegundos (.withNano(0)) y en la zona horaria oficial de Argentina
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Argentina/Buenos_Aires")).withNano(0);
        String genTime = now.minusMinutes(10).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expTime = now.plusHours(12).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 4. uniqueId en segundos (Timestamp UNIX de 10 dígitos) para respetar el xs:unsignedInt de AFIP
        long uniqueId = System.currentTimeMillis() / 1000;

        // 5. Estructura XML limpia
        String traXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<uniqueId>" + uniqueId + "</uniqueId>" +
                "<generationTime>" + genTime + "</generationTime>" +
                "<expirationTime>" + expTime + "</expirationTime>" +
                "</header>" +
                "<service>wsfe</service>" +
                "</loginTicketRequest>";

        // 6. Firma digital CMS / PKCS#7 usando BouncyCastle
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(privateKey);

        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                .build(sha256Signer, cert));

        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));
        CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8)), true);

        String cmsBase64 = Base64.getEncoder().encodeToString(signedData.getEncoded());

        // 7. Sobre SOAP para la llamada a LoginCms
        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:nso=\"http://wsaa.afip.gov.ar/\">" +
                "<soapenv:Header/><soapenv:Body><nso:loginCms><nso:in0>" + cmsBase64 + "</nso:in0></nso:loginCms></soapenv:Body></soapenv:Envelope>";

        // 8. Enviar petición HTTP POST a AFIP con la cabecera SOAPAction obligatoria
        String responseXml = enviarSoap(URL_WSAA_HOMO, soapEnvelope);

        // 9. Extraer Token y Sign de la respuesta XML
        String xmlLimpio = responseXml.replace("&lt;", "<").replace("&gt;", ">");
        String token = xmlLimpio.substring(xmlLimpio.indexOf("<token>") + 7, xmlLimpio.indexOf("</token>"));
        String sign = xmlLimpio.substring(xmlLimpio.indexOf("<sign>") + 6, xmlLimpio.indexOf("</sign>"));

        return new String[]{token, sign};
    }

    private static InputStream abrirCertificado(String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("La ruta del certificado .p12 / .pfx no puede estar vacía.");
        }

        String pathLimpia = path.replace("\"", "").trim();

        // 1. Probar como archivo en disco
        File file = new File(pathLimpia);
        if (file.exists() && file.isFile()) {
            return new FileInputStream(file);
        }

        // 2. Probar como recurso en el classpath (por si se usa ruta relativa o classpath:)
        String normalizada = pathLimpia.replace("\\", "/");
        if (normalizada.startsWith("classpath:")) {
            normalizada = normalizada.substring("classpath:".length());
        }
        if (normalizada.contains("certificados/")) {
            normalizada = normalizada.substring(normalizada.indexOf("certificados/"));
        }

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalizada);
        if (is != null) {
            return is;
        }

        // 3. Fallback: buscar directamente en carpeta /certificados/ por nombre de archivo
        String nombre = new File(pathLimpia).getName();
        is = Thread.currentThread().getContextClassLoader().getResourceAsStream("certificados/" + nombre);
        if (is != null) {
            return is;
        }

        throw new FileNotFoundException("No se encontró el archivo de certificado en: " + pathLimpia +
                ". Asegúrate de que el archivo exista y no contenga caracteres de escape.");
    }

    private static String enviarSoap(String urlStr, String soapXml) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", ""); // Requerido por AFIP
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            if (responseCode >= 400) {
                throw new RuntimeException("Rechazo de AFIP (" + responseCode + "): " + sb.toString());
            }
            return sb.toString();
        }
    }
}
