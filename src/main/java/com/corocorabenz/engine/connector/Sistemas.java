package com.corocorabenz.engine.connector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * URLs de los sistemas externos, para usarlas en los HTTP Connector de los BPMN.
 * Ejemplo: {@code ${sistemas.banco}/api/dispersiones}.
 */
@Component("sistemas")
public class Sistemas {

  private final String banco;
  private final String tallerBodega;
  private final String portalTi;

  public Sistemas(@Value("${corocora.sistemas.banco}") String banco,
      @Value("${corocora.sistemas.taller-bodega}") String tallerBodega,
      @Value("${corocora.sistemas.portal-ti}") String portalTi) {
    this.banco = banco;
    this.tallerBodega = tallerBodega;
    this.portalTi = portalTi;
  }

  public String getBanco() {
    return banco;
  }

  public String getTallerBodega() {
    return tallerBodega;
  }

  public String getPortalTi() {
    return portalTi;
  }
}
