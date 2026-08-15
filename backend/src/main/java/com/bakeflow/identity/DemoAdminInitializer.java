package com.bakeflow.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
class DemoAdminInitializer implements CommandLineRunner {
  private final IdentityService service;
  private final String email, password;

  DemoAdminInitializer(
      IdentityService service,
      @Value("${security.demo-admin-email}") String email,
      @Value("${security.demo-admin-password}") String password) {
    this.service = service;
    this.email = email;
    this.password = password;
  }

  public void run(String... args) {
    if (password != null
        && !password.isBlank()
        && (password.length() < 8 || password.length() > 128))
      throw new IllegalStateException(
          "DEMO_ADMIN_PASSWORD must contain between 8 and 128 characters.");
    service.ensureAdmin(email, password);
  }
}
