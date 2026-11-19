package com.corocorabenz.engine.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.camunda.bpm.engine.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Propiedades {@code corocora.demo.*}, sin arrancar el motor. */
class IdentidadesDemoConfiguracionTest {

  private final ApplicationContextRunner contexto = new ApplicationContextRunner()
      .withUserConfiguration(IdentidadesDemo.class)
      .withBean(IdentityService.class, () -> mock(IdentityService.class));

  @Test
  void seCreanSiLaPropiedadNoExiste() {
    contexto.run(resultado -> assertThat(resultado).hasSingleBean(ApplicationRunner.class));
  }

  @Test
  void seDesactivanConLaPropiedadEnFalse() {
    contexto.withPropertyValues("corocora.demo.identidades=false")
        .run(resultado -> assertThat(resultado).doesNotHaveBean(ApplicationRunner.class));
  }

  @Test
  void unaContrasenaVaciaImpideArrancar() {
    contexto.withPropertyValues("corocora.demo.password=")
        .run(resultado -> assertThat(resultado)
            .getFailure()
            .hasRootCauseMessage("La contraseña de demostración (corocora.demo.password) no puede estar vacía"));
  }
}
