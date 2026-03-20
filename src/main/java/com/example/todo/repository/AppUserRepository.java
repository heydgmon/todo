package com.example.todo.repository;

import com.example.todo.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByCognitoSub(String cognitoSub);
    Optional<AppUser> findByEmail(String email);
}