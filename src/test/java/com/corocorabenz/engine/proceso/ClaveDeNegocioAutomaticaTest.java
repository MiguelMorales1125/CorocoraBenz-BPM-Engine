package com.corocorabenz.engine.proceso;

import static org.assertj.core.api.Assertions.assertThat;

import com.corocorabenz.engine.PruebaDelMotorSinWeb;
import java.util.List;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Sección 6 del contrato: ninguna instancia raíz se queda sin businessKey. */
@PruebaDelMotorSinWeb
class ClaveDeNegocioAutomaticaTest {

  private static final String SUFIJO = "-[A-Z0-9]{8}";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private HistoryService historyService;

  @Test
  void nominaSinClaveRecibeUnaConPrefijoNom() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionNomina");

    String clave = instancia.getBusinessKey();
    assertThat(clave).matches("NOM" + SUFIJO);
    assertThat(claveGuardada(instancia.getId())).isEqualTo(clave);
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo(clave);
  }

  @Test
  void laInstanciaSeEncuentraPorSuClaveAutomatica() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionNomina");
    String clave = instancia.getBusinessKey();

    long activas = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey(clave)
        .processDefinitionKey("CB_GestionNomina")
        .count();
    long enHistorial = historyService.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(clave)
        .processDefinitionKey("CB_GestionNomina")
        .count();

    assertThat(activas).isEqualTo(1);
    assertThat(enHistorial).isEqualTo(1);
  }

  @Test
  void lasInstanciasHijasHeredanLaClaveAutomatica() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionNomina");

    List<ProcessInstance> hijas = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(instancia.getId())
        .list();

    // Nómina abre una validación (call activity) por cada novedad del periodo.
    assertThat(hijas).isNotEmpty();
    assertThat(hijas).allSatisfy(hija -> assertThat(hija.getBusinessKey()).isEqualTo(instancia.getBusinessKey()));
  }

  @Test
  void conservaLaClaveQueTraeLaInstancia() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionNomina", "NOM-2026-11-ABC123");

    assertThat(instancia.getBusinessKey()).isEqualTo("NOM-2026-11-ABC123");
    assertThat(claveGuardada(instancia.getId())).isEqualTo("NOM-2026-11-ABC123");
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo("NOM-2026-11-ABC123");
  }

  @Test
  void unaClaveEnBlancoCuentaComoAusente() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionNomina", "  ");

    assertThat(instancia.getBusinessKey()).matches("NOM" + SUFIJO);
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo(instancia.getBusinessKey());
  }

  @Test
  void incidenteDeTiRecibePrefijoTi() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_GestionIncidenteTI");

    assertThat(instancia.getBusinessKey()).matches("TI" + SUFIJO);
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo(instancia.getBusinessKey());
  }

  @Test
  void postventaIniciadaPorMensajeRecibePrefijoPv() {
    ProcessInstance instancia = runtimeService.startProcessInstanceByMessage("SolicitudServicio");

    assertThat(instancia.getBusinessKey()).matches("PV" + SUFIJO);
    assertThat(claveGuardada(instancia.getId())).isEqualTo(instancia.getBusinessKey());
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo(instancia.getBusinessKey());
  }

  @Test
  void cualquierOtroProcesoRecibePrefijoGenerico() {
    // Un proceso reutilizable arrancado directamente es una instancia raíz como cualquier otra.
    ProcessInstance instancia = runtimeService.startProcessInstanceByKey("CB_ValidarNovedadEmpleado");

    assertThat(instancia.getBusinessKey()).matches("PROC" + SUFIJO);
    assertThat(claveEnHistorial(instancia.getId())).isEqualTo(instancia.getBusinessKey());
  }

  @Test
  void dosInstanciasNoCompartenClave() {
    ProcessInstance primera = runtimeService.startProcessInstanceByKey("CB_GestionIncidenteTI");
    ProcessInstance segunda = runtimeService.startProcessInstanceByKey("CB_GestionIncidenteTI");

    assertThat(primera.getBusinessKey()).isNotEqualTo(segunda.getBusinessKey());
  }

  private String claveGuardada(String idInstancia) {
    ProcessInstance guardada = runtimeService.createProcessInstanceQuery()
        .processInstanceId(idInstancia)
        .singleResult();
    assertThat(guardada).as("la instancia sigue activa").isNotNull();
    return guardada.getBusinessKey();
  }

  private String claveEnHistorial(String idInstancia) {
    HistoricProcessInstance historica = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(idInstancia)
        .singleResult();
    assertThat(historica).as("la instancia está en el historial").isNotNull();
    return historica.getBusinessKey();
  }
}
