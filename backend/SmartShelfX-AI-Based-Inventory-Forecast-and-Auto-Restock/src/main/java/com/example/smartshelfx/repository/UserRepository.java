package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.Role;
import com.example.smartshelfx.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByActiveTrue();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(Role role);

    // This query expects the DB column contains 'ROLE_VENDOR'
    @Query("SELECT u FROM User u WHERE u.role = com.example.smartshelfx.model.Role.ROLE_VENDOR AND u.active = true")
    List<User> findActiveVendors();
}
