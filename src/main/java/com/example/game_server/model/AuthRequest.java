package com.example.game_server.model;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
