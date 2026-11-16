package com.corocorabenz.engine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo/events")
@Tag(name = "Eventos externos", description = "Respuestas simuladas de Cliente, Bodega y Banco")
public class DemoEventController {

    private final RuntimeService runtimeService;

    public DemoEventController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("/cotizacion-aceptada/{businessKey}")
    @Operation(summary = "Correlacionar la aceptación de una cotización", description = "Usa en la ruta la misma businessKey con la que iniciaste Postventa. El cuerpo es opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEventRequest.class),
                            examples = @ExampleObject(name = "Evento del cliente", value = SwaggerExamples.MESSAGE_EVENT))))
    public Map<String, Object> quoteAccepted(
            @PathVariable String businessKey, @RequestBody(required = false) MessageEventRequest request) {
        return correlate("CotizacionAceptada", businessKey, request);
    }

    @PostMapping("/cotizacion-rechazada/{businessKey}")
    @Operation(summary = "Correlacionar el rechazo de una cotización", description = "Usa la misma businessKey del caso Postventa. El cuerpo es opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEventRequest.class),
                            examples = @ExampleObject(name = "Evento del cliente", value = SwaggerExamples.MESSAGE_EVENT))))
    public Map<String, Object> quoteRejected(
            @PathVariable String businessKey, @RequestBody(required = false) MessageEventRequest request) {
        return correlate("CotizacionRechazada", businessKey, request);
    }

    @PostMapping("/repuesto-recibido/{businessKey}")
    @Operation(summary = "Correlacionar la llegada de un repuesto", description = "Usa la misma businessKey del caso Postventa. El cuerpo es opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEventRequest.class),
                            examples = @ExampleObject(name = "Confirmación de bodega", value = SwaggerExamples.MESSAGE_EVENT))))
    public Map<String, Object> partReceived(
            @PathVariable String businessKey, @RequestBody(required = false) MessageEventRequest request) {
        return correlate("RepuestoRecibido", businessKey, request);
    }

    @PostMapping("/dispersion-aceptada/{businessKey}")
    @Operation(summary = "Correlacionar la aceptación del Banco", description = "Usa la misma businessKey con la que iniciaste Nómina. El cuerpo es opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEventRequest.class),
                            examples = @ExampleObject(name = "Respuesta del banco", value = "{\"businessKey\":\"NOM-DEMO-001\",\"variables\":{}}"))))
    public Map<String, Object> paymentAccepted(
            @PathVariable String businessKey, @RequestBody(required = false) MessageEventRequest request) {
        return correlate("DispersionAceptada", businessKey, request);
    }

    @PostMapping("/dispersion-rechazada/{businessKey}")
    @Operation(summary = "Correlacionar el rechazo del Banco", description = "Usa la misma businessKey con la que iniciaste Nómina. El cuerpo es opcional.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEventRequest.class),
                            examples = @ExampleObject(name = "Respuesta del banco", value = "{\"businessKey\":\"NOM-DEMO-001\",\"variables\":{}}"))))
    public Map<String, Object> paymentRejected(
            @PathVariable String businessKey, @RequestBody(required = false) MessageEventRequest request) {
        return correlate("DispersionRechazada", businessKey, request);
    }

    private Map<String, Object> correlate(
            String messageName, String businessKey, MessageEventRequest request) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessKey es obligatorio");
        }
        MessageEventRequest safeRequest = request == null
                ? new MessageEventRequest(businessKey, Map.of()) : request;
        var result = runtimeService.createMessageCorrelation(messageName)
                .processInstanceBusinessKey(businessKey)
                .setVariables(safeRequest.safeVariables())
                .correlateWithResult();
        return Map.of(
                "messageName", messageName,
                "businessKey", businessKey,
                "processInstanceId", result.getProcessInstance() == null
                        ? "" : result.getProcessInstance().getId());
    }
}
