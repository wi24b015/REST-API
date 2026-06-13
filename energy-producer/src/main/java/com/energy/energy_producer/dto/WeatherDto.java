package com.energy.energy_producer.dto;

public class WeatherDto {

    private final int cloudCover;
    private final boolean day;

    public WeatherDto(int cloudCover, boolean day) {
        this.cloudCover = cloudCover;
        this.day = day;
    }

    public int getCloudCover() {
        return cloudCover;
    }

    public boolean isDay() {
        return day;
    }
}