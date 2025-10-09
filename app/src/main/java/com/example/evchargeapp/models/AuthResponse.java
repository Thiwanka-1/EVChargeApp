package com.example.evchargeapp.models;

public class AuthResponse {
    public String accessToken;
    public String role;            // "Owner", "Operator", "Backoffice"
    public String expiresAtUtc;
}
