package com.securevault.securevault.repository;

import com.securevault.securevault.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByRoleName(String roleName);
}
