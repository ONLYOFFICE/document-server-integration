package com.onlyoffice.integration.repositories;

import com.onlyoffice.integration.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
