package com.securevault.securevault.service;

import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.entity.Role;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.RoleRepository;
import com.securevault.securevault.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final String DEFAULT_ROLE_NAME = "USER";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final MessageSource messageSource;

  public UserService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      MessageSource messageSource) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.messageSource = messageSource;
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User register(RegisterRequest request) {

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalArgumentException(getMessage(MessageKeys.USER_REGISTER_EMAIL_EXISTS));
    }

    Role userRole =
        roleRepository
            .findByRoleName(DEFAULT_ROLE_NAME)
            .orElseThrow(
                () -> new IllegalStateException(getMessage(MessageKeys.USER_ROLE_DEFAULT_MISSING)));

    User user = new User();
    user.setFullName(request.getFullName().trim());
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(userRole);
    user.setEmailVerified(false);

    return userRepository.save(user);
  }

  private String getMessage(String messageKey) {
    return messageSource.getMessage(messageKey, null, Locale.getDefault());
  }
}
