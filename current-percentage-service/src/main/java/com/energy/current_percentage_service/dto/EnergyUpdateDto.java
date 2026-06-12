package com.energy.current_percentage_service.dto;

import java.io.Serializable;

public class EnergyUpdateDto implements Serializable {

    private String type;
    private String association;
    private double kwh;
    private String datetime;

    public EnergyUpdateDto() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssociation() {
        return association;
    }

    public void setAssociation(String association) {
        this.association = association;
    }

    public double getKwh() {
        return kwh;
    }

    public void setKwh(double kwh) {
        this.kwh = kwh;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
