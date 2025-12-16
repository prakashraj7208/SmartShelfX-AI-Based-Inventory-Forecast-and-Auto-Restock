package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.dto.AuthRequest;
import com.example.smartshelfx.dto.RegisterRequest;
import com.example.smartshelfx.model.Role;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.security.CustomUserDetails;
import com.example.smartshelfx.security.JwtUtil;
import com.example.smartshelfx.security.TokenBlacklist;
import com.example.smartshelfx.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody AuthRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            String accessToken = jwtUtil.generateAccessToken(email, role);
            String refreshToken = jwtUtil.generateRefreshToken(email, role);

            // Set refresh token in HttpOnly cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false) // set true in production (HTTPS)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            Map<String, String> tokens = Map.of(
                    "accessToken", accessToken,
                    "tokenType", "Bearer",
                    "role", role,
                    "email", email
            );

            return ResponseEntity.ok(ApiResponse.success("Logged in successfully", tokens));

        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials", null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword());
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            user.setPhone(request.getPhone());
            user.setCompany(request.getCompany());

            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", savedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired refresh token", null));
        }

        try {
            String email = jwtUtil.extractEmail(refreshToken);
            CustomUserDetails user = (CustomUserDetails) userService.loadUserByUsername(email);
            String role = user.getAuthorities().iterator().next().getAuthority();

            String newAccessToken = jwtUtil.generateAccessToken(email, role);

            Map<String, String> tokens = Map.of(
                    "accessToken", newAccessToken,
                    "tokenType", "Bearer"
            );

            return ResponseEntity.ok(ApiResponse.success("New access token generated", tokens));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid refresh token", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing or invalid Authorization header", null));
        }

        String token = authHeader.substring(7);

        // Add token to blacklist
        tokenBlacklist.blacklist(token);

        // Clear authentication context
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Not authenticated", null));
            }

            // Get the email from authentication
            String email = auth.getName();

            // Load user from database using email
            User user = userService.getUserByEmail(email);

            return ResponseEntity.ok(ApiResponse.success("User details fetched", user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error fetching user details: " + e.getMessage(), null));
        }
    }
}