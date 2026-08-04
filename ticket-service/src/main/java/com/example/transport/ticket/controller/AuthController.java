package com.example.transport.ticket.controller;

import com.example.transport.ticket.config.JwtUtil;
import com.example.transport.ticket.model.User;
import com.example.transport.ticket.repository.UserRepository;
import lombok.Data;
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

@RestController
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody AuthRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username cannot be empty"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password cannot be empty"
            );
        }

        try {

            Authentication authentication =
                    authenticationManager.authenticate(

                            new UsernamePasswordAuthenticationToken(
                                    request.getUsername().trim(),
                                    request.getPassword()
                            )
                    );

            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("");

            String token = jwtUtil.generateToken(
                    request.getUsername().trim(),
                    role
            );

            return Map.of(
                    "token", token,
                    "role", role
            );

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );

        }
    }    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody AuthRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Username cannot be empty"
                    ));
        }

        if (request.getPassword() == null ||
                request.getPassword().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Password cannot be empty"
                    ));
        }

        String username = request.getUsername().trim();

        if (userRepository.findByUsername(username).isPresent()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Username already exists"
                    ));
        }

        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(newUser);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "User registered successfully"
                )
        );
    }    @Data
    static class AuthRequest {

        private String username;

        private String password;

    }

}