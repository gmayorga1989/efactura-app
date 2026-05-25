package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.emision.ride.RideDocumentoTitulo;
import ec.tusaas.efactura.emision.ride.RideLayoutPdfBuilder;
import ec.tusaas.efactura.emision.ride.RideLogoService;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import ec.tusaas.efactura.service.RidePlantillaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Genera el RIDE en PDF con la plantilla visual configurada por tipo de comprobante. */
@Service
@RequiredArgsConstructor
public class RideComprobantePdfGeneratorService {

  private final RidePlantillaService ridePlantillaService;
  private final RideLogoService rideLogoService;

  public byte[] generar(Comprobante comprobante, List<ComprobanteDetalle> detalles) {
    String tipo = comprobante.getTipo() == null ? "FACTURA" : comprobante.getTipo();
    RidePlantillaDto plantilla = ridePlantillaService.desdeEmpresa(comprobante.getEmpresa(), tipo);
    RideDocumentoTitulo titulo = RideDocumentoTitulo.fromTipo(tipo);
    var logo = rideLogoService.cargarLogo(comprobante.getEmpresa());
    return RideLayoutPdfBuilder.generar(comprobante, detalles, plantilla, titulo, logo);
  }
}
