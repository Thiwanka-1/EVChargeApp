package com.example.evchargeapp.api;

import com.example.evchargeapp.models.OwnerDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EvOwnersService {

    @GET("api/evowners/{nic}")
    Call<OwnerDto> getByNic(@Path("nic") String nic);

    @PUT("api/evowners/{nic}")
    Call<Void> update(@Path("nic") String nic, @Body OwnerDto body);

    // owners can only deactivate themselves; backoffice can reactivate anyone
    @PATCH("api/evowners/{nic}/status")
    Call<Map<String,String>> setActive(@Path("nic") String nic, @Query("isActive") boolean isActive);

    @DELETE("api/evowners/{nic}")
    Call<Void> delete(@Path("nic") String nic);

    // ---- password change (server patch included below) ----
    class ChangePasswordRequest { public String currentPassword; public String newPassword; }
    class MessageResponse { public String message; }

    @PATCH("api/evowners/{nic}/password")
    Call<MessageResponse> changePassword(@Path("nic") String nic, @Body ChangePasswordRequest body);
}
