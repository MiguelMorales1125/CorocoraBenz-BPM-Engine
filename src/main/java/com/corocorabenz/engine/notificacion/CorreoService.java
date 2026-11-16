package com.corocorabenz.engine.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía los correos del motor. Con {@code corocora.correo.habilitado=false} (por defecto) solo los deja
 * en el log; así el proceso funciona aunque no haya servidor de correo. Para verlos de verdad se puede
 * usar Mailpit (SMTP en localhost:1025, bandeja en http://localhost:8025).
 */
@Service
public class CorreoService {

  private static final Logger LOG = LoggerFactory.getLogger(CorreoService.class);

  private final ObjectProvider<JavaMailSender> mailSender;
  private final boolean habilitado;
  private final String remitente;

  public CorreoService(ObjectProvider<JavaMailSender> mailSender,
      @Value("${corocora.correo.habilitado:false}") boolean habilitado,
      @Value("${corocora.correo.remitente:notificaciones@corocorabenz.test}") String remitente) {
    this.mailSender = mailSender;
    this.habilitado = habilitado;
    this.remitente = remitente;
  }

  /** Resultado del intento de envío, para guardarlo como variable del proceso. */
  public enum Modo { EMAIL, LOG, ERROR }

  public Modo enviar(String destinatario, String asunto, String cuerpo) {
    JavaMailSender sender = mailSender.getIfAvailable();
    if (!habilitado || sender == null || destinatario == null || destinatario.isBlank()) {
      LOG.info("[correo simulado] para={} asunto=\"{}\"\n{}", destinatario, asunto, cuerpo);
      return Modo.LOG;
    }
    try {
      SimpleMailMessage mensaje = new SimpleMailMessage();
      mensaje.setFrom(remitente);
      mensaje.setTo(destinatario);
      mensaje.setSubject(asunto);
      mensaje.setText(cuerpo);
      sender.send(mensaje);
      LOG.info("Correo enviado a {}: {}", destinatario, asunto);
      return Modo.EMAIL;
    } catch (MailException e) {
      // Un fallo de correo no debe detener el proceso de negocio.
      LOG.warn("No se pudo enviar el correo a {} ({}): {}", destinatario, asunto, e.getMessage());
      return Modo.ERROR;
    }
  }
}
