package com.energy.usage_service.dto;

import java.io.Serializable;

public class EnergyUpdateDto implements Serializable {

    private String type;
    private String association;
    private double kwh;
    private String datetime;

    public EnergyUpdateDto() {
    }

    public EnergyUpdateDto(String type, String association, double kwh, String datetime) {
        this.type = type;
        this.association = association;
        this.kwh = kwh;
        this.datetime = datetime;
    }

    public String getType() {
        return type;
    }

    public String getAssociation() {
        return association;
    }

    public double getKwh() {
        return kwh;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAssociation(String association) {
        this.association = association;
    }

    public void setKwh(double kwh) {
        this.kwh = kwh;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
