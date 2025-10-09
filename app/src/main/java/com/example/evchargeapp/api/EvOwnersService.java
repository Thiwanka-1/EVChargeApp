package com.example.evchargeapp.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import com.example.evchargeapp.models.OwnerDto;

public interface EvOwnersService {
    @GET("api/evowners/{nic}")
    Call<OwnerDto> getByNic(@Path("nic") String nic);
}
