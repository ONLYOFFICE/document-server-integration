package com.onlyoffice.integration.repositories;

import com.onlyoffice.integration.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
}
