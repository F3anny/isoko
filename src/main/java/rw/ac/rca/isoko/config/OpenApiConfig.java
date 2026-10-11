package rw.ac.rca.isoko.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI cloudSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Isoko Ecommerce API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Isoko containerized ecommerce platform (SPEDO501 capstone).
                                Maps to SRS ISK-SRS-2026-001 (buyer, seller and admin workflows). Money is integer RWF.
                                Guest carts use header `X-Cart-Token`. Authenticated calls use Bearer JWT.
                                """)
                        .contact(new Contact().name("Year 3 SPE / Isoko").email("ops@isoko.rw"))
                        .license(new License().name("Academic / capstone")))
                .servers(List.of(new Server().url("/").description("Current host")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
