
package com.securevault.securevault.config;

import com.securevault.securevault.entity.Role;
import com.securevault.securevault.entity.User;
import com.securevault.securevault.repository.RoleRepository;
import com.securevault.securevault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${securevault.admin.email:}")
    private String adminEmail;

    @Value("${securevault.admin.password:}")
    private String adminPassword;

    public InitialAdminInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
            throw new IllegalStateException(
                    "Both INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD must be configured.");
        }

        String email = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);

        var existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.getRole() != null
                    && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
                return;
            }

            throw new IllegalStateException(
                    "Initial admin email already belongs to a non-admin account. "
                            + "Use a new email or promote the account through an authorized process.");
        }

        Role adminRole = roleRepository
                .findByRoleName("ADMIN")
                .orElseThrow(
                        () -> new IllegalStateException("ADMIN role not found in the roles table."));

        User admin = new User();
        admin.setFullName("SecureVault Administrator");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setEmailVerified(false);

        userRepository.save(admin);
    }
}
