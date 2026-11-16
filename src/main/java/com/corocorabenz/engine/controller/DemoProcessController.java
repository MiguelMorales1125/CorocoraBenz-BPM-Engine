package com.corocorabenz.engine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo/processes")
@Tag(name = "Procesos demo", description = "Inicio de los tres procesos principales de Corocora Benz")
public class DemoProcessController {

    private final RuntimeService runtimeService;

    public DemoProcessController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/postventa")
    @Operation(summary = "Iniciar una solicitud de postventa", description = "Usa businessKey como referencia legible del caso. El ejemplo muestra una garantía con recall activo.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StartProcessRequest.class),
                            examples = @ExampleObject(name = "Postventa con recall", value = SwaggerExamples.POSTVENTA_START))))
    public Map<String, String> startPostventa(@RequestBody StartProcessRequest request) {
        requireBusinessKey(request);
        MessageCorrelationResult result = runtimeService.createMessageCorrelation("SolicitudServicio")
                .processInstanceBusinessKey(request.businessKey())
                .setVariables(request.safeVariables())
                .correlateWithResult();
        if (result.getProcessInstance() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "La solicitud no inició una instancia de postventa");
        }
        return processResponse(result.getProcessInstance());
    }

    @PostMapping("/ti")
    @Operation(summary = "Iniciar un incidente de TI", description = "Valores válidos de impacto: Bajo, Medio o Alto; urgencia: Baja, Media o Alta. El ejemplo recorre la ruta P3 de Nivel 1.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StartProcessRequest.class),
                            examples = @ExampleObject(name = "Incidente TI P3", value = SwaggerExamples.TI_START))))
    public Map<String, String> startTi(@RequestBody StartProcessRequest request) {
        return startByKey("CB_GestionIncidenteTI", request);
    }

    @PostMapping("/nomina")
    @Operation(summary = "Iniciar un periodo de nómina", description = "El ejemplo inicia un periodo mensual de demostración.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StartProcessRequest.class),
                            examples = @ExampleObject(name = "Periodo mensual", value = SwaggerExamples.NOMINA_START))))
    public Map<String, String> startNomina(@RequestBody StartProcessRequest request) {
        return startByKey("CB_GestionNomina", request);
    }

    private Map<String, String> startByKey(String processDefinitionKey, StartProcessRequest request) {
        requireBusinessKey(request);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey, request.businessKey(), request.safeVariables());
        return processResponse(instance);
    }

    private static Map<String, String> processResponse(ProcessInstance instance) {
        return Map.of("processInstanceId", instance.getId(), "businessKey", instance.getBusinessKey());
    }

    private static void requireBusinessKey(StartProcessRequest request) {
        if (request == null || request.businessKey() == null || request.businessKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessKey es obligatorio");
        }
    }
}
