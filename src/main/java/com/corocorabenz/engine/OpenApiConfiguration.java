package com.corocorabenz.engine;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI corocoraOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Corocora Benz - API demo")
                .version("1.0.0")
                .description("API simple para ejecutar los procesos embebidos y simular eventos externos."));
    }
}
