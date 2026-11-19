package com.corocorabenz.engine.identidad;

import java.util.List;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cuentas y grupos de demostración (sección 3 del contrato). Los portales validan contra ellas el
 * inicio de sesión, así que se crean en cualquier perfil, no solo con H2.
 *
 * <p>La cuenta {@code demo} (grupo {@code camunda-admin}) no está en esta lista: la crea Camunda con
 * {@code camunda.bpm.admin-user}, para que las webapps siempre tengan un administrador.
 *
 * <p>Se desactiva con {@code corocora.demo.identidades=false}.
 */
@Configuration
@ConditionalOnProperty(name = "corocora.demo.identidades", havingValue = "true", matchIfMissing = true)
public class IdentidadesDemo {

  static final String DOMINIO_CORREO = "@corocorabenz.test";

  /** Correo que la versión anterior del motor le ponía al solicitante. */
  static final String CORREO_VERSION_ANTERIOR = "usr-01@example.com";

  /** Único usuario cuya contraseña no es la de demostración: es igual a su id. */
  static final String SOLICITANTE = "USR-01";

  private static final String TIPO_GRUPO = "WORKFLOW";

  /** Una cuenta de la tabla del contrato. {@code grupo} es {@code null} si no pertenece a ninguno. */
  record Cuenta(String id, String nombre, String grupo) {
  }

  static final List<Cuenta> CUENTAS = List.of(
      new Cuenta("mesa1", "Mesa de servicio", "mesa-servicio"),
      new Cuenta("soporte1", "Soporte TI nivel 1", "soporte-ti-n1"),
      new Cuenta("especialista1", "Soporte TI especializado", "soporte-ti-especializado"),
      new Cuenta("responsable1", "Responsable de servicio TI", "responsables-ti"),
      new Cuenta("nomina1", "Analista de nómina", "analistas-nomina"),
      new Cuenta("talento1", "Gestión de talento humano", "gestion-talento-humano"),
      new Cuenta("jefatura1", "Jefatura de nómina", "jefatura-nomina"),
      new Cuenta("tesoreria1", "Tesorería", "tesoreria"),
      new Cuenta("asesor1", "Asesor de servicio", "asesores-servicio"),
      new Cuenta("garantia1", "Analista de garantía", "analistas-garantia"),
      new Cuenta("coordinador1", "Coordinador de postventa", "coordinadores-postventa"),
      new Cuenta("tecnico1", "Técnico de taller", "tecnicos-taller"),
      new Cuenta("calidad1", "Inspector de calidad", "inspectores-calidad"),
      new Cuenta("bodega1", "Bodega", "bodega"),
      new Cuenta("cliente1", "Cliente uno", "clientes"),
      new Cuenta("cliente2", "Cliente dos", "clientes"),
      new Cuenta(SOLICITANTE, "Solicitante", null));

  @Bean
  ApplicationRunner crearIdentidadesDemo(IdentityService identidad,
      @Value("${corocora.demo.password:demo}") String password) {
    if (password.isBlank()) {
      throw new IllegalArgumentException(
          "La contraseña de demostración (corocora.demo.password) no puede estar vacía");
    }
    return args -> {
      for (Cuenta cuenta : CUENTAS) {
        asegurarUsuario(identidad, cuenta, password);
        if (cuenta.grupo() != null) {
          asegurarGrupo(identidad, cuenta.grupo());
          asegurarPertenencia(identidad, cuenta.id(), cuenta.grupo());
        }
      }
    };
  }

  /** Correo de una cuenta de demostración: {@code <id>@corocorabenz.test}, con el id tal cual. */
  static String correo(String id) {
    return id + DOMINIO_CORREO;
  }

  private static void asegurarUsuario(IdentityService identidad, Cuenta cuenta, String password) {
    User existente = identidad.createUserQuery().userId(cuenta.id()).singleResult();
    if (existente != null) {
      completarCorreo(identidad, existente);
      return;
    }
    User usuario = identidad.newUser(cuenta.id());
    usuario.setFirstName(cuenta.nombre());
    usuario.setLastName("Demo");
    usuario.setEmail(correo(cuenta.id()));
    usuario.setPassword(SOLICITANTE.equals(cuenta.id()) ? SOLICITANTE : password);
    identidad.saveUser(usuario);
  }

  /**
   * Una cuenta creada por una versión anterior puede no tener correo, o tener el que esa versión
   * generaba, y las notificaciones necesitan el del contrato. Solo se corrige ese dato: la
   * contraseña y el resto de la cuenta no se tocan, ni tampoco un correo que alguien haya cambiado.
   */
  private static void completarCorreo(IdentityService identidad, User existente) {
    String esperado = correo(existente.getId());
    String actual = existente.getEmail();
    if (esperado.equals(actual) || !esHeredado(actual, esperado)) {
      return;
    }
    existente.setEmail(esperado);
    identidad.saveUser(existente);
  }

  /** Vacío, el de la versión anterior, o el de demostración escrito con otras mayúsculas. */
  private static boolean esHeredado(String actual, String esperado) {
    if (actual == null || actual.isBlank()) {
      return true;
    }
    return CORREO_VERSION_ANTERIOR.equals(actual) || esperado.equalsIgnoreCase(actual);
  }

  private static void asegurarGrupo(IdentityService identidad, String id) {
    if (identidad.createGroupQuery().groupId(id).count() > 0) {
      return;
    }
    Group grupo = identidad.newGroup(id);
    grupo.setName(id);
    grupo.setType(TIPO_GRUPO);
    identidad.saveGroup(grupo);
  }

  private static void asegurarPertenencia(IdentityService identidad, String usuario, String grupo) {
    boolean yaPertenece = identidad.createUserQuery().userId(usuario).memberOfGroup(grupo).count() > 0;
    if (!yaPertenece) {
      identidad.createMembership(usuario, grupo);
    }
  }
}
