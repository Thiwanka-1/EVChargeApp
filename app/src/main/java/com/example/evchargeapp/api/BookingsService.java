package com.example.evchargeapp.api;

import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.BookingCreateRequest;
import com.example.evchargeapp.models.BookingUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookingsService {
    @GET("api/bookings/my")
    Call<List<BookingDto>> getMyBookings();

    @GET("api/bookings/{id}")
    Call<BookingDto> getById(@Path("id") String id);

    // for later (create/edit pages)
    @POST("api/bookings")
    Call<BookingDto> create(@Body BookingCreateRequest body);

    @PUT("api/bookings/{id}")
    Call<Void> update(@Path("id") String id, @Body BookingUpdateRequest body);

    @DELETE("api/bookings/{id}")
    Call<Void> cancel(@Path("id") String id);

    // Availability hint (date format "yyyy-MM-dd")
    @GET("api/bookings/station/{stationId}/availability")
    Call<AvailabilityResponse> availability(
            @Path("stationId") String stationId,
            @Query("date") String date
    );

    // ---- models for availability ----
    class AvailabilityResponse {
        public String stationId;
        public String date;
        public java.util.List<Slot> availability;
        public static class Slot { public String time; public int availableSlots; }
    }
    @GET("api/bookings/my/upcoming")
    Call<List<BookingDto>> getMyUpcoming();

    @GET("api/bookings/my/history")
    Call<List<BookingDto>> getMyHistory();

    // BookingsService.java
    @GET("api/bookings/station/{stationId}")
    Call<List<BookingDto>> getForStation(@Path("stationId") String stationId);

    @PATCH("api/bookings/{id}/approve")
    Call<ApproveResponse> approveOrReject(@Path("id") String id, @Body ApproveRequest body);

    @PATCH("api/bookings/{id}/start")
    Call<MessageResponse> start(@Path("id") String id, @Body StartRequest body);

    @PATCH("api/bookings/{id}/complete")
    Call<MessageResponse> complete(@Path("id") String id);

    // DTOs
    public class ApproveRequest { public boolean approve; public String reason; }
    public class StartRequest { public String qrCode; }
    public class ApproveResponse { public String id, status, qrCode, rejectionReason; }
    public class MessageResponse { public String message; }

    @GET("api/stations")
    Call<java.util.List<com.example.evchargeapp.models.StationDto>> getAll();

}
