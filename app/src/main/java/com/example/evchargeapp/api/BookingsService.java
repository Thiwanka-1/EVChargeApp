package com.example.evchargeapp.api;

import com.example.evchargeapp.models.BookingCreateRequest;
import com.example.evchargeapp.models.BookingDto;
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

    @POST("api/bookings")
    Call<BookingDto> create(@Body BookingCreateRequest body);

    @PUT("api/bookings/{id}")
    Call<Void> update(@Path("id") String id, @Body BookingUpdateRequest body);

    @DELETE("api/bookings/{id}")
    Call<Void> cancel(@Path("id") String id);

    // ---------- Availability (updated to dateLocal + tzOffsetMinutes) ----------
    @GET("api/bookings/station/{stationId}/availability")
    Call<AvailabilityResponse> availability(
            @Path("stationId") String stationId,
            @Query("dateLocal") String dateLocal,          // e.g. "2025-10-13"
            @Query("tzOffsetMinutes") int tzOffsetMinutes  // e.g. 330 for Sri Lanka
    );

    // Response DTO for availability
    class AvailabilityResponse {
        public String stationId;
        public String date; // yyyy-MM-dd
        public Integer tzOffsetMinutes; // nullable if server omits; harmless
        public java.util.List<Slot> availability;
        public static class Slot { public String time; public int availableSlots; } // time = "HH:mm" (LOCAL)
    }

    // Extra owner lists
    @GET("api/bookings/my/upcoming")
    Call<List<BookingDto>> getMyUpcoming();

    @GET("api/bookings/my/history")
    Call<List<BookingDto>> getMyHistory();

    // ---- Operator endpoints reused elsewhere ----
    @GET("api/bookings/station/{stationId}")
    Call<List<BookingDto>> getForStation(@Path("stationId") String stationId);

    @PATCH("api/bookings/{id}/approve")
    Call<ApproveResponse> approveOrReject(@Path("id") String id, @Body ApproveRequest body);

    @PATCH("api/bookings/{id}/start")
    Call<MessageResponse> start(@Path("id") String id, @Body StartRequest body);

    @PATCH("api/bookings/{id}/complete")
    Call<MessageResponse> complete(@Path("id") String id);

    // Small DTOs kept here for convenience
    class ApproveRequest { public boolean approve; public String reason; }
    class StartRequest { public String qrCode; }
    class ApproveResponse { public String id, status, qrCode, rejectionReason; }
    class MessageResponse { public String message; }

    // ---- (Some teams placed stations here—keep to avoid breaking imports) ----
    @GET("api/stations")
    Call<java.util.List<com.example.evchargeapp.models.StationDto>> getAll();
}
