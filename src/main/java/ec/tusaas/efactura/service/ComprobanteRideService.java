package ec.tusaas.efactura.service;



import ec.tusaas.efactura.emision.DocumentoModificadoRideUtil;
import ec.tusaas.efactura.emision.RideComprobantePdfGeneratorService;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.repository.ComprobanteDetalleRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

@RequiredArgsConstructor

public class ComprobanteRideService {



  private final ComprobanteDetalleRepository comprobanteDetalleRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final RideComprobantePdfGeneratorService rideComprobantePdfGeneratorService;

  @Transactional(readOnly = true)
  public byte[] generarPdf(Comprobante comprobante) {
    completarDatosFacturaModificadaParaRide(comprobante);
    var detalles = comprobanteDetalleRepository.findByComprobante_IdOrderByLineaAsc(comprobante.getId());
    return rideComprobantePdfGeneratorService.generar(comprobante, detalles);
  }

  /** Completa número y fecha de la factura sustento en memoria para el PDF (NC/ND). */
  private void completarDatosFacturaModificadaParaRide(Comprobante comprobante) {
    if (!DocumentoModificadoRideUtil.esNotaCreditoODebito(comprobante)) {
      return;
    }
    var datos = DocumentoModificadoRideUtil.leerParaRide(comprobante);
    if (datos != null
        && !datos.numeroComprobante().isBlank()
        && !datos.fechaEmisionModificado().isBlank()) {
      return;
    }
    Map<String, Object> cd =
        new HashMap<>(comprobante.getCustomData() != null ? comprobante.getCustomData() : Map.of());
    UUID facturaId = parseUuid(cd.get("facturaModificadaId"));
    if (facturaId == null) {
      facturaId = parseUuid(cd.get("facturaOrigenId"));
    }
    if (facturaId == null) {
      return;
    }
    comprobanteRepository
        .findById(facturaId)
        .ifPresent(
            factura -> {
              DocumentoModificadoRideUtil.enriquecerCustomDataDesdeFactura(cd, factura);
              comprobante.setCustomData(cd);
            });
  }

  private static UUID parseUuid(Object raw) {
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(String.valueOf(raw).trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}

