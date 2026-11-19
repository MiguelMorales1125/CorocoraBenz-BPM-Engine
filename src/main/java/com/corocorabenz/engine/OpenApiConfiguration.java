package com.corocorabenz.engine;

import com.corocorabenz.comun.credencial.CredencialInterna;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private static final String ESQUEMA_CREDENCIAL = "credencialInterna";

    @Bean
    OpenAPI corocoraOpenApi() {
        Info info = new Info()
                .title("Corocora Benz - API demo")
                .version("1.0.0")
                .description("API simple para ejecutar los procesos embebidos y simular eventos externos. "
                        + "Las rutas /api/demo/** exigen la credencial interna: pulsa \"Authorize\" y escribe "
                        + "el valor de corocora.internal-token; Swagger UI lo enviará en el encabezado "
                        + CredencialInterna.HEADER + ".");

        SecurityScheme credencialInterna = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(CredencialInterna.HEADER)
                .description("Credencial interna entre sistemas (propiedad corocora.internal-token).");

        // Requisito global: tras "Authorize", Swagger UI envía el encabezado en todas las operaciones.
        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(ESQUEMA_CREDENCIAL, credencialInterna))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_CREDENCIAL));
    }
}
