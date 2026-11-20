package com.facturacion.api.service;

import com.afip.wsfe.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class AfipWsfeClient {

    public record ResultadoCae(String cae, Date fechaVencimiento, String resultadoAfip) {
    }

    private static final DateTimeFormatter FORMATO_AFIP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static volatile ServiceSoap serviceSoap;

    private static ServiceSoap obtenerServiceSoap() {
        if (serviceSoap == null) {
            synchronized (AfipWsfeClient.class) {
                if (serviceSoap == null) {
                    serviceSoap = new com.afip.wsfe.Service().getServiceSoap();
                }
            }
        }
        return serviceSoap;
    }

    public static int obtenerUltimoComprobante(String token, String sign, long cuit, int ptoVta, int cbteTipo) {
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token);
        auth.setSign(sign);
        auth.setCuit(cuit);

        FERecuperaLastCbteResponse ultimoResp = obtenerServiceSoap().feCompUltimoAutorizado(auth, ptoVta, cbteTipo);
        return ultimoResp.getCbteNro();
    }

    public String solicitarCae(String token, String sign, Long cuitEmisor, int ptoVta, long nuevoNro,
                               int cbteTipo, int docTipo, long docNroCliente,
                               double total, double impNeto, double impIva, double impTrib, int condicionIvaReceptor) {
        ResultadoCae resultado = solicitarCaeDetallado(
                token, sign, cuitEmisor, ptoVta, nuevoNro,
                cbteTipo, docTipo, docNroCliente,
                total, impNeto, impIva, impTrib, condicionIvaReceptor);
        return resultado.cae();
    }

    public ResultadoCae solicitarCaeDetallado(String token, String sign, Long cuitEmisor, int ptoVta, long nuevoNro,
                                              int cbteTipo, int docTipo, long docNroCliente,
                                              double total, double impNeto, double impIva, double impTrib, int condicionIvaReceptor) {
        // Credenciales
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token);
        auth.setSign(sign);
        auth.setCuit(cuitEmisor);

        // Cabecera
        FECAERequest feReq = new FECAERequest();
        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(ptoVta);
        cab.setCbteTipo(cbteTipo);
        feReq.setFeCabReq(cab);

        // Detalle
        ArrayOfFECAEDetRequest detArray = new ArrayOfFECAEDetRequest();
        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(1); // 1 = Productos
        det.setDocTipo(docTipo); // 80 = CUIT, 99 = Consumidor Final
        det.setDocNro(docNroCliente);
        det.setCbteDesde(nuevoNro);
        det.setCbteHasta(nuevoNro);

        // Formato de fecha y moneda
        det.setCbteFch(LocalDate.now().format(FORMATO_AFIP));
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        // Importes dinámicos
        det.setImpTotal(total);
        det.setImpTotConc(0.00);
        det.setImpNeto(impNeto);
        det.setImpOpEx(0.00);
        det.setImpTrib(impTrib);
        det.setImpIVA(impIva);
        det.setCondicionIVAReceptorId(condicionIvaReceptor);

        // Array de IVA
        if (impIva > 0) {
            ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
            AlicIva alicIva = new AlicIva();
            alicIva.setId(5); // 5 = 21%
            alicIva.setBaseImp(impNeto);
            alicIva.setImporte(impIva);
            arrayIva.getAlicIva().add(alicIva);
            det.setIva(arrayIva);
        }

        detArray.getFECAEDetRequest().add(det);
        feReq.setFeDetReq(detArray);

        // Llamada al web service
        FECAEResponse resp = obtenerServiceSoap().fecaeSolicitar(auth, feReq);

        // Verificamos si ARCA aprobó el comprobante
        if (resp.getFeDetResp() != null && resp.getFeDetResp().getFECAEDetResponse() != null
                && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            com.afip.wsfe.FECAEDetResponse detalleResp = resp.getFeDetResp().getFECAEDetResponse().get(0);

            if ("A".equals(detalleResp.getResultado())) {
                Date fechaVto = parsearFechaAfip(detalleResp.getCAEFchVto());
                return new ResultadoCae(detalleResp.getCAE(), fechaVto, "FACTURA APROBADA");
            }
        }

        // Si no fue aprobado, recopilamos los motivos del rechazo
        StringBuilder errores = new StringBuilder();

        if (resp.getErrors() != null && resp.getErrors().getErr() != null) {
            for (com.afip.wsfe.Err err : resp.getErrors().getErr()) {
                errores.append("Error ").append(err.getCode()).append(": ").append(err.getMsg()).append(" | ");
            }
        }

        if (resp.getFeDetResp() != null && resp.getFeDetResp().getFECAEDetResponse() != null
                && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            com.afip.wsfe.FECAEDetResponse detalleResp = resp.getFeDetResp().getFECAEDetResponse().get(0);
            if (detalleResp.getObservaciones() != null && detalleResp.getObservaciones().getObs() != null) {
                for (com.afip.wsfe.Obs obs : detalleResp.getObservaciones().getObs()) {
                    errores.append("Obs ").append(obs.getCode()).append(": ").append(obs.getMsg()).append(" | ");
                }
            }
        }

        throw new RuntimeException("Rechazado por AFIP: " + errores);
    }

    private Date parsearFechaAfip(String fechaAfip) {
        try {
            if (fechaAfip != null && fechaAfip.length() == 8) {
                LocalDate ld = LocalDate.parse(fechaAfip, FORMATO_AFIP);
                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
        } catch (Exception ignored) {
        }
        return Date.from(LocalDate.now().plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
