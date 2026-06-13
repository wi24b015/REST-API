package com.energy.energy_producer;

import com.energy.energy_producer.dto.WeatherDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    @Value("${weather.latitude}")
    private double latitude;

    @Value("${weather.longitude}")
    private double longitude;

    public WeatherDto getCurrentWeather() {
        try {
            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=cloud_cover,is_day";

            RestTemplate restTemplate = new RestTemplate();

            String json = restTemplate.getForObject(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode current = objectMapper.readTree(json).get("current");

            int cloudCover = current.get("cloud_cover").asInt();
            boolean isDay = current.get("is_day").asInt() == 1;

            System.out.println("Weather API data: cloudCover=" + cloudCover + ", isDay=" + isDay);

            return new WeatherDto(cloudCover, isDay);

        } catch (Exception e) {
            System.out.println("Weather API not available, using fallback weather.");
            return new WeatherDto(50, true);
        }
    }
}