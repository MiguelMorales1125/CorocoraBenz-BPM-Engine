package com.corocorabenz.engine.delegate.nomina;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * "Calcular liquidación". Cálculo ficticio: no implementa reglas laborales reales.
 * La consistencia no se decide aquí, la decide el analista en su formulario.
 */
@Component("calcularLiquidacionDelegate")
public class CalcularLiquidacionDelegate implements JavaDelegate {

  static final long TOTAL_SIMULADO = 1_000_000L;

  @Override
  public void execute(DelegateExecution execution) {
    Object period = execution.getVariable("payPeriodId");
    execution.setVariable("payrollSummary", "Liquidación simulada del periodo "
        + (period != null ? period : "sin periodo") + ", sin excepciones automáticas");
    execution.setVariable("payrollTotal", TOTAL_SIMULADO);
  }
}
