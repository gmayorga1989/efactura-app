package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.permiso.PermisoCatalogoResponse;
import ec.tusaas.efactura.repository.PermisoRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class PermisoCatalogController {

  private final PermisoRepository permisoRepository;

  @GetMapping("/permisos-catalogo")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  @SecurityRequirement(name = "bearer-jwt")
  public List<PermisoCatalogoResponse> catalogo() {
    return permisoRepository.findAllByEstadoOrderByModuloAscCodigoAsc("ACTIVO").stream()
        .map(
            p ->
                new PermisoCatalogoResponse(
                    p.getId(), p.getCodigo(), p.getDescripcion(), p.getModulo()))
        .toList();
  }
}
