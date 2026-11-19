package com.corocorabenz.engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Arranca el motor completo sobre H2 en memoria, sin servidor web y sin job executor: las tareas
 * asíncronas (los HTTP Connector) se quedan esperando y ninguna prueba llama a otro sistema.
 *
 * <p>Todas las clases anotadas comparten el mismo contexto de Spring, es decir, el mismo motor.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "camunda.bpm.job-execution.enabled=false",
        // La REST de Camunda exige un servidor web (Jersey) y sin él impide arrancar el contexto.
        "spring.autoconfigure.exclude="
            + "org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration"})
@ActiveProfiles("h2")
public @interface PruebaDelMotorSinWeb {
}
