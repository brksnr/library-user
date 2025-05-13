package com.library_user.repository;

import com.library_user.model.entity.Role;
import com.library_user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndRole(UUID id, Role role);

    boolean existsByEmailAndIdNot(String email, UUID id );
}
