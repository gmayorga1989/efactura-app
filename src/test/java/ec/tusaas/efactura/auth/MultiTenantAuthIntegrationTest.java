package ec.tusaas.efactura.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import ec.tusaas.efactura.dto.auth.LoginRequest;
import ec.tusaas.efactura.dto.auth.RefreshRequest;
import ec.tusaas.efactura.dto.auth.SelectEmpresaRequest;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.Permiso;
import ec.tusaas.efactura.entity.Rol;
import ec.tusaas.efactura.repository.AuditoriaRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.MembresiaEmpresaRepository;
import ec.tusaas.efactura.repository.MembresiaInvitacionRepository;
import ec.tusaas.efactura.repository.PermisoRepository;
import ec.tusaas.efactura.repository.RefreshTokenRepository;
import ec.tusaas.efactura.repository.RolRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class MultiTenantAuthIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("ef_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void dataSourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("efactura.bootstrap.enabled", () -> "false");
    registry.add(
        "efactura.jwt.secret",
        () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
  }

  @Autowired private TestRestTemplate rest;
  @Autowired private EmpresaRepository empresaRepository;
  @Autowired private RolRepository rolRepository;
  @Autowired private PermisoRepository permisoRepository;
  @Autowired private IdentidadRepository identidadRepository;
  @Autowired private MembresiaEmpresaRepository membresiaEmpresaRepository;
  @Autowired private MembresiaInvitacionRepository membresiaInvitacionRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuditoriaRepository auditoriaRepository;

  private UUID empresaAId;
  private UUID empresaBId;

  @BeforeEach
  void limpiarYsembrar() {
    auditoriaRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    membresiaInvitacionRepository.deleteAll();
    membresiaEmpresaRepository.deleteAll();
    identidadRepository.deleteAll();
    rolRepository.deleteAll();
    empresaRepository.deleteAll();

    Permiso adminPerm =
        permisoRepository.findByCodigo("EMPRESA_ADMIN").orElseThrow();

    Empresa a = new Empresa();
    a.setRuc("1790012345001");
    a.setRazonSocial("Empresa A");
    empresaRepository.save(a);
    empresaAId = a.getId();

    Empresa b = new Empresa();
    b.setRuc("1790012345002");
    b.setRazonSocial("Empresa B");
    empresaRepository.save(b);
    empresaBId = b.getId();

    Rol rolA = new Rol();
    rolA.setEmpresa(a);
    rolA.setCodigo("ADMIN");
    rolA.setNombre("Admin A");
    rolA.setSistema(true);
    rolA.getPermisos().add(adminPerm);
    rolRepository.save(rolA);

    Rol rolB = new Rol();
    rolB.setEmpresa(b);
    rolB.setCodigo("ADMIN");
    rolB.setNombre("Admin B");
    rolB.setSistema(true);
    rolB.getPermisos().add(adminPerm);
    rolRepository.save(rolB);

    Identidad identidad = new Identidad();
    identidad.setEmail("multi@test.local");
    identidad.setPasswordHash(passwordEncoder.encode("Secret123!"));
    identidad.setNombre("Usuario multi");
    identidadRepository.save(identidad);

    MembresiaEmpresa ma = new MembresiaEmpresa();
    ma.setIdentidad(identidad);
    ma.setEmpresa(a);
    ma.setEstado("ACTIVO");
    ma.getRoles().add(rolA);
    membresiaEmpresaRepository.save(ma);

    MembresiaEmpresa mb = new MembresiaEmpresa();
    mb.setIdentidad(identidad);
    mb.setEmpresa(b);
    mb.setEstado("ACTIVO");
    mb.getRoles().add(rolB);
    membresiaEmpresaRepository.save(mb);
  }

  @Test
  void loginDosEmpresasSelectRefreshYIdorEmpresa() {
    HttpHeaders json = new HttpHeaders();
    json.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<JsonNode> login =
        rest.exchange(
            "/api/web/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(new LoginRequest("multi@test.local", "Secret123!", null), json),
            JsonNode.class);

    assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(login.getBody().path("loginStep").asText()).isEqualTo("SELECT_EMPRESA");
    String ticket = login.getBody().path("sessionTicket").asText();
    assertThat(ticket).isNotBlank();

    ResponseEntity<JsonNode> picked =
        rest.exchange(
            "/api/web/v1/auth/select-empresa",
            HttpMethod.POST,
            new HttpEntity<>(new SelectEmpresaRequest(ticket, empresaAId), json),
            JsonNode.class);

    assertThat(picked.getStatusCode()).isEqualTo(HttpStatus.OK);
    String access = picked.getBody().path("tokens").path("accessToken").asText();
    String refresh = picked.getBody().path("tokens").path("refreshToken").asText();
    assertThat(access).isNotBlank();
    assertThat(refresh).isNotBlank();

    HttpHeaders bearer = new HttpHeaders();
    bearer.setBearerAuth(access);
    ResponseEntity<JsonNode> forb =
        rest.exchange(
            "/api/web/v1/empresas/" + empresaBId,
            HttpMethod.GET,
            new HttpEntity<>(bearer),
            JsonNode.class);
    assertThat(forb.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<JsonNode> okSelf =
        rest.exchange(
            "/api/web/v1/empresas/" + empresaAId,
            HttpMethod.GET,
            new HttpEntity<>(bearer),
            JsonNode.class);
    assertThat(okSelf.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<JsonNode> refreshed =
        rest.exchange(
            "/api/web/v1/auth/refresh",
            HttpMethod.POST,
            new HttpEntity<>(new RefreshRequest(refresh), json),
            JsonNode.class);
    assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshed.getBody().path("accessToken").asText()).isNotBlank();
  }
}
