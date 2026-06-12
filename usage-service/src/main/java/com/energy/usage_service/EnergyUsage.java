package com.energy.usage_service;

import jakarta.persistence.*;

@Entity
@Table(name = "energy_usage")
public class EnergyUsage {

    @Id
    @Column(length = 19)
    private String hour;

    @Column(name = "community_produced")
    private double communityProduced;

    @Column(name = "community_used")
    private double communityUsed;

    @Column(name = "grid_used")
    private double gridUsed;

    public EnergyUsage() {
    }

    public EnergyUsage(String hour) {
        this.hour = hour;
        this.communityProduced = 0.0;
        this.communityUsed = 0.0;
        this.gridUsed = 0.0;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public double getCommunityProduced() {
        return communityProduced;
    }

    public void setCommunityProduced(double communityProduced) {
        this.communityProduced = communityProduced;
    }

    public double getCommunityUsed() {
        return communityUsed;
    }

    public void setCommunityUsed(double communityUsed) {
        this.communityUsed = communityUsed;
    }

    public double getGridUsed() {
        return gridUsed;
    }

    public void setGridUsed(double gridUsed) {
        this.gridUsed = gridUsed;
    }
}

