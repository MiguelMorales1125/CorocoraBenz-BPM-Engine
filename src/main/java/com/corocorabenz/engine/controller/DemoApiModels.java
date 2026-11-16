package com.corocorabenz.engine.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

record StartProcessRequest(
        @Schema(description = "Identificador legible de esta instancia; úsalo para reconocer el caso.", example = "TI-DEMO-001")
        String businessKey,
        @Schema(description = "Variables de inicio del proceso. Usa el ejemplo específico de cada endpoint.")
        Map<String, Object> variables) {
    Map<String, Object> safeVariables() {
        return variables == null ? Map.of() : variables;
    }
}

record CompleteTaskRequest(
        @Schema(description = "Variables capturadas en el formulario de la tarea que vas a completar.")
        Map<String, Object> variables) {
    Map<String, Object> safeVariables() {
        return variables == null ? Map.of() : variables;
    }
}

record ExternalTaskLockRequest(
        @Schema(description = "Nombre que identifica el worker simulado.", example = "demo-worker") String workerId,
        @Schema(description = "Duración del bloqueo en milisegundos.", example = "60000") long lockDurationMs) {
}

record ExternalTaskCompletionRequest(
        @Schema(description = "El mismo workerId usado para bloquear la tarea.", example = "demo-worker") String workerId,
        @Schema(description = "Variables que el worker entrega al completar la tarea.") Map<String, Object> variables) {
    Map<String, Object> safeVariables() {
        return variables == null ? Map.of() : variables;
    }
}

record MessageEventRequest(
        @Schema(description = "La misma referencia de negocio; también debe coincidir con la businessKey de la ruta.", example = "POST-DEMO-001")
        String businessKey,
        @Schema(description = "Variables opcionales para el mensaje. Puede enviarse un objeto vacío.")
        Map<String, Object> variables) {
    Map<String, Object> safeVariables() {
        return variables == null ? Map.of() : variables;
    }
}
