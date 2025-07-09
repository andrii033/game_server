package com.example.game_server.auth;

import com.example.game_server.model.AuthRequest;
import com.example.game_server.model.AuthResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest authRequest) {
        users.put(authRequest.getUsername(), authRequest.getPassword());
        String token = jwtUtil.generateToken(authRequest.getUsername());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        String stored = users.get(request.getUsername());
        if (stored == null || !stored.equals(request.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(request.getUsername());
        return new AuthResponse(token);
    }
}
