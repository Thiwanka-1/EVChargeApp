package com.example.evchargeapp.api;

import com.example.evchargeapp.models.OwnerDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EvOwnersService {

    @GET("api/evowners/{nic}")
    Call<OwnerDto> getByNic(@Path("nic") String nic);

    // Update basic profile fields
    @PUT("api/evowners/{nic}")
    Call<Void> update(@Path("nic") String nic, @Body OwnerDto body);

    // Activate/Deactivate
    @PATCH("api/evowners/{nic}/status")
    Call<Map<String,String>> setActive(@Path("nic") String nic, @Query("isActive") boolean isActive);
}
