package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.dto.RoleUpdateRequest;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.model.Role;
import com.example.smartshelfx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            log.info("Fetching all users");
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch users", null));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.success("User fetched successfully", user));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/admin/users
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        try {
            User saved = userService.registerUser(user);
            return ResponseEntity.ok(ApiResponse.success("User created", saved));
        } catch (Exception e) {
            log.error("Create user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Create failed: " + e.getMessage(), null));
        }
    }

    // PUT /api/admin/users/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> editUser(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id);
            User updated = userService.updateUser(user);
            return ResponseEntity.ok(ApiResponse.success("User updated", updated));
        } catch (Exception e) {
            log.error("Update user failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("Update failed", null));
        }
    }

    // PUT /api/admin/users/{id}/role  (you already had this but keep)
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateRequest roleUpdateRequest) {
        try {
            String raw = roleUpdateRequest.getRole().trim().toUpperCase();
            String normalized = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw;
            Role newRole = Role.valueOf(normalized);
            User user = userService.updateUserRole(id, newRole);
            return ResponseEntity.ok(ApiResponse.success("Role updated successfully", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update role", null));
        }
    }



    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete user", null));
        }
    }

    @GetMapping("/vendors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<User>>> getVendors() {
        try {
            List<User> vendors = userService.getActiveVendors();
            return ResponseEntity.ok(ApiResponse.success("Vendors fetched successfully", vendors));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch vendors", null));
        }
    }


}