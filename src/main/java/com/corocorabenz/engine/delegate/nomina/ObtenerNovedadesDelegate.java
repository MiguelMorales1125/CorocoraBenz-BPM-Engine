package com.corocorabenz.engine.delegate.nomina;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * "Recolectar novedades del periodo". Simula el sistema de RR. HH. con novedades fijas de demostración.
 *
 * <p>Variables opcionales al iniciar el proceso: {@code simulatePeriodClosed},
 * {@code simulateMissingSupports}, {@code simulateLateChanges}. Cuando Talento Humano ya registró su
 * corrección ({@code correctionAction}), el periodo vuelve abierto y con soportes para no repetir el ciclo.
 */
@Component("obtenerNovedadesDelegate")
public class ObtenerNovedadesDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    boolean corrected = execution.getVariable("correctionAction") != null;
    boolean periodOpen = corrected || !Boolean.TRUE.equals(execution.getVariable("simulatePeriodClosed"));
    boolean documentsComplete = corrected || !Boolean.TRUE.equals(execution.getVariable("simulateMissingSupports"));
    boolean hasLateChanges = !corrected && Boolean.TRUE.equals(execution.getVariable("simulateLateChanges"));

    List<Map<String, Object>> changes = new ArrayList<>();
    changes.add(novedad("EMP-001", "Horas extra", 2, true));
    changes.add(novedad("EMP-002", "Incapacidad", 1, documentsComplete));
    if (hasLateChanges) {
      changes.add(novedad("EMP-003", "Horas extra", 4, true));
    }

    execution.setVariable("periodOpen", periodOpen);
    execution.setVariable("documentsComplete", documentsComplete);
    execution.setVariable("hasLateChanges", hasLateChanges);
    execution.setVariable("lateChangesSummary",
        hasLateChanges ? "EMP-003 · Horas extra · 4 h registradas después de la fecha de corte" : null);
    // La multiinstancia "Validar cada novedad" recorre esta colección.
    execution.setVariable("employeeChanges", changes);
  }

  private static Map<String, Object> novedad(String employeeId, String changeType, long changeValue,
      boolean supportAttached) {
    Map<String, Object> change = new LinkedHashMap<>();
    change.put("employeeId", employeeId);
    change.put("changeType", changeType);
    change.put("changeValue", changeValue);
    change.put("supportAttached", supportAttached);
    return change;
  }
}
