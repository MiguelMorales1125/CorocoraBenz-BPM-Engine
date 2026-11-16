package com.corocorabenz.engine;

import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Identidades de ejemplo para Tasklist. No sustituye la protección de las API y portales. */
@Configuration
@Profile("h2")
public class DemoIdentityInitializer {
  private static final Map<String, String> EMPLEADOS = Map.ofEntries(
      Map.entry("mesa1", "mesa-servicio"),
      Map.entry("soporte1", "soporte-ti-n1"),
      Map.entry("especialista1", "soporte-ti-especializado"),
      Map.entry("responsable1", "responsables-ti"),
      Map.entry("nomina1", "analistas-nomina"),
      Map.entry("talento1", "gestion-talento-humano"),
      Map.entry("jefatura1", "jefatura-nomina"),
      Map.entry("tesoreria1", "tesoreria"),
      Map.entry("asesor1", "asesores-servicio"),
      Map.entry("garantia1", "analistas-garantia"),
      Map.entry("coordinador1", "coordinadores-postventa"),
      Map.entry("tecnico1", "tecnicos-taller"),
      Map.entry("calidad1", "inspectores-calidad"));

  @Bean
  ApplicationRunner createDemoGroups(IdentityService identity,
      @Value("${corocora.demo-password:demo}") String password) {
    if (password.isBlank()) throw new IllegalArgumentException("La contraseña de demostración no puede estar vacía");
    return args -> {
      for (String id : EMPLEADOS.values()) {
        if (identity.createGroupQuery().groupId(id).singleResult() == null) {
          Group group = identity.newGroup(id);
          group.setName(id);
          group.setType("WORKFLOW");
          identity.saveGroup(group);
        }
      }
      for (var entry : EMPLEADOS.entrySet()) {
        ensureUser(identity, entry.getKey(), password);
        if (identity.createGroupQuery().groupId(entry.getValue()).groupMember(entry.getKey()).count() == 0)
          identity.createMembership(entry.getKey(), entry.getValue());
      }
      for (String id : List.of("cliente1", "cliente2")) ensureUser(identity, id, password);
    };
  }

  private static void ensureUser(IdentityService identity, String id, String password) {
    if (identity.createUserQuery().userId(id).singleResult() == null) {
      User user = identity.newUser(id);
      user.setFirstName(id);
      user.setPassword(password);
      identity.saveUser(user);
    }
  }
}
