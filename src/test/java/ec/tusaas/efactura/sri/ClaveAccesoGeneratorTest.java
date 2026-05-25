package ec.tusaas.efactura.sri;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClaveAccesoGeneratorTest {

  @Test
  void genera49Digitos() {
    String clave =
        ClaveAccesoGenerator.generar(
            LocalDate.of(2026, 5, 11),
            "01",
            "1790012345001",
            1,
            1,
            "001",
            "001",
            1L,
            "12345678");
    assertThat(clave).hasSize(49).matches("\\d{49}");
  }

  @Test
  void estructuraCoincideConCamposXmlSri() {
    String aleatorio = "12861712";
    String clave =
        ClaveAccesoGenerator.generar(
            LocalDate.of(2026, 5, 19),
            "01",
            "1793230378001",
            1,
            1,
            "001",
            "001",
            11L,
            aleatorio);

    assertThat(clave.substring(0, 8)).isEqualTo("19052026");
    assertThat(clave.substring(8, 10)).isEqualTo("01");
    assertThat(clave.substring(10, 23)).isEqualTo("1793230378001");
    assertThat(clave.substring(23, 24)).isEqualTo("1");
    assertThat(clave.substring(24, 27)).isEqualTo("001");
    assertThat(clave.substring(27, 30)).isEqualTo("001");
    assertThat(clave.substring(30, 39)).isEqualTo("000000011");
    assertThat(clave.substring(39, 47)).isEqualTo(aleatorio);
    assertThat(clave.substring(47, 48)).isEqualTo("1");
    assertThat(clave.substring(48, 49)).matches("\\d");
  }
}
