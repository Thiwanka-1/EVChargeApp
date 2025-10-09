package com.example.evchargeapp.models;

public class OwnerLoginRequest {
    public String email;
    public String password;
    public OwnerLoginRequest(String email, String password) {
        this.email = email; this.password = password;
    }
}
