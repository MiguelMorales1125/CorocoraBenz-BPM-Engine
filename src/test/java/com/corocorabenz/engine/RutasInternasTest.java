package com.corocorabenz.engine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Secciones 2 y 6 del contrato vistas desde HTTP, con MockMvc (no abre ningún puerto).
 *
 * <p>Este contexto convive en la misma JVM con el de {@link PruebaDelMotorSinWeb}; por eso lleva su
 * propia base de datos y nombres únicos de motor y de aplicación de procesos.
 */
@SpringBootTest(properties = {
    "camunda.bpm.job-execution.enabled=false",
    "camunda.bpm.generate-unique-process-engine-name=true",
    "camunda.bpm.generate-unique-process-application-name=true",
    "spring.datasource.url=jdbc:h2:mem:corocora_camunda_web;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class RutasInternasTest {

  private static final String ENCABEZADO = "X-CB-Internal";

  @Autowired
  private MockMvc mvc;

  @Value("${corocora.internal-token}")
  private String token;

  @Test
  void laApiDemoRechazaLasPeticionesSinCredencial() throws Exception {
    mvc.perform(get("/api/demo/tasks"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Credencial interna requerida"));
  }

  @Test
  void laApiDemoRechazaUnaCredencialIncorrecta() throws Exception {
    mvc.perform(get("/api/demo/tasks").header(ENCABEZADO, token + "-incorrecta"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void laApiDemoAceptaLaCredencialEnSuEncabezado() throws Exception {
    mvc.perform(get("/api/demo/tasks").header(ENCABEZADO, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void laApiDemoAceptaLaCredencialComoBearer() throws Exception {
    mvc.perform(get("/api/demo/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void laApiDemoYaNoPublicaLasTareasExternasDeLosWorkers() throws Exception {
    // Ningún BPMN usa External Tasks desde que los workers se cambiaron por HTTP Connector.
    mvc.perform(get("/api/demo/external-tasks").header(ENCABEZADO, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void laRestDeCamundaQuedaDetrasDelFiltro() throws Exception {
    mvc.perform(get("/engine-rest/task"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Credencial interna requerida"));
  }

  @Test
  void elMotorYaNoRespondeAlPreflightDeCors() throws Exception {
    mvc.perform(options("/engine-rest/task")
            .header(HttpHeaders.ORIGIN, "http://localhost:9003")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void losErroresDeLaApiDemoSalenComoJsonDelContrato() throws Exception {
    mvc.perform(post("/api/demo/processes/nomina")
            .header(ENCABEZADO, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"variables\":{}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("businessKey es obligatorio"));
  }

  @Test
  void laDocumentacionOpenApiEsPublicaYDeclaraLaCredencial() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.credencialInterna.type").value("apiKey"))
        .andExpect(jsonPath("$.components.securitySchemes.credencialInterna.in").value("header"))
        .andExpect(jsonPath("$.components.securitySchemes.credencialInterna.name").value(ENCABEZADO))
        .andExpect(jsonPath("$.security[0].credencialInterna").isArray());
  }
}
