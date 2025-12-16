package com.example.smartshelfx.dto;

import lombok.Data;

@Data
public class RoleUpdateRequest {
    private String role; // expects "ROLE_ADMIN" or "ADMIN" (we will normalize)
}
