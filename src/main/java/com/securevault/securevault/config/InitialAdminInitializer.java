package com.securevault.securevault.config;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.entity.Role;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.RoleRepository;
import com.securevault.securevault.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final MessageSource messageSource;

  @Value("${securevault.admin.email:}")
  private String adminEmail;

  @Value("${securevault.admin.password:}")
  private String adminPassword;

  public InitialAdminInitializer(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      MessageSource messageSource) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.messageSource = messageSource;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {

    boolean emailMissing = adminEmail == null || adminEmail.isBlank();
    boolean passwordMissing = adminPassword == null || adminPassword.isBlank();

    if (emailMissing && passwordMissing) {
      return;
    }

    if (emailMissing || passwordMissing) {
      throw new IllegalStateException(getMessage(MessageKeys.ADMIN_CONFIGURATION_REQUIRED));
    }

    String email = adminEmail.trim().toLowerCase(Locale.ROOT);

    var existingUser = userRepository.findByEmail(email);

    if (existingUser.isPresent()) {
      User user = existingUser.get();

      if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
        return;
      }

      throw new IllegalStateException(getMessage(MessageKeys.ADMIN_EMAIL_ALREADY_REGISTERED));
    }

    Role adminRole =
        roleRepository
            .findByRoleName("ADMIN")
            .orElseThrow(
                () -> new IllegalStateException(getMessage(MessageKeys.ADMIN_ROLE_NOT_FOUND)));

    User admin = new User();
    admin.setFullName("SecureVault Administrator");
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
    admin.setRole(adminRole);
    admin.setEmailVerified(false);

    userRepository.save(admin);
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}
