package com.corocorabenz.engine.proceso;

import static org.assertj.core.api.Assertions.assertThat;

import com.corocorabenz.engine.PruebaDelMotorSinWeb;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Los BPMN y formularios tal como quedan desplegados: qué ofrece Tasklist y con qué datos arranca. */
@PruebaDelMotorSinWeb
class ModelosDesplegadosTest {

  private final ObjectMapper json = new ObjectMapper();

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  @Autowired
  private FormService formService;

  @Autowired
  private ManagementService managementService;

  @Test
  void tasklistSoloOfreceLosTresProcesosPrincipales() {
    List<String> iniciables = repositoryService.createProcessDefinitionQuery()
        .latestVersion()
        .startableInTasklist()
        .list()
        .stream()
        .map(ProcessDefinition::getKey)
        .toList();

    assertThat(iniciables)
        .contains("CB_GestionIncidenteTI", "CB_GestionNomina", "CB_PostventaGarantias")
        .doesNotContain("CB_ResolverIncidenteTI", "CB_AtenderOrdenTaller", "CB_ValidarNovedadEmpleado");
  }

  @Test
  void postventaArrancaDesdeSuFormularioDeInicioConLosValoresPorDefecto() throws IOException {
    String definicion = ultimaDefinicion("CB_PostventaGarantias");
    assertThat(formService.getStartFormData(definicion).getCamundaFormRef().getKey())
        .isEqualTo("postventa-solicitud");

    Map<String, Object> valores = new LinkedHashMap<>();
    for (JsonNode campo : formularioDeInicio(definicion).get("components")) {
      assertThat(campo.path("disabled").asBoolean()).as("campo editable: %s", campo.get("key")).isFalse();
      assertThat(campo.has("defaultValue")).as("valor por defecto: %s", campo.get("key")).isTrue();
      valores.put(campo.get("key").asText(), json.convertValue(campo.get("defaultValue"), Object.class));
    }

    ProcessInstance instancia = formService.submitStartForm(definicion, valores);

    assertThat(instancia.getBusinessKey()).matches("PV-[A-Z0-9]{8}");
    assertThat(tareaDe(instancia).getTaskDefinitionKey()).isEqualTo("registerRequest");
    // El conector vehicleLookup arma su URL con estas dos variables.
    assertThat(runtimeService.getVariable(instancia.getId(), "vin")).isEqualTo("CB0TEST0000000001");
    assertThat(runtimeService.getVariable(instancia.getId(), "mileageKm")).isEqualTo(15000);
  }

  @Test
  void losNumerosQueDebenSerEnterosNoAceptanDecimalesEnElFormularioDeInicio() throws IOException {
    JsonNode kilometraje = campo(formularioDeInicio(ultimaDefinicion("CB_PostventaGarantias")), "mileageKm");
    JsonNode equipos = campo(formularioDeInicio(ultimaDefinicion("CB_GestionIncidenteTI")), "affectedTeams");

    assertThat(kilometraje.path("decimalDigits").asInt(-1)).isZero();
    // El DMN de clasificación declara affectedTeams como integer: con 1.5 el proceso queda en incidente.
    assertThat(equipos.path("decimalDigits").asInt(-1)).isZero();
  }

  @Test
  void elFormularioDeInicioDePostventaLimitaLosTextosQueTallerBodegaRechaza() throws IOException {
    JsonNode formulario = formularioDeInicio(ultimaDefinicion("CB_PostventaGarantias"));

    // Taller-Bodega responde 400 a una cotización con más de 255 caracteres y el proceso queda en incidente.
    assertThat(campo(formulario, "customerName").path("validate").path("maxLength").asInt(-1)).isEqualTo(255);
    assertThat(campo(formulario, "symptom").path("validate").path("maxLength").asInt(-1)).isEqualTo(255);
  }

  @Test
  void quienRehaceElTrabajoVeElMotivoDeLaDevolucionSinDatoDeEjemplo() throws IOException {
    JsonNode liquidacion = formularioDeLaTarea(iniciarEn("CB_GestionNomina", "reviewPayroll"));
    JsonNode reparacion = formularioDeLaTarea(iniciarEn("CB_AtenderOrdenTaller", "repairVehicle"));
    JsonNode diagnostico = formularioDeLaTarea(resolucionTrasUnIntento(null, null));

    assertSoloLecturaSinValorPorDefecto(campo(liquidacion, "approvalReason"));
    assertSoloLecturaSinValorPorDefecto(campo(reparacion, "inspectionResults"));
    assertSoloLecturaSinValorPorDefecto(campo(diagnostico, "previousRootCause"));
    assertSoloLecturaSinValorPorDefecto(campo(diagnostico, "previousResolution"));
  }

  @Test
  void laPrimeraResolucionDelIncidenteConservaLosValoresDeEjemploEditables() throws IOException {
    ProcessInstance resolucion = resolucionTrasUnIntento(null, null);

    // Un null en rootCause taparía el defaultValue del campo obligatorio: por eso no se mapea con ese nombre.
    Map<String, Object> variables = runtimeService.getVariables(resolucion.getId());
    assertThat(variables).doesNotContainKeys("rootCause", "resolutionSummary");
    assertThat(variables.get("previousRootCause")).isNull();
    assertThat(variables.get("previousResolution")).isNull();
    JsonNode diagnostico = formularioDeLaTarea(resolucion);
    assertThat(campo(diagnostico, "rootCause").has("defaultValue")).isTrue();
    assertThat(campo(diagnostico, "resolutionSummary").has("defaultValue")).isTrue();
  }

  @Test
  void laSegundaResolucionDelIncidenteRecibeLaSolucionRechazada() {
    ProcessInstance resolucion = resolucionTrasUnIntento("Causa del primer intento", "Solución del primer intento");

    Map<String, Object> variables = runtimeService.getVariables(resolucion.getId());
    assertThat(variables.get("previousRootCause")).isEqualTo("Causa del primer intento");
    assertThat(variables.get("previousResolution")).isEqualTo("Solución del primer intento");
    assertThat(variables).doesNotContainKeys("rootCause", "resolutionSummary");
  }

  @Test
  void siElBancoNoRespondeEnDosHorasElCodigoDeRespuestaLoDice() {
    ProcessInstance instancia = runtimeService.createProcessInstanceByKey("CB_GestionNomina")
        .startBeforeActivity("waitBank")
        .execute();
    Job temporizador = managementService.createJobQuery()
        .processInstanceId(instancia.getId())
        .timers()
        .singleResult();

    managementService.executeJob(temporizador.getId());

    assertThat(tareaDe(instancia).getTaskDefinitionKey()).isEqualTo("fixBankBatch");
    assertThat(runtimeService.getVariable(instancia.getId(), "bankResponseCode")).isEqualTo("SIN-RESPUESTA-2H");
  }

  private String ultimaDefinicion(String clave) {
    return repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(clave)
        .latestVersion()
        .singleResult()
        .getId();
  }

  private JsonNode formularioDeInicio(String definicion) throws IOException {
    try (InputStream formulario = formService.getDeployedStartForm(definicion)) {
      return json.readTree(formulario);
    }
  }

  private JsonNode formularioDeLaTarea(ProcessInstance instancia) throws IOException {
    try (InputStream formulario = formService.getDeployedTaskForm(tareaDe(instancia).getId())) {
      return json.readTree(formulario);
    }
  }

  private ProcessInstance iniciarEn(String proceso, String actividad) {
    return runtimeService.createProcessInstanceByKey(proceso)
        .startBeforeActivity(actividad)
        .execute();
  }

  /**
   * Entra a "Resolver incidente técnico" como lo hace el proceso de TI y devuelve la instancia hija.
   * La causa y la solución son las del intento anterior; null cuando es la primera vuelta.
   */
  private ProcessInstance resolucionTrasUnIntento(String causaAnterior, String solucionAnterior) {
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("ticketId", "TI-0001");
    variables.put("technicalDomain", "catalogoNivel1");
    variables.put("priority", "P3");
    variables.put("description", "La cámara de reversa no carga");
    variables.put("targetDuration", "PT8H");
    if (causaAnterior != null) {
      variables.put("rootCause", causaAnterior);
      variables.put("resolutionSummary", solucionAnterior);
    }
    ProcessInstance incidente = runtimeService.createProcessInstanceByKey("CB_GestionIncidenteTI")
        .startBeforeActivity("resolveIncident")
        .setVariables(variables)
        .execute();
    return runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(incidente.getId())
        .singleResult();
  }

  private static void assertSoloLecturaSinValorPorDefecto(JsonNode campo) {
    assertThat(campo.path("disabled").asBoolean()).as("solo lectura: %s", campo.get("key")).isTrue();
    assertThat(campo.has("defaultValue")).as("sin dato de ejemplo: %s", campo.get("key")).isFalse();
  }

  private static JsonNode campo(JsonNode formulario, String clave) {
    for (JsonNode campo : formulario.get("components")) {
      if (clave.equals(campo.path("key").asText())) {
        return campo;
      }
    }
    throw new AssertionError("El formulario no tiene el campo " + clave);
  }

  private Task tareaDe(ProcessInstance instancia) {
    return taskService.createTaskQuery()
        .processInstanceId(instancia.getId())
        .singleResult();
  }
}
