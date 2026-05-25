package ec.tusaas.efactura.bootstrap;

import ec.tusaas.efactura.config.props.BootstrapProperties;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.Permiso;
import ec.tusaas.efactura.entity.Plan;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.entity.Suscripcion;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.PermisoRepository;
import ec.tusaas.efactura.repository.PlanRepository;
import ec.tusaas.efactura.repository.RolRepository;
import ec.tusaas.efactura.repository.SuscripcionRepository;
import ec.tusaas.efactura.service.TenantDatasourceConfigService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1000)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "efactura.bootstrap", name = "enabled", havingValue = "true")
public class SecurityDataBootstrap implements ApplicationRunner {

  private final IdentidadRepository identidadRepository;
  private final MembresiaEmpresaRepository membresiaEmpresaRepository;
  private final EmpresaRepository empresaRepository;
  private final PermisoRepository permisoRepository;
  private final RolRepository rolRepository;
  private final PlanRepository planRepository;
  private final SuscripcionRepository suscripcionRepository;
  private final TenantDatasourceConfigService tenantDatasourceConfigService;
  private final PasswordEncoder passwordEncoder;
  private final BootstrapProperties props;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (identidadRepository.count() > 0) {
      log.debug("Bootstrap omitido: ya existen identidades");
      return;
    }
    List<Permiso> todos = permisoRepository.findAll();

    Rol rolPlataforma = new Rol();
    rolPlataforma.setEmpresa(null);
    rolPlataforma.setCodigo("PLATFORM_ADMIN");
    rolPlataforma.setNombre("Administrador de plataforma");
    rolPlataforma.setSistema(true);
    rolPlataforma.getPermisos().addAll(todos);
    rolPlataforma = rolRepository.save(rolPlataforma);

    Identidad platformIdent = new Identidad();
    platformIdent.setEmail(props.getPlatformAdminEmail().trim().toLowerCase());
    platformIdent.setPasswordHash(passwordEncoder.encode(props.getPlatformAdminPassword()));
    platformIdent.setNombre("Administrador plataforma");
    platformIdent = identidadRepository.save(platformIdent);

    MembresiaEmpresa mPlatform = new MembresiaEmpresa();
    mPlatform.setIdentidad(platformIdent);
    mPlatform.setEmpresa(null);
    mPlatform.setEstado("ACTIVO");
    mPlatform.getRoles().add(rolPlataforma);
    membresiaEmpresaRepository.save(mPlatform);

    Empresa demo = new Empresa();
    demo.setRuc(props.getDemoRuc().trim());
    demo.setSlug("demo");
    demo.setRazonSocial("Empresa Demo S.A.");
    demo.setNombreComercial("Demo");
    demo.setObligadoContabilidad(true);
    demo = empresaRepository.save(demo);
    tenantDatasourceConfigService.asegurarShared(demo, "bootstrap");

    Plan starter =
        planRepository
            .findByCodigo("STARTER")
            .orElseThrow(() -> new IllegalStateException("Plan STARTER no encontrado"));
    Suscripcion suscripcion = new Suscripcion();
    suscripcion.setEmpresa(demo);
    suscripcion.setPlan(starter);
    suscripcion.setFechaInicio(LocalDate.now());
    suscripcion.setEstado("ACTIVA");
    suscripcionRepository.save(suscripcion);

    Rol rolAdmin = new Rol();
    rolAdmin.setEmpresa(demo);
    rolAdmin.setCodigo("ADMIN");
    rolAdmin.setNombre("Administrador empresa");
    rolAdmin.setSistema(true);
    final Rol rolAdminRef = rolAdmin;
    for (String codigo :
        List.of(
            "EMPRESA_ADMIN",
            "FACTURA_EMITIR",
            "COMPROBANTE_MONITOR",
            "REPORTE_VER",
            "PROVEEDOR_GESTIONAR",
            "VENTAS_GESTIONAR",
            "SUITE_APP_CARTERA",
            "SUITE_APP_POS")) {
      permisoRepository.findByCodigo(codigo).ifPresent(p -> rolAdminRef.getPermisos().add(p));
    }
    rolAdmin = rolRepository.save(rolAdmin);

    Identidad adminDemo = new Identidad();
    adminDemo.setEmail(props.getDemoAdminEmail().trim().toLowerCase());
    adminDemo.setPasswordHash(passwordEncoder.encode(props.getDemoAdminPassword()));
    adminDemo.setNombre("Administrador demo");
    adminDemo = identidadRepository.save(adminDemo);

    MembresiaEmpresa mDemo = new MembresiaEmpresa();
    mDemo.setIdentidad(adminDemo);
    mDemo.setEmpresa(demo);
    mDemo.setEstado("ACTIVO");
    mDemo.getRoles().add(rolAdmin);
    membresiaEmpresaRepository.save(mDemo);

    log.info(
        "Bootstrap: identidad plataforma {} y admin demo {} (RUC {})",
        platformIdent.getEmail(),
        adminDemo.getEmail(),
        demo.getRuc());
  }
}
