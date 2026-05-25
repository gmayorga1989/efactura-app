package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.menu.MenuItemResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.MenuService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class MenuController {

  private final MenuService menuService;

  /** Ítems de menú activos filtrados por permisos del JWT actual. */
  @GetMapping("/menu")
  @SecurityRequirement(name = "bearer-jwt")
  public List<MenuItemResponse> menu(@AuthenticationPrincipal UsuarioPrincipal principal) {
    return menuService.menuVisiblePara(principal);
  }
}
