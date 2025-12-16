package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.Role;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Slf4j
public class VendorController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<User>>> getAllVendors() {
        List<User> vendors = userService.getActiveVendors();
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved", vendors));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<User>> createVendor(@RequestBody User vendor) {
        vendor.setRole(Role.ROLE_VENDOR);
        User created = userService.registerUser(vendor);
        return ResponseEntity.ok(ApiResponse.success("Vendor created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getVendor(@PathVariable Long id) {
        User vendor = userService.getUserById(id);
        if (vendor.getRole() != Role.ROLE_VENDOR) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not a vendor", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Vendor fetched", vendor));
    }
}
