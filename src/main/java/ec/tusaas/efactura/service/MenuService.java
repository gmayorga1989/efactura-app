package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.menu.MenuItemResponse;
import ec.tusaas.efactura.entity.MenuItem;
import ec.tusaas.efactura.repository.MenuItemRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final MenuItemRepository menuItemRepository;

  @Transactional(readOnly = true)
  public List<MenuItemResponse> menuVisiblePara(UsuarioPrincipal principal) {
    List<MenuItem> items = menuItemRepository.findAllActivosOrdered("ACTIVO");
    Set<String> authorities =
        principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    boolean adminPlataforma =
        principal.getEmpresaId() == null && authorities.contains("PLATFORM_ADMIN");

    Map<String, MutableMenuItem> visibles = new LinkedHashMap<>();
    for (MenuItem item : items) {
      if (visibleParaSesion(item, authorities, adminPlataforma)) {
        agregarConPadres(item, visibles);
      }
    }

    visibles.values().forEach(n -> {
      if (n.item.getPadre() != null) {
        MutableMenuItem padre = visibles.get(n.item.getPadre().getCodigo());
        if (padre != null) {
          padre.hijos.add(n);
        }
      }
    });

    return visibles.values().stream()
        .filter(n -> n.item.getPadre() == null)
        .sorted(MutableMenuItem.BY_ORDER)
        .map(this::toDto)
        .toList();
  }

  private static boolean visibleParaSesion(
      MenuItem m, Set<String> authorities, boolean adminPlataforma) {
    if (adminPlataforma) {
      return true;
    }
    String req = m.getRequierePermisoCodigo();
    if (req == null || req.isBlank()) {
      return true;
    }
    for (String candidate : req.split("\\|")) {
      if (authorities.contains(candidate.trim())) {
        return true;
      }
    }
    return false;
  }

  private static void agregarConPadres(MenuItem item, Map<String, MutableMenuItem> visibles) {
    MenuItem actual = item;
    while (actual != null) {
      visibles.putIfAbsent(actual.getCodigo(), new MutableMenuItem(actual));
      actual = actual.getPadre();
    }
  }

  private MenuItemResponse toDto(MutableMenuItem node) {
    MenuItem m = node.item;
    String padreCodigo = m.getPadre() == null ? null : m.getPadre().getCodigo();
    return new MenuItemResponse(
        m.getId(),
        m.getCodigo(),
        padreCodigo,
        m.getOrden(),
        m.getEtiqueta(),
        m.getLabelKey(),
        m.getEtiqueta(),
        m.getRutaFront(),
        m.getIcono(),
        m.getModulo(),
        node.hijos.stream().sorted(MutableMenuItem.BY_ORDER).map(this::toDto).toList());
  }

  private static final class MutableMenuItem {
    private static final Comparator<MutableMenuItem> BY_ORDER =
        Comparator.comparingInt((MutableMenuItem n) -> n.item.getOrden()).thenComparing(n -> n.item.getEtiqueta());

    private final MenuItem item;
    private final List<MutableMenuItem> hijos = new ArrayList<>();

    private MutableMenuItem(MenuItem item) {
      this.item = item;
    }
  }
}
