package com.corocorabenz.engine.connector;

import static org.assertj.core.api.Assertions.assertThat;

import com.corocorabenz.engine.PruebaDelMotorSinWeb;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaConnector;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaEntry;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaInputParameter;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/** Sección 2 del contrato: todo HTTP Connector de los BPMN desplegados envía la credencial interna. */
@PruebaDelMotorSinWeb
class ConectoresConCredencialTest {

  private static final int CONECTORES_ESPERADOS = 9;

  /** El mismo mapa de encabezados que usan los BPMN, sobre una tarea que no llama a nadie. */
  private static final String PROCESO_CON_MAPA_DE_ENCABEZADOS = """
      <?xml version="1.0" encoding="UTF-8"?>
      <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
          xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
          id="Definitions_pruebaCredencialInterna" targetNamespace="http://bpmn.io/schema/bpmn">
        <bpmn:process id="pruebaCredencialInterna" isExecutable="true" camunda:historyTimeToLive="1">
          <bpmn:startEvent id="inicio">
            <bpmn:outgoing>Flow_01</bpmn:outgoing>
          </bpmn:startEvent>
          <bpmn:userTask id="esperar">
            <bpmn:extensionElements>
              <camunda:inputOutput>
                <camunda:inputParameter name="headers">
                  <camunda:map>
                    <camunda:entry key="Content-Type">application/json</camunda:entry>
                    <camunda:entry key="X-CB-Internal">${sistemas.token}</camunda:entry>
                  </camunda:map>
                </camunda:inputParameter>
              </camunda:inputOutput>
            </bpmn:extensionElements>
            <bpmn:incoming>Flow_01</bpmn:incoming>
            <bpmn:outgoing>Flow_02</bpmn:outgoing>
          </bpmn:userTask>
          <bpmn:endEvent id="fin">
            <bpmn:incoming>Flow_02</bpmn:incoming>
          </bpmn:endEvent>
          <bpmn:sequenceFlow id="Flow_01" sourceRef="inicio" targetRef="esperar" />
          <bpmn:sequenceFlow id="Flow_02" sourceRef="esperar" targetRef="fin" />
        </bpmn:process>
      </bpmn:definitions>
      """;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  @Autowired
  private Sistemas sistemas;

  @Value("${corocora.internal-token}")
  private String tokenConfigurado;

  @Test
  void elBeanSistemasExponeLaCredencialConfigurada() {
    assertThat(sistemas.getToken()).isEqualTo(tokenConfigurado);
    assertThat(tokenConfigurado).isNotBlank();
  }

  @Test
  void todosLosConectoresEnvianElEncabezadoDeLaCredencial() {
    List<Map<String, String>> encabezadosPorConector = new ArrayList<>();
    for (ProcessDefinition definicion : ultimasDefiniciones()) {
      BpmnModelInstance modelo = repositoryService.getBpmnModelInstance(definicion.getId());
      for (CamundaConnector conector : modelo.getModelElementsByType(CamundaConnector.class)) {
        assertThat(conector.getCamundaConnectorId().getTextContent()).isEqualTo("http-connector");
        encabezadosPorConector.add(encabezados(conector));
      }
    }

    assertThat(encabezadosPorConector).hasSize(CONECTORES_ESPERADOS);
    assertThat(encabezadosPorConector)
        .allSatisfy(encabezados -> assertThat(encabezados).containsEntry("X-CB-Internal", "${sistemas.token}"));
  }

  @Test
  void elMapaDeEncabezadosLlevaElValorDeLaCredencial() {
    String despliegue = repositoryService.createDeployment()
        .addString("pruebaCredencialInterna.bpmn", PROCESO_CON_MAPA_DE_ENCABEZADOS)
        .deploy()
        .getId();
    try {
      ProcessInstance instancia = runtimeService.startProcessInstanceByKey("pruebaCredencialInterna");
      Task tarea = taskService.createTaskQuery()
          .processInstanceId(instancia.getId())
          .singleResult();

      Object encabezados = taskService.getVariable(tarea.getId(), "headers");

      assertThat(encabezados).isInstanceOf(Map.class);
      assertThat(((Map<?, ?>) encabezados).get("X-CB-Internal")).isEqualTo(tokenConfigurado);
      assertThat(((Map<?, ?>) encabezados).get("Content-Type")).isEqualTo("application/json");
    } finally {
      repositoryService.deleteDeployment(despliegue, true);
    }
  }

  private List<ProcessDefinition> ultimasDefiniciones() {
    return repositoryService.createProcessDefinitionQuery()
        .latestVersion()
        .list();
  }

  private static Map<String, String> encabezados(CamundaConnector conector) {
    Map<String, String> encabezados = new LinkedHashMap<>();
    for (CamundaInputParameter parametro : conector.getCamundaInputOutput().getCamundaInputParameters()) {
      if (!"headers".equals(parametro.getCamundaName())) {
        continue;
      }
      CamundaMap mapa = parametro.getValue();
      for (CamundaEntry entrada : mapa.getCamundaEntries()) {
        encabezados.put(entrada.getCamundaKey(), entrada.getTextContent());
      }
    }
    return encabezados;
  }
}
