package com.securevault.securevault.service;

import com.securevault.securevault.constant.ApiMessages;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.entity.Role;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.RoleRepository;
import com.securevault.securevault.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final String DEFAULT_ROLE_NAME = "USER";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User register(RegisterRequest request) {

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalArgumentException(ApiMessages.EMAIL_ALREADY_REGISTERED);
    }

    Role userRole =
        roleRepository
            .findByRoleName(DEFAULT_ROLE_NAME)
            .orElseThrow(
                () -> new IllegalStateException("Default role USER not found in roles table"));

    User user = new User();
    user.setFullName(request.getFullName().trim());
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(userRole);
    user.setEmailVerified(false);

    return userRepository.save(user);
  }
}
