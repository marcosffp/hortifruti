package com.hortifruti.sl.hortifruti.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SwaggerConfig {

  @SuppressWarnings("deprecation")
  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(
            new Info()
                .title("Hortifruti SL API")
                .version("1.0.0")
                .description("Documentação da API do sistema Hortifruti SL"))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addRequestBodies(
                    "fileUpload",
                    new RequestBody()
                        .content(
                            new Content()
                                .addMediaType(
                                    "multipart/form-data",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .type("object")
                                                .addProperties(
                                                    "file",
                                                    new Schema<>()
                                                        .type("string")
                                                        .format("binary")))))));
  }
}
