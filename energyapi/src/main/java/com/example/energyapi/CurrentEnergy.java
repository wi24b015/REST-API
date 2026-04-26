package com.example.energyapi;

public class CurrentEnergy {
    public String hour;
    public double communityDepleted;
    public double gridPortion;

    public CurrentEnergy(String hour, double communityDepleted, double gridPortion) {
        this.hour = hour;
        this.communityDepleted = communityDepleted;
        this.gridPortion = gridPortion;
    }
}