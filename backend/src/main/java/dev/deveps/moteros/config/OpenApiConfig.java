package dev.deveps.moteros.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de la documentacion OpenAPI (Swagger UI en {@code /swagger-ui.html}).
 * Define el esquema de seguridad Bearer JWT para poder autenticar las llamadas desde la UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearer-jwt";

    @Bean
    public OpenAPI moterosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API moter@s")
                        .version("v1")
                        .description("Red social de moteros y sus rutas: usuarios, motos, rutas con "
                                + "geolocalizacion, quedadas, muro social, amistades, chat privado y notificaciones.")
                        .contact(new Contact().name("moter@s")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .name(ESQUEMA_BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /api/auth/login o /api/auth/registro")));
    }
}
