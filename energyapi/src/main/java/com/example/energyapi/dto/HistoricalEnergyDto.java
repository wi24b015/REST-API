package com.example.energyapi.dto;

public class HistoricalEnergyDto {

    public String hour;
    public double communityProduced;
    public double communityUsed;
    public double gridUsed;

    public HistoricalEnergyDto(String hour, double communityProduced, double communityUsed, double gridUsed) {
        this.hour = hour;
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
        this.gridUsed = gridUsed;
    }
}
