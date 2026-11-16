package com.corocorabenz.engine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Operación demo", description = "Consulta y avance manual de tareas del motor embebido")
public class DemoTaskController {

    private final TaskService taskService;
    private final ExternalTaskService externalTaskService;

    public DemoTaskController(TaskService taskService, ExternalTaskService externalTaskService) {
        this.taskService = taskService;
        this.externalTaskService = externalTaskService;
    }

    @GetMapping("/tasks")
    @Operation(summary = "Consultar tareas humanas activas", description = "Filtra por el processInstanceId devuelto al iniciar el proceso para ver solo sus tareas.")
    public List<Map<String, Object>> humanTasks(
            @Parameter(description = "Id técnico de la instancia devuelto por el endpoint de inicio.", example = "f302a110-0000-0000-0000-000000000000")
            @RequestParam(required = false) String processInstanceId) {
        var query = taskService.createTaskQuery().active();
        if (processInstanceId != null && !processInstanceId.isBlank()) {
            query.processInstanceId(processInstanceId);
        }
        return query.list().stream().map(DemoTaskController::humanTaskResponse).collect(Collectors.toList());
    }

    @PostMapping("/tasks/{taskId}/complete")
    @Operation(summary = "Completar una tarea humana", description = "Elige el ejemplo que corresponde al nombre de la tarea. Copia el id desde GET /api/demo/tasks.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CompleteTaskRequest.class),
                            examples = {
                                @ExampleObject(name = "TI: asignar a Nivel 1", value = SwaggerExamples.TRIAGE_TASK),
                                @ExampleObject(name = "TI: registrar diagnóstico", value = SwaggerExamples.DIAGNOSIS_TASK),
                                @ExampleObject(name = "TI: confirmar solución", value = SwaggerExamples.CONFIRMATION_TASK)
                            })))
    public Map<String, String> completeHumanTask(
            @PathVariable String taskId, @RequestBody CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        taskService.complete(taskId, request == null ? Map.of() : request.safeVariables());
        return Map.of("taskId", taskId, "status", "completed");
    }

    @GetMapping("/external-tasks")
    @Operation(summary = "Consultar tareas externas pendientes", description = "Con los cuatro workers activos normalmente se completan automáticamente.")
    public List<Map<String, Object>> externalTasks(
            @Parameter(description = "Topic exacto de BPMN, por ejemplo ti.crearTicket.", example = "ti.crearTicket")
            @RequestParam(required = false) String topic) {
        var query = externalTaskService.createExternalTaskQuery().notLocked();
        if (topic != null && !topic.isBlank()) {
            query.topicName(topic);
        }
        return query.list().stream().map(DemoTaskController::externalTaskResponse).collect(Collectors.toList());
    }

    @PostMapping("/external-tasks/{taskId}/lock")
    @Operation(summary = "Bloquear una tarea externa para simular un worker", description = "Úsalo solo si vas a completar manualmente una External Task.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ExternalTaskLockRequest.class),
                            examples = @ExampleObject(name = "Bloqueo de 60 segundos", value = SwaggerExamples.EXTERNAL_TASK_LOCK))))
    public Map<String, String> lockExternalTask(
            @PathVariable String taskId, @RequestBody ExternalTaskLockRequest request) {
        requireWorker(request);
        externalTaskService.lock(taskId, request.workerId(), request.lockDurationMs());
        return Map.of("taskId", taskId, "workerId", request.workerId(), "status", "locked");
    }

    @PostMapping("/external-tasks/{taskId}/complete")
    @Operation(summary = "Completar una tarea externa simulada", description = "El workerId debe coincidir con el que usaste al bloquear la tarea.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ExternalTaskCompletionRequest.class),
                            examples = @ExampleObject(name = "Respuesta del worker", value = SwaggerExamples.EXTERNAL_TASK_COMPLETE))))
    public Map<String, String> completeExternalTask(
            @PathVariable String taskId, @RequestBody ExternalTaskCompletionRequest request) {
        if (request == null || request.workerId() == null || request.workerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workerId es obligatorio");
        }
        externalTaskService.complete(taskId, request.workerId(), request.safeVariables());
        return Map.of("taskId", taskId, "workerId", request.workerId(), "status", "completed");
    }

    private static Map<String, Object> humanTaskResponse(Task task) {
        return Map.of(
                "id", task.getId(),
                "name", task.getName(),
                "taskDefinitionKey", task.getTaskDefinitionKey(),
                "processInstanceId", task.getProcessInstanceId(),
                "assignee", task.getAssignee() == null ? "" : task.getAssignee());
    }

    private static Map<String, Object> externalTaskResponse(ExternalTask task) {
        return Map.of(
                "id", task.getId(),
                "topicName", task.getTopicName(),
                "activityId", task.getActivityId(),
                "processInstanceId", task.getProcessInstanceId());
    }

    private static void requireWorker(ExternalTaskLockRequest request) {
        if (request == null || request.workerId() == null || request.workerId().isBlank()
                || request.lockDurationMs() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "workerId y lockDurationMs positivo son obligatorios");
        }
    }
}
