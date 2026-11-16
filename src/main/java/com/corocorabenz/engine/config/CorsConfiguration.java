package com.corocorabenz.engine.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Permite que las webs (portales y CentralSys) llamen a la REST API de Camunda desde el navegador.
 * La REST API corre en Jersey, fuera de Spring MVC, por eso se usa un filtro de servlet.
 */
@Configuration
public class CorsConfiguration {

  @Bean
  FilterRegistrationBean<CorsFilter> engineRestCorsFilter(@Value("${corocora.cors-origins}") String origins) {
    var cors = new org.springframework.web.cors.CorsConfiguration();
    cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("*"));

    // Spring calcula la ruta relativa al servlet de Jersey, por eso el patrón es /** y el filtro
    // solo se registra sobre /engine-rest/*.
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);

    var registration = new FilterRegistrationBean<>(new CorsFilter(source));
    registration.addUrlPatterns("/engine-rest/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
