package com.example.evchargeapp.models;

public class BookingDto {
    public String id;
    public String ownerNic;
    public String stationId;
    public String status; // Pending, Approved, ...
    public String startTimeUtc; // ISO string
    public String endTimeUtc;   // ISO string
    public String qrCode;
    public String createdUtc;
    public String updatedUtc;
    public String rejectionReason;
    // in BookingDto.java
    public transient String __stationName;

}
