package com.securevault.securevault.repository;

import com.securevault.securevault.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {

  Optional<Role> findByRoleName(String roleName);
}
