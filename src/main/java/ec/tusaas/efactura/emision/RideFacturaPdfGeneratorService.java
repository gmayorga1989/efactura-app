package ec.tusaas.efactura.emision;

import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** @deprecated Usar {@link RideComprobantePdfGeneratorService}. */
@Deprecated
@Service
@RequiredArgsConstructor
public class RideFacturaPdfGeneratorService {

  private final RideComprobantePdfGeneratorService rideComprobantePdfGeneratorService;

  public byte[] generar(Comprobante comprobante, List<ComprobanteDetalle> detalles) {
    return rideComprobantePdfGeneratorService.generar(comprobante, detalles);
  }
}
