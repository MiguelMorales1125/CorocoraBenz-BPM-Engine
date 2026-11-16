package com.corocorabenz.engine.delegate.nomina;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** "Generar comprobantes de pago". */
@Component("generarComprobantesDelegate")
public class GenerarComprobantesDelegate implements JavaDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(GenerarComprobantesDelegate.class);

  @Override
  public void execute(DelegateExecution execution) {
    LOG.info("Comprobantes de pago generados para el periodo {} (lote {})",
        execution.getVariable("payPeriodId"), execution.getVariable("paymentBatchId"));
    execution.setVariable("receiptsGenerated", true);
  }
}
