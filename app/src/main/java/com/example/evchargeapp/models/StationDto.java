package com.example.evchargeapp.models;

import java.util.List;

public class StationDto {
    public String stationId;
    public String name;
    public double latitude;
    public double longitude;
    public String address;
    public String type; // AC / DC
    public int availableSlots;
    public boolean isActive;
    public List<String> operatorUserIds;
}
