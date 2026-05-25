package ec.tusaas.efactura.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.tusaas.efactura.config.props.JwtProperties;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  @Test
  void creaYparseaToken() {
    JwtProperties p = new JwtProperties();
    p.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    p.setAccessTokenMinutes(5);
    JwtService jwt = new JwtService(p);
    UUID uid = UUID.randomUUID();
    UUID eid = UUID.randomUUID();
    String token = jwt.createAccessToken(uid, eid, "u@test.com", List.of("EMPRESA_ADMIN"));
    UsuarioPrincipal principal = jwt.parseAccessToken(token);
    assertThat(principal.getId()).isEqualTo(uid);
    assertThat(principal.getEmpresaId()).isEqualTo(eid);
    assertThat(principal.getEmail()).isEqualTo("u@test.com");
    assertThat(principal.getAuthorities()).map(a -> a.getAuthority()).containsExactly("EMPRESA_ADMIN");
  }

  @Test
  void ticketSeleccionEmpresaParseaSubject() {
    JwtProperties p = new JwtProperties();
    p.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    p.setEmpresaSelectorMinutes(5);
    JwtService jwt = new JwtService(p);
    UUID identidad = UUID.randomUUID();
    String ticket = jwt.createSelectEmpresaTicket(identidad);
    assertThat(jwt.parseSelectEmpresaTicket(ticket)).isEqualTo(identidad);
  }

  @Test
  void accessTokenParserRechazaTicketDeSeleccion() {
    JwtProperties p = new JwtProperties();
    p.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    p.setEmpresaSelectorMinutes(5);
    JwtService jwt = new JwtService(p);
    String ticket = jwt.createSelectEmpresaTicket(UUID.randomUUID());
    assertThatThrownBy(() -> jwt.parseAccessToken(ticket)).isInstanceOf(JwtException.class);
  }
}
