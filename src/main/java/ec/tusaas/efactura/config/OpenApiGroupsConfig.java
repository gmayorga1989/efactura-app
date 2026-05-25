package ec.tusaas.efactura.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

  @Bean
  public OpenApiCustomizer efacturaSecuritySchemes() {
    return openApi -> {
      if (openApi.getComponents() == null) {
        openApi.setComponents(new Components());
      }
      openApi
          .getComponents()
          .addSecuritySchemes(
              "bearer-jwt",
              new SecurityScheme()
                  .type(SecurityScheme.Type.HTTP)
                  .scheme("bearer")
                  .bearerFormat("JWT")
                  .description("Access token. Subject = identidadId; claim empresaId = tenant."));
    };
  }

  @Bean
  public GroupedOpenApi webOpenApi() {
    return GroupedOpenApi.builder().group("web").pathsToMatch("/api/web/**").build();
  }

  @Bean
  public GroupedOpenApi publicOpenApi() {
    return GroupedOpenApi.builder().group("public").pathsToMatch("/api/public/**").build();
  }
}
