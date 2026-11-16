package com.corocorabenz.engine.delegate.nomina;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** "Registrar pago y cerrar periodo". */
@Component("registrarPagoDelegate")
public class RegistrarPagoDelegate implements JavaDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(RegistrarPagoDelegate.class);

  @Override
  public void execute(DelegateExecution execution) {
    LOG.info("Pago registrado y periodo {} cerrado", execution.getVariable("payPeriodId"));
    execution.setVariable("paymentRecorded", true);
    execution.setVariable("payrollPeriodClosed", true);
  }
}
