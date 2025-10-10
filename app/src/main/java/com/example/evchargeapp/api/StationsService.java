package com.example.evchargeapp.api;

import com.example.evchargeapp.models.StationDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StationsService {

    @GET("api/Stations")
    Call<List<StationDto>> getAll();

    @GET("api/Stations/{stationId}")
    Call<StationDto> getById(@Path("stationId") String stationId);

    // Operator: update only availableSlots
    @PATCH("api/Stations/{stationId}/slots")
    Call<Map<String,String>> updateSlots(
            @Path("stationId") String stationId,
            @Query("availableSlots") int availableSlots
    );
}
