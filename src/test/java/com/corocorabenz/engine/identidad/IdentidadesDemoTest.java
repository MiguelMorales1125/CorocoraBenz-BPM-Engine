package com.corocorabenz.engine.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.corocorabenz.engine.PruebaDelMotorSinWeb;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;

/** Cuentas de demostración: la tabla de la sección 3 del contrato, copiada aquí a propósito. */
@PruebaDelMotorSinWeb
class IdentidadesDemoTest {

  private static final String SIN_GRUPO = "";
  private static final Map<String, String> GRUPO_POR_CUENTA = tablaDelContrato();

  @Autowired
  private IdentityService identidad;

  @Autowired
  @Qualifier("crearIdentidadesDemo")
  private ApplicationRunner crearIdentidadesDemo;

  private static Map<String, String> tablaDelContrato() {
    Map<String, String> tabla = new LinkedHashMap<>();
    tabla.put("demo", "camunda-admin");
    tabla.put("mesa1", "mesa-servicio");
    tabla.put("soporte1", "soporte-ti-n1");
    tabla.put("especialista1", "soporte-ti-especializado");
    tabla.put("responsable1", "responsables-ti");
    tabla.put("nomina1", "analistas-nomina");
    tabla.put("talento1", "gestion-talento-humano");
    tabla.put("jefatura1", "jefatura-nomina");
    tabla.put("tesoreria1", "tesoreria");
    tabla.put("asesor1", "asesores-servicio");
    tabla.put("garantia1", "analistas-garantia");
    tabla.put("coordinador1", "coordinadores-postventa");
    tabla.put("tecnico1", "tecnicos-taller");
    tabla.put("calidad1", "inspectores-calidad");
    tabla.put("bodega1", "bodega");
    tabla.put("cliente1", "clientes");
    tabla.put("cliente2", "clientes");
    tabla.put("USR-01", SIN_GRUPO);
    return tabla;
  }

  @Test
  void cadaCuentaExisteConSuGrupoExacto() {
    for (Map.Entry<String, String> fila : GRUPO_POR_CUENTA.entrySet()) {
      String cuenta = fila.getKey();
      List<String> esperados = fila.getValue().equals(SIN_GRUPO) ? List.of() : List.of(fila.getValue());

      assertThat(usuario(cuenta)).as("cuenta %s", cuenta).isNotNull();
      assertThat(gruposDe(cuenta)).as("grupos de %s", cuenta).isEqualTo(esperados);
    }
  }

  @Test
  void noHayMasCuentasQueLasDelContrato() {
    List<String> cuentas = identidad.createUserQuery().list().stream()
        .map(User::getId)
        .toList();

    assertThat(cuentas).containsExactlyInAnyOrderElementsOf(GRUPO_POR_CUENTA.keySet());
  }

  @Test
  void cadaCuentaTieneElCorreoDelContrato() {
    for (String cuenta : GRUPO_POR_CUENTA.keySet()) {
      assertThat(usuario(cuenta).getEmail()).as("correo de %s", cuenta).isEqualTo(cuenta + "@corocorabenz.test");
    }
    assertThat(usuario("USR-01").getEmail()).isEqualTo("USR-01@corocorabenz.test");
  }

  @Test
  void laContrasenaEsDemoSalvoLaDelSolicitante() {
    for (String cuenta : GRUPO_POR_CUENTA.keySet()) {
      String esperada = cuenta.equals("USR-01") ? "USR-01" : "demo";
      assertThat(identidad.checkPassword(cuenta, esperada)).as("contraseña de %s", cuenta).isTrue();
    }
  }

  @Test
  void rechazaUnaContrasenaIncorrecta() {
    assertThat(rechaza("USR-01", "demo")).isTrue();
    assertThat(rechaza("cliente2", "demo-incorrecta")).isTrue();
  }

  @Test
  void volverAEjecutarNoFallaNiDuplica() {
    long usuarios = identidad.createUserQuery().count();
    long grupos = identidad.createGroupQuery().count();

    assertThatCode(this::ejecutarDeNuevo).doesNotThrowAnyException();

    assertThat(identidad.createUserQuery().count()).isEqualTo(usuarios);
    assertThat(identidad.createGroupQuery().count()).isEqualTo(grupos);
    assertThat(gruposDe("cliente1")).containsExactly("clientes");
  }

  @Test
  void volverAEjecutarNoCambiaUnaContrasenaYaCambiada() throws Exception {
    cambiarContrasena("bodega1", "otra-clave");
    try {
      ejecutarDeNuevo();

      assertThat(identidad.checkPassword("bodega1", "otra-clave")).isTrue();
      assertThat(rechaza("bodega1", "demo")).isTrue();
    } finally {
      cambiarContrasena("bodega1", "demo");
    }
  }

  @Test
  void completaElCorreoDeUnaCuentaQueNoLoTenia() throws Exception {
    User tecnico = usuario("tecnico1");
    tecnico.setEmail(null);
    identidad.saveUser(tecnico);

    ejecutarDeNuevo();

    assertThat(usuario("tecnico1").getEmail()).isEqualTo("tecnico1@corocorabenz.test");
    assertThat(identidad.checkPassword("tecnico1", "demo")).isTrue();
  }

  @Test
  void reemplazaElCorreoQueDejoLaVersionAnterior() throws Exception {
    for (String heredado : List.of("usr-01@example.com", "usr-01@corocorabenz.test")) {
      ponerCorreo("USR-01", heredado);

      ejecutarDeNuevo();

      assertThat(usuario("USR-01").getEmail()).as("tras heredar %s", heredado).isEqualTo("USR-01@corocorabenz.test");
    }
    assertThat(identidad.checkPassword("USR-01", "USR-01")).isTrue();
  }

  @Test
  void respetaUnCorreoCambiadoAMano() throws Exception {
    ponerCorreo("calidad1", "calidad@otro-dominio.test");
    try {
      ejecutarDeNuevo();

      assertThat(usuario("calidad1").getEmail()).isEqualTo("calidad@otro-dominio.test");
    } finally {
      ponerCorreo("calidad1", "calidad1@corocorabenz.test");
    }
  }

  private void ejecutarDeNuevo() throws Exception {
    crearIdentidadesDemo.run(new DefaultApplicationArguments());
  }

  /**
   * Tras un intento fallido Camunda bloquea la cuenta unos segundos (protección contra fuerza bruta)
   * y rechazaría también la contraseña correcta. Se desbloquea para no afectar a las demás pruebas.
   */
  private boolean rechaza(String cuenta, String contrasena) {
    boolean aceptada = identidad.checkPassword(cuenta, contrasena);
    identidad.unlockUser(cuenta);
    return !aceptada;
  }

  private void cambiarContrasena(String cuenta, String contrasena) {
    User usuario = usuario(cuenta);
    usuario.setPassword(contrasena);
    identidad.saveUser(usuario);
  }

  private void ponerCorreo(String cuenta, String correo) {
    User usuario = usuario(cuenta);
    usuario.setEmail(correo);
    identidad.saveUser(usuario);
  }

  private User usuario(String id) {
    return identidad.createUserQuery().userId(id).singleResult();
  }

  private List<String> gruposDe(String cuenta) {
    return identidad.createGroupQuery().groupMember(cuenta).list().stream()
        .map(Group::getId)
        .toList();
  }
}
