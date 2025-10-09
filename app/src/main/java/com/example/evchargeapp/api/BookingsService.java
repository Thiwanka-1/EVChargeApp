package com.example.evchargeapp.api;

import com.example.evchargeapp.models.BookingDto;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface BookingsService {
    @GET("api/bookings/my")
    Call<List<BookingDto>> getMyBookings();

    @GET("api/bookings/my/upcoming")
    Call<List<BookingDto>> getMyUpcoming();

    @GET("api/bookings/my/history")
    Call<List<BookingDto>> getMyHistory();
}
