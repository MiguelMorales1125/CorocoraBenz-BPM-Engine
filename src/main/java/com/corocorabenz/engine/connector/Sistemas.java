package com.corocorabenz.engine.connector;

import com.corocorabenz.comun.credencial.CredencialInterna;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Datos de los sistemas externos para los HTTP Connector de los BPMN: sus URLs y la credencial
 * interna con la que el motor se identifica ante ellos.
 * Ejemplo: {@code ${sistemas.banco}/api/dispersiones} con el encabezado
 * {@code X-CB-Internal: ${sistemas.token}}.
 */
@Component("sistemas")
public class Sistemas {

  private final String banco;
  private final String tallerBodega;
  private final String portalTi;
  private final CredencialInterna credencial;

  public Sistemas(@Value("${corocora.sistemas.banco}") String banco,
      @Value("${corocora.sistemas.taller-bodega}") String tallerBodega,
      @Value("${corocora.sistemas.portal-ti}") String portalTi,
      CredencialInterna credencial) {
    this.banco = banco;
    this.tallerBodega = tallerBodega;
    this.portalTi = portalTi;
    this.credencial = credencial;
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

  /** Credencial interna (sección 2 del contrato). En los BPMN: {@code ${sistemas.token}}. */
  public String getToken() {
    return credencial.token();
  }
}
