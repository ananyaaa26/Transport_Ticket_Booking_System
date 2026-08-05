package com.example.transport.ticket.controller;

import com.example.transport.ticket.config.JwtUtil;
import com.example.transport.ticket.model.User;
import com.example.transport.ticket.repository.UserRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Add this temporary method to your AuthController class
//    @GetMapping("/create-admin")
//    public String createAdminUser() {
//        // Check if the user already exists to avoid errors on multiple runs
//        if (userRepository.findByUsername("superadmin").isEmpty()) {
//            User adminUser = User.builder()
//                    .username("superadmin")
//                    .password(passwordEncoder.encode("password")) // Use the app's own encoder
//                    .role("ROLE_ADMIN")
//                    .build();
//            userRepository.save(adminUser);
//            return "New admin 'superadmin' created with password 'password'. You can now log in.";
//        }
//        return "Admin 'superadmin' already exists.";
//    }
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("");

            String token = jwtUtil.generateToken(request.getUsername(), role);
            return Map.of("token", token, "role", role);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody AuthRequest request
    ) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Username already exists."
                    )
            );

        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(newUser);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User registered successfully."
                )
        );

    }

    @Data
    static class AuthRequest {
        private String username;
        private String password;
    }
}