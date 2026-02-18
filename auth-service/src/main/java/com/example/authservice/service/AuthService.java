package com.example.authservice.service;

import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.UserEntity;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest req) {
        if(userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username already taken");
        UserEntity u = UserEntity.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(u);
    }

    public AuthResponse login(AuthRequest req) {
        UserEntity user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token);
    }
}
