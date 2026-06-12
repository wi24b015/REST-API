package com.energy.current_percentage_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "current_percentage")
public class CurrentPercentage {

    @Id
    @Column(length = 19)
    private String hour;

    @Column(name = "community_depleted")
    private double communityDepleted;

    @Column(name = "grid_portion")
    private double gridPortion;

    public CurrentPercentage() {
    }

    public CurrentPercentage(String hour, double communityDepleted, double gridPortion) {
        this.hour = hour;
        this.communityDepleted = communityDepleted;
        this.gridPortion = gridPortion;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public double getCommunityDepleted() {
        return communityDepleted;
    }

    public void setCommunityDepleted(double communityDepleted) {
        this.communityDepleted = communityDepleted;
    }

    public double getGridPortion() {
        return gridPortion;
    }

    public void setGridPortion(double gridPortion) {
        this.gridPortion = gridPortion;
    }
}
