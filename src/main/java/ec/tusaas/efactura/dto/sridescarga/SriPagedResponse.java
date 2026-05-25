package ec.tusaas.efactura.dto.sridescarga;

import java.util.List;

public record SriPagedResponse<T>(
    List<T> content, long totalElements, int page, int size, int totalPages) {}
