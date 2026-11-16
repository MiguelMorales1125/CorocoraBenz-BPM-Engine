package com.corocorabenz.engine.connector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Ayudas para los HTTP Connector de los BPMN, para no tener que escribir scripts:
 * <ul>
 *   <li>Entrada: {@code ${json.cuerpo(execution, 'vin,mileageKm')}} arma el JSON con la businessKey
 *       y las variables indicadas.</li>
 *   <li>Salida: {@code ${json.texto(statusCode, response, 'quoteId')}}, {@code json.booleano(...)} y
 *       {@code json.numero(...)} leen un campo de la respuesta. Si el sistema respondió con error
 *       (4xx/5xx) lanzan una excepción: el job se reintenta y, si sigue fallando, queda un incidente
 *       visible en Cockpit.</li>
 * </ul>
 */
@Component("json")
public class JsonConnector {

  private final ObjectMapper mapper;

  public JsonConnector(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String cuerpo(DelegateExecution execution, String variables) {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("businessKey", execution.getProcessBusinessKey());
    for (String nombre : variables.split(",")) {
      String variable = nombre.trim();
      if (!variable.isEmpty()) {
        cuerpo.put(variable, execution.getVariable(variable));
      }
    }
    try {
      return mapper.writeValueAsString(cuerpo);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo armar el JSON del connector", e);
    }
  }

  public String texto(Integer statusCode, String response, String campo) {
    JsonNode valor = campo(statusCode, response, campo);
    return valor.isNull() || valor.isMissingNode() ? null : valor.asText();
  }

  public Boolean booleano(Integer statusCode, String response, String campo) {
    JsonNode valor = campo(statusCode, response, campo);
    return valor.isNull() || valor.isMissingNode() ? null : valor.asBoolean();
  }

  public Long numero(Integer statusCode, String response, String campo) {
    JsonNode valor = campo(statusCode, response, campo);
    return valor.isNull() || valor.isMissingNode() ? null : valor.asLong();
  }

  private JsonNode campo(Integer statusCode, String response, String campo) {
    if (statusCode == null || statusCode < 200 || statusCode >= 300) {
      throw new IllegalStateException("El sistema externo respondió HTTP " + statusCode + ": " + response);
    }
    try {
      return mapper.readTree(response == null ? "{}" : response).path(campo);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Respuesta no es JSON: " + response, e);
    }
  }
}
