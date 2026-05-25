package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ProductoImportLineResult;
import ec.tusaas.efactura.dto.maestro.ProductoImportResult;
import ec.tusaas.efactura.dto.maestro.ProductoListaPrecioRequest;
import ec.tusaas.efactura.dto.maestro.ProductoRequest;
import ec.tusaas.efactura.entity.Producto;
import ec.tusaas.efactura.entity.ProductoCategoria;
import ec.tusaas.efactura.repository.ProductoCategoriaRepository;
import ec.tusaas.efactura.repository.ProductoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductoImportService {

  private static final int MAX_FILAS = 2000;
  private static final int MAX_DETALLES = 150;
  private static final char DELIM = ';';

  private static final String[] COLUMNAS =
      new String[] {
        "codigo_principal",
        "descripcion",
        "precio",
        "codigo_auxiliar",
        "codigo_barra",
        "iva_codigo",
        "ice_codigo",
        "irbpnr_codigo",
        "categoria_codigo"
      };

  private final ProductoService productoService;
  private final ProductoRepository productoRepository;
  private final ProductoCategoriaRepository productoCategoriaRepository;
  private final ListaPrecioService listaPrecioService;

  public byte[] generarPlantillaCsv(String tipoProducto) {
    String tipo = normalizarTipo(tipoProducto);
    String ejemplo =
        tipo.equals("SERVICIO")
            ? "SRV-EJEMPLO;Servicio de consultoria;120.00;SRV-01;;4;;;"
            : "PROD-EJEMPLO;Producto de ejemplo;10.50;AUX-01;7890123456789;4;;;";
    String header = String.join(String.valueOf(DELIM), COLUMNAS);
    String instrucciones =
        "# Plantilla de importacion (" + tipo + "). Separador: punto y coma (;). No modifique la fila de encabezados.";
    String contenido = "\uFEFF" + instrucciones + "\r\n" + header + "\r\n" + ejemplo + "\r\n";
    return contenido.getBytes(StandardCharsets.UTF_8);
  }

  @Transactional
  public ProductoImportResult importar(
      UUID empresaId, MultipartFile archivo, String tipoProducto, UsuarioPrincipal principal) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio");
    }
    String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
    if (!nombre.endsWith(".csv") && !nombre.endsWith(".txt")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se admiten archivos .csv o .txt");
    }
    listaPrecioService.asegurarListaBase(empresaId);
    String tipo = normalizarTipo(tipoProducto);
    Map<String, UUID> categoriasPorCodigo = indexarCategorias(empresaId);

    List<ProductoImportLineResult> detalles = new ArrayList<>();
    int creados = 0;
    int actualizados = 0;
    int errores = 0;
    int totalFilas = 0;

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      int lineNo = 0;
      Map<String, Integer> headerIndex = null;

      while ((line = reader.readLine()) != null) {
        lineNo++;
        String trimmed = stripBom(line).trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        List<String> cells = parseLine(trimmed);
        if (headerIndex == null) {
          headerIndex = mapHeader(cells);
          continue;
        }
        totalFilas++;
        if (totalFilas > MAX_FILAS) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Maximo " + MAX_FILAS + " filas de datos por archivo");
        }
        try {
          ProductoRequest req = toRequest(cells, headerIndex, tipo, categoriasPorCodigo);
          String codigo = req.codigoPrincipal();
          Optional<Producto> existente = productoRepository.findByEmpresa_IdAndCodigoPrincipal(empresaId, codigo);
          if (existente.isPresent()) {
            Producto p = existente.get();
            productoService.actualizar(empresaId, p.getId(), req, principal);
            actualizados++;
            addDetalle(detalles, lineNo, codigo, "ACTUALIZADO", "Registro actualizado");
          } else {
            productoService.crear(empresaId, req, principal);
            creados++;
            addDetalle(detalles, lineNo, codigo, "CREADO", "Registro creado");
          }
        } catch (Exception ex) {
          errores++;
          String codigo = cell(cells, headerIndex, "codigo_principal");
          addDetalle(detalles, lineNo, codigo, "ERROR", mensajeError(ex));
        }
      }
      if (headerIndex == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no contiene encabezados validos");
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
    }

    return new ProductoImportResult(totalFilas, creados, actualizados, errores, detalles);
  }

  private ProductoRequest toRequest(
      List<String> cells,
      Map<String, Integer> headerIndex,
      String tipo,
      Map<String, UUID> categoriasPorCodigo) {
    String codigo = requerido(cells, headerIndex, "codigo_principal", 50);
    String descripcion = requerido(cells, headerIndex, "descripcion", 300);
    BigDecimal precio = parsePrecio(requerido(cells, headerIndex, "precio", 20));

    String codigoAux = opcional(cells, headerIndex, "codigo_auxiliar", 50);
    String codigoBarra = opcional(cells, headerIndex, "codigo_barra", 80);
    String iva = opcional(cells, headerIndex, "iva_codigo", 4);
    String ice = opcional(cells, headerIndex, "ice_codigo", 10);
    String irbpnr = opcional(cells, headerIndex, "irbpnr_codigo", 10);
    String catCodigo = opcional(cells, headerIndex, "categoria_codigo", 50);

    UUID categoriaId = null;
    if (catCodigo != null && !catCodigo.isBlank()) {
      String key = catCodigo.trim().toUpperCase(Locale.ROOT);
      categoriaId =
          Optional.ofNullable(categoriasPorCodigo.get(key))
              .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + catCodigo));
    }

    Map<String, Object> custom = new HashMap<>();
    if (codigoBarra != null && !codigoBarra.isBlank()) {
      custom.put("codigoBarra", codigoBarra.trim());
    }

    return new ProductoRequest(
        codigo,
        blankToNull(codigoAux),
        descripcion,
        tipo,
        precio,
        blankToNull(iva),
        blankToNull(ice),
        blankToNull(irbpnr),
        categoriaId,
        custom.isEmpty() ? null : custom,
        List.of(new ProductoListaPrecioRequest("BASE", precio)),
        List.of(),
        List.of());
  }

  private Map<String, UUID> indexarCategorias(UUID empresaId) {
    Map<String, UUID> map = new HashMap<>();
    for (ProductoCategoria c :
        productoCategoriaRepository.findByEmpresa_IdAndEstadoOrderByOrdenAscNombreAsc(empresaId, "ACTIVO")) {
      if (c.getCodigo() != null && !c.getCodigo().isBlank()) {
        map.put(c.getCodigo().trim().toUpperCase(Locale.ROOT), c.getId());
      }
    }
    return map;
  }

  private static Map<String, Integer> mapHeader(List<String> cells) {
    Map<String, Integer> idx = new LinkedHashMap<>();
    for (int i = 0; i < cells.size(); i++) {
      String key = cells.get(i).trim().toLowerCase(Locale.ROOT).replace('\uFEFF', ' ');
      if (!key.isEmpty()) {
        idx.put(key, i);
      }
    }
    if (!idx.containsKey("codigo_principal") || !idx.containsKey("descripcion") || !idx.containsKey("precio")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Encabezados obligatorios: codigo_principal, descripcion, precio");
    }
    return idx;
  }

  private static String requerido(List<String> cells, Map<String, Integer> idx, String col, int maxLen) {
    String v = cell(cells, idx, col);
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException("Columna obligatoria vacia: " + col);
    }
    v = v.trim();
    if (v.length() > maxLen) {
      throw new IllegalArgumentException("Columna " + col + " supera " + maxLen + " caracteres");
    }
    if ("codigo_principal".equals(col)) {
      return v.toUpperCase(Locale.ROOT);
    }
    return v;
  }

  private static String opcional(List<String> cells, Map<String, Integer> idx, String col, int maxLen) {
    String v = cell(cells, idx, col);
    if (v == null || v.isBlank()) {
      return null;
    }
    v = v.trim();
    if (v.length() > maxLen) {
      throw new IllegalArgumentException("Columna " + col + " supera " + maxLen + " caracteres");
    }
    return v;
  }

  private static String cell(List<String> cells, Map<String, Integer> idx, String col) {
    Integer i = idx.get(col);
    if (i == null || i < 0 || i >= cells.size()) {
      return null;
    }
    return cells.get(i);
  }

  private static BigDecimal parsePrecio(String raw) {
    String n = raw.trim().replace(',', '.');
    try {
      BigDecimal v = new BigDecimal(n);
      if (v.signum() < 0) {
        throw new IllegalArgumentException("El precio no puede ser negativo");
      }
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Precio invalido: " + raw);
    }
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }

  private static String normalizarTipo(String tipo) {
    if (tipo != null && tipo.equalsIgnoreCase("SERVICIO")) {
      return "SERVICIO";
    }
    return "PRODUCTO";
  }

  private static String stripBom(String s) {
    if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
      return s.substring(1);
    }
    return s;
  }

  private static List<String> parseLine(String line) {
    List<String> out = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          cur.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == DELIM && !inQuotes) {
        out.add(cur.toString().trim());
        cur.setLength(0);
      } else {
        cur.append(c);
      }
    }
    out.add(cur.toString().trim());
    return out;
  }

  private static void addDetalle(
      List<ProductoImportLineResult> detalles, int fila, String codigo, String estado, String mensaje) {
    if (detalles.size() >= MAX_DETALLES) {
      return;
    }
    detalles.add(new ProductoImportLineResult(fila, codigo, estado, mensaje));
  }

  private static String mensajeError(Exception ex) {
    if (ex instanceof ResponseStatusException rse) {
      return rse.getReason() != null ? rse.getReason() : rse.getMessage();
    }
    return ex.getMessage() != null ? ex.getMessage() : "Error desconocido";
  }
}
