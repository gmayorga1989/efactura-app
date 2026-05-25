package ec.tusaas.efactura.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

  @GetMapping("/version")
  public Map<String, String> version() {
    return Map.of("name", "efactura-app", "version", "0.1.0-SNAPSHOT");
  }
}
