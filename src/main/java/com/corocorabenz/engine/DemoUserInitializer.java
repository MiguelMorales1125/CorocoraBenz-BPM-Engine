package com.corocorabenz.engine;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoUserInitializer {

  @Bean
  ApplicationRunner createDemoRequester(IdentityService identityService) {
    return args -> {
      if (identityService.createUserQuery().userId("USR-01").singleResult() == null) {
        User requester = identityService.newUser("USR-01");
        requester.setFirstName("Solicitante");
        requester.setLastName("Demo");
        requester.setEmail("usr-01@example.com");
        requester.setPassword("USR-01");
        identityService.saveUser(requester);
      }
    };
  }
}
