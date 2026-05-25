package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComprobanteReemisionSriService {

  private final ComprobanteRepository comprobanteRepository;
  private final FacturaElectronicaService facturaElectronicaService;
  private final NotaCreditoElectronicaService notaCreditoElectronicaService;
  private final DocumentoModificadoEmisionService documentoModificadoEmisionService;
  private final GuiaRemisionElectronicaService guiaRemisionElectronicaService;
  private final RetencionElectronicaService retencionElectronicaService;
  private final LiquidacionCompraElectronicaService liquidacionCompraElectronicaService;

  @Transactional
  public ComprobanteResponse reemitirAlSri(UUID empresaId, UUID comprobanteId) {
    Comprobante c =
        comprobanteRepository
            .findByIdAndEmpresa_Id(comprobanteId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
    ComprobanteSriReemisionSupport.validarPuedeReemitir(c.getEstadoSri());
    if (c.getClaveAcceso() == null || c.getClaveAcceso().length() != 49) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El comprobante no tiene una clave de acceso válida para reenvío");
    }
    String tipo = c.getTipo() == null ? "" : c.getTipo().trim().toUpperCase();
    return switch (tipo) {
      case "FACTURA" -> facturaElectronicaService.reemitirAlSri(empresaId, comprobanteId);
      case "NOTA_CREDITO" -> notaCreditoElectronicaService.reemitirAlSri(empresaId, comprobanteId);
      case "NOTA_DEBITO" -> documentoModificadoEmisionService.reemitirAlSri(empresaId, comprobanteId, "NOTA_DEBITO");
      case "GUIA_REMISION" -> guiaRemisionElectronicaService.reemitirAlSri(empresaId, comprobanteId);
      case "RETENCION" -> retencionElectronicaService.reemitirAlSri(empresaId, comprobanteId);
      case "LIQUIDACION_COMPRA" -> liquidacionCompraElectronicaService.reemitirAlSri(empresaId, comprobanteId);
      default ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Reenvío al SRI no disponible para tipo " + c.getTipo());
    };
  }
}
