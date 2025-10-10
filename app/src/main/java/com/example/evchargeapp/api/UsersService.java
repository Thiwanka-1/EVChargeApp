package com.example.evchargeapp.api;

import com.example.evchargeapp.models.UserDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UsersService {

    @GET("api/users/{id}")
    Call<UserDto> getById(@Path("id") String id);

    @PUT("api/users/{id}")
    Call<Void> update(@Path("id") String id, @Body UserUpdate body);

    @PATCH("api/users/{id}/status")
    Call<Map<String, String>> setActive(@Path("id") String id, @Query("isActive") boolean isActive);

    // --- DTOs used by this interface ---
    class UserUpdate {
        public String id;
        public String username;
        /** Send plaintext; backend hashes it (UsersController.Update) */
        public String passwordHash;
        public String role;
        public boolean isActive;
    }
}
