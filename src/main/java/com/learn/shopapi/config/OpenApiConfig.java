package com.learn.shopapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cau hinh Swagger/OpenAPI.
 *
 * springdoc tu quet cac @RestController de sinh trang tai lieu tai /swagger-ui.html.
 * O day ta khai bao them "security scheme" kieu Bearer JWT de tren Swagger UI co nut
 * "Authorize": dan access token vao 1 lan, moi request sau tu them header Authorization.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shopApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("shop-api")
                        .version("1.0")
                        .description("API ban hang co phan quyen (JWT). Du an hoc Backend & Data."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
