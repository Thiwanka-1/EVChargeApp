package com.example.evchargeapp.models;

public class BookingCreateRequest {
    public String stationId;
    public String startTimeUtc; // ISO string "yyyy-MM-dd'T'HH:mm:ss'Z'"
    public String endTimeUtc;   // ISO string
}
