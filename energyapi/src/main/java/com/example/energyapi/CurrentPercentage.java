package com.example.energyapi;

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

    public String getHour() {
        return hour;
    }

    public double getCommunityDepleted() {
        return communityDepleted;
    }

    public double getGridPortion() {
        return gridPortion;
    }
}
