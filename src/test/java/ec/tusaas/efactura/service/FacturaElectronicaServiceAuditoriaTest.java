package ec.tusaas.efactura.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import ec.tusaas.efactura.emision.XmlFacturaGeneratorService;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.repository.ApiKeyRepository;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.repository.ComprobanteDetalleRepository;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.repository.VendedorRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.sri.client.SriAutorizacionClient;
import ec.tusaas.efactura.sri.client.SriRecepcionClient;
import ec.tusaas.efactura.sri.signature.XmlSignerService;
import ec.tusaas.efactura.sri.xml.XmlXsdValidatorService;
import ec.tusaas.efactura.storage.LocalComprobanteStorage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FacturaElectronicaServiceAuditoriaTest {

  @Mock private PuntoEmisionRepository puntoEmisionRepository;
  @Mock private CertificadoRepository certificadoRepository;
  @Mock private ComprobanteRepository comprobanteRepository;
  @Mock private ComprobanteDetalleRepository comprobanteDetalleRepository;
  @Mock private ComprobanteArchivoRepository comprobanteArchivoRepository;
  @Mock private ComprobanteLogSriRepository comprobanteLogSriRepository;
  @Mock private SecuencialService secuencialService;
  @Mock private XmlFacturaGeneratorService xmlFacturaGeneratorService;
  @Mock private XmlSignerService xmlSignerService;
  @Mock private SriRecepcionClient sriRecepcionClient;
  @Mock private SriAutorizacionClient sriAutorizacionClient;
  @Mock private LocalComprobanteStorage localComprobanteStorage;
  @Mock private XmlXsdValidatorService xmlXsdValidatorService;
  @Mock private ComprobanteRideService comprobanteRideService;
  @Mock private ComprobanteSriProcesoService comprobanteSriProcesoService;
  @Mock private ComprobanteNotificacionService comprobanteNotificacionService;
  @Mock private ApiKeyRepository apiKeyRepository;
  @Mock private AuditoriaService auditoriaService;
  @Mock private DashboardCacheService dashboardCacheService;
  @Mock private VendedorRepository vendedorRepository;

  private FacturaElectronicaService service;

  @BeforeEach
  void setUp() {
    service =
        new FacturaElectronicaService(
            puntoEmisionRepository,
            certificadoRepository,
            comprobanteRepository,
            comprobanteDetalleRepository,
            comprobanteArchivoRepository,
            comprobanteLogSriRepository,
            secuencialService,
            xmlFacturaGeneratorService,
            xmlSignerService,
            sriRecepcionClient,
            sriAutorizacionClient,
            localComprobanteStorage,
            xmlXsdValidatorService,
            comprobanteRideService,
            comprobanteSriProcesoService,
            comprobanteNotificacionService,
            apiKeyRepository,
            auditoriaService,
            dashboardCacheService,
            vendedorRepository);
  }

  @Test
  void registrarAuditoriaEmision_registraActorEmailCuandoEsUsuarioWeb() {
    UUID empresaId = UUID.randomUUID();
    UUID comprobanteId = UUID.randomUUID();
    Comprobante c = new Comprobante();
    c.setId(comprobanteId);
    c.setClaveAcceso("clave");
    c.setEstadoSri("AUTORIZADO");
    c.setEstablecimientoCodigo("001");
    c.setPuntoEmisionCodigo("002");
    c.setSecuencial("000000001");
    c.setValorTotal(new BigDecimal("12.34"));
    c.setOrigen("WEB");
    UsuarioPrincipal principal =
        UsuarioPrincipal.authenticated(
            UUID.randomUUID(), empresaId, "user@test.com", List.of("FACTURA_EMITIR"));

    ReflectionTestUtils.invokeMethod(service, "registrarAuditoriaEmision", empresaId, c, principal);

    verify(auditoriaService)
        .registrar(
            eq("FACTURA_EMITIDA"),
            eq(empresaId),
            eq("user@test.com"),
            eq("Comprobante"),
            eq(comprobanteId),
            argThat(
                (Map<String, Object> m) ->
                    "clave".equals(m.get("claveAcceso"))
                        && "AUTORIZADO".equals(m.get("estadoSri"))
                        && "001-002-000000001".equals(m.get("secuencial"))
                        && "12.34".equals(m.get("valorTotal"))
                        && "WEB".equals(m.get("origen"))));
  }

  @Test
  void registrarAuditoriaEmision_registraActorApiKeyCuandoSesionPorClave() {
    UUID empresaId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    UUID comprobanteId = UUID.randomUUID();
    Comprobante c = new Comprobante();
    c.setId(comprobanteId);
    c.setClaveAcceso("k");
    c.setEstadoSri("RECIBIDA");
    c.setEstablecimientoCodigo("1");
    c.setPuntoEmisionCodigo("2");
    c.setSecuencial("3");
    c.setValorTotal(BigDecimal.ONE);
    c.setOrigen("API");
    UsuarioPrincipal principal =
        UsuarioPrincipal.authenticatedWithApiKey(
            apiKeyId, empresaId, "mi-label", List.of("FACTURA_EMITIR"), 30);

    ReflectionTestUtils.invokeMethod(service, "registrarAuditoriaEmision", empresaId, c, principal);

    verify(auditoriaService)
        .registrar(
            eq("FACTURA_EMITIDA"),
            eq(empresaId),
            eq("apiKey:" + apiKeyId),
            eq("Comprobante"),
            eq(comprobanteId),
            any());
  }
}
