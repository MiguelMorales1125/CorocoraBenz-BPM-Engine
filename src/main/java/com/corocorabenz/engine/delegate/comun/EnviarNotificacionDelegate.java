package com.corocorabenz.engine.delegate.comun;

import com.corocorabenz.engine.notificacion.CorreoService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Component;

/**
 * Notificaciones por correo de los tres procesos. El mensaje depende de la actividad que lo dispara:
 * <ul>
 *   <li>{@code notifySolution} (TI): la solución está lista, el usuario debe confirmarla.</li>
 *   <li>{@code notifyAutoClose} (TI): el ticket se cerró porque el usuario no respondió.</li>
 *   <li>{@code notifyRejection} (Postventa): el caso se cerró sin reparación.</li>
 * </ul>
 * Destinatario: {@code notificationEmail} o {@code customerEmail} si existen; si no, el correo del
 * usuario {@code requesterId} registrado en Camunda.
 */
@Component("enviarNotificacionDelegate")
public class EnviarNotificacionDelegate implements JavaDelegate {

  private final CorreoService correo;
  private final IdentityService identityService;

  public EnviarNotificacionDelegate(CorreoService correo, IdentityService identityService) {
    this.correo = correo;
    this.identityService = identityService;
  }

  @Override
  public void execute(DelegateExecution execution) {
    String actividad = execution.getCurrentActivityId();
    String caso = texto(execution, "ticketId", execution.getProcessBusinessKey());
    String asunto;
    String cuerpo;

    switch (actividad) {
      case "notifySolution" -> {
        asunto = "Tu incidente " + caso + " tiene solución";
        cuerpo = "Hola,\n\nSoporte TI registró esta solución para tu incidente " + caso + ":\n\n"
            + texto(execution, "resolutionSummary", "(sin detalle)")
            + "\n\nEntra al portal de Mesa de Ayuda TI y confirma si el problema quedó resuelto."
            + "\nSi no respondes en 24 horas el ticket se cerrará automáticamente.";
      }
      case "notifyAutoClose" -> {
        asunto = "Tu incidente " + caso + " fue cerrado";
        cuerpo = "Hola,\n\nCerramos el incidente " + caso
            + " porque no recibimos tu confirmación en 24 horas."
            + "\nSi el problema continúa, reporta un nuevo incidente en el portal de Mesa de Ayuda TI.";
      }
      case "notifyRejection" -> {
        asunto = "Tu solicitud de servicio " + caso + " fue cerrada";
        cuerpo = "Hola " + texto(execution, "customerName", "") + ",\n\n"
            + "Cerramos la solicitud de servicio del vehículo " + texto(execution, "vin", "")
            + " sin realizar la reparación, porque la cotización no fue aceptada."
            + "\nPuedes crear una nueva solicitud cuando quieras en el Portal de Clientes.";
      }
      default -> {
        asunto = "Notificación de Corocora Benz";
        cuerpo = "Hay una novedad en tu caso " + caso + ".";
      }
    }

    CorreoService.Modo modo = correo.enviar(destinatario(execution), asunto, cuerpo);
    execution.setVariable("notificationSent", modo == CorreoService.Modo.EMAIL);
    execution.setVariable("notificationMode", modo.name());
  }

  private String destinatario(DelegateExecution execution) {
    String directo = texto(execution, "notificationEmail", null);
    if (directo == null) {
      directo = texto(execution, "customerEmail", null);
    }
    if (directo != null) {
      return directo;
    }
    String requesterId = texto(execution, "requesterId", null);
    if (requesterId == null) {
      return null;
    }
    User user = identityService.createUserQuery().userId(requesterId).singleResult();
    return user != null ? user.getEmail() : null;
  }

  private static String texto(DelegateExecution execution, String variable, String porDefecto) {
    Object valor = execution.getVariable(variable);
    return valor != null && !valor.toString().isBlank() ? valor.toString() : porDefecto;
  }
}
