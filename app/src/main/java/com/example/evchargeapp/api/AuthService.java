package com.example.evchargeapp.api;

import com.example.evchargeapp.models.AuthResponse;
import com.example.evchargeapp.models.OwnerLoginRequest;
import com.example.evchargeapp.models.OwnerRegisterRequest;
import com.example.evchargeapp.models.SystemLoginRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("api/auth/login")
    Call<AuthResponse> systemLogin(@Body SystemLoginRequest req);   // Operator/Backoffice

    @POST("api/auth/owner/login")
    Call<AuthResponse> ownerLogin(@Body OwnerLoginRequest req);

    @POST("api/auth/owner/register")
    Call<AuthResponse> ownerRegister(@Body OwnerRegisterRequest req);
}
