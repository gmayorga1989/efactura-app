package ec.tusaas.efactura.mapper;

import ec.tusaas.efactura.dto.empresa.EmpresaResponse;
import ec.tusaas.efactura.entity.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmpresaMapper {

  EmpresaResponse toResponse(Empresa empresa);
}
