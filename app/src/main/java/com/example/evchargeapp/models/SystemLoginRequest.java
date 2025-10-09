package com.example.evchargeapp.models;

public class SystemLoginRequest {
    public String username;
    public String password;
    public SystemLoginRequest(String username, String password) {
        this.username = username; this.password = password;
    }
}
