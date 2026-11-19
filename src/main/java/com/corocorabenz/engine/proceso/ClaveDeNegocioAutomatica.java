package com.corocorabenz.engine.proceso;

import java.security.SecureRandom;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Da una businessKey a toda instancia raíz que arranque sin ella, por ejemplo desde Tasklist
 * (sección 6 del contrato). Banco, Taller-Bodega y los portales responden al motor por businessKey,
 * así que una instancia sin clave se quedaría esperando para siempre.
 *
 * <p>Escucha los eventos de ejecución que publica camunda-bpm-spring-boot
 * ({@code camunda.bpm.eventing.execution}). El listener corre dentro del mismo comando que arranca
 * la instancia, por eso la clave queda en la instancia y en su historial desde el principio.
 */
@Component
public class ClaveDeNegocioAutomatica {

  static final String PREFIJO_GENERICO = "PROC";
  static final int LARGO_SUFIJO = 8;

  private static final Map<String, String> PREFIJOS = Map.of(
      "CB_GestionIncidenteTI", "TI",
      "CB_GestionNomina", "NOM",
      "CB_PostventaGarantias", "PV");
  private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  private final SecureRandom azar = new SecureRandom();

  @EventListener
  public void alEjecutar(DelegateExecution ejecucion) {
    if (!ExecutionListener.EVENTNAME_START.equals(ejecucion.getEventName())) {
      return;
    }
    String claveActual = ejecucion.getProcessBusinessKey();
    if (claveActual != null && !claveActual.isBlank()) {
      return;
    }
    // Las instancias hijas reciben la clave de su call activity (camunda:in businessKey); no se tocan.
    boolean esHija = ejecucion.getProcessInstance().getSuperExecution() != null;
    if (esHija) {
      return;
    }
    ejecucion.setProcessBusinessKey(nuevaClave(claveDelProceso(ejecucion)));
  }

  /** {@code <prefijo>-<8 caracteres>}: TI, NOM o PV según el proceso; PROC para cualquier otro. */
  String nuevaClave(String claveDelProceso) {
    String prefijo = PREFIJOS.getOrDefault(claveDelProceso, PREFIJO_GENERICO);
    StringBuilder clave = new StringBuilder(prefijo).append('-');
    for (int i = 0; i < LARGO_SUFIJO; i++) {
      clave.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
    }
    return clave.toString();
  }

  private static String claveDelProceso(DelegateExecution ejecucion) {
    return ejecucion.getProcessEngineServices()
        .getRepositoryService()
        .getProcessDefinition(ejecucion.getProcessDefinitionId())
        .getKey();
  }
}
