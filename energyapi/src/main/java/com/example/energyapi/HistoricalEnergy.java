package com.example.energyapi;

public class HistoricalEnergy {
    public String hour;
    public double communityProduced;
    public double communityUsed;
    public double gridUsed;

    public HistoricalEnergy(String hour, double communityProduced, double communityUsed, double gridUsed) {
        this.hour = hour;
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
        this.gridUsed = gridUsed;
    }
}