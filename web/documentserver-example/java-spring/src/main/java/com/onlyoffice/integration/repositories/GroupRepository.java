package com.onlyoffice.integration.repositories;

import com.onlyoffice.integration.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Integer> {
    Optional<Group> findGroupByName(String name);
}
