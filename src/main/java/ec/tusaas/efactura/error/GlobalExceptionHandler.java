package ec.tusaas.efactura.error;

import java.net.URI;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ProblemDetail> badCredentials(BadCredentialsException ex) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    pd.setTitle("No autorizado");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> accessDenied(AccessDeniedException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    pd.setTitle("Prohibido");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getReason());
    pd.setTitle(status.getReasonPhrase());
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
    String detalle =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::formatFieldError)
            .collect(Collectors.joining("; "));
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    pd.setTitle("Validación");
    pd.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> illegalArgument(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setTitle("Petición inválida");
    return ResponseEntity.badRequest().body(pd);
  }

  @ExceptionHandler(DataAccessResourceFailureException.class)
  public ResponseEntity<ProblemDetail> dataAccessUnavailable(DataAccessResourceFailureException ex) {
    return dbUnavailableResponse();
  }

  @ExceptionHandler(JpaSystemException.class)
  public ResponseEntity<ProblemDetail> jpaSystem(JpaSystemException ex) {
    if (isTransientDbFailure(ex)) {
      return dbUnavailableResponse();
    }
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error de persistencia");
    pd.setTitle("Error interno");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  private static ResponseEntity<ProblemDetail> dbUnavailableResponse() {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Base de datos no disponible. Compruebe que PostgreSQL esté en ejecución (docker compose up -d postgres).");
    pd.setTitle("Servicio no disponible");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
  }

  private static boolean isTransientDbFailure(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql
          && ("57P01".equals(sql.getSQLState()) || "08006".equals(sql.getSQLState()))) {
        return true;
      }
      String msg = t.getMessage();
      if (msg != null
          && (msg.contains("terminating connection due to administrator command")
              || msg.contains("Connection is closed"))) {
        return true;
      }
    }
    return false;
  }

  private static String formatFieldError(FieldError fe) {
    return fe.getField() + ": " + fe.getDefaultMessage();
  }
}
