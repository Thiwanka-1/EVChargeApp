package com.example.evchargeapp.api;

import com.example.evchargeapp.models.StationDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface StationsService {
    // assuming GET /api/stations returns all stations
    @GET("api/stations")
    Call<List<StationDto>> getAll();
}
