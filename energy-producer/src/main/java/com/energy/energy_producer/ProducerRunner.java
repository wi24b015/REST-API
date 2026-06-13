package com.energy.energy_producer;

import com.energy.energy_producer.dto.EnergyMessageDto;
import com.energy.energy_producer.dto.WeatherDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
@Profile("!test")
public class ProducerRunner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final WeatherService weatherService;
    private final Random random = new Random();

    public ProducerRunner(RabbitTemplate rabbitTemplate, WeatherService weatherService) {
        this.rabbitTemplate = rabbitTemplate;
        this.weatherService = weatherService;
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            double kwh = calculateProductionKwh();

            EnergyMessageDto message = new EnergyMessageDto(
                    "PRODUCER",
                    "COMMUNITY",
                    kwh,
                    LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.ENERGY_MESSAGES_QUEUE,
                    message
            );

            System.out.println("Sent producer message: " + message.getKwh()
                    + " kWh at " + message.getDatetime());

            int sleepSeconds = 1 + random.nextInt(5);
            Thread.sleep(sleepSeconds * 1000L);
        }
    }

    private double calculateProductionKwh() {
        try {
            WeatherDto weather = weatherService.getCurrentWeather();

            if (!weather.isDay()) {
                return 0.001 + random.nextDouble() * 0.004;
            }

            int cloudCover = weather.getCloudCover();

            if (cloudCover <= 25) {
                return 0.035 + random.nextDouble() * 0.025;
            }

            if (cloudCover <= 70) {
                return 0.015 + random.nextDouble() * 0.020;
            }

            return 0.003 + random.nextDouble() * 0.012;

        } catch (Exception e) {
            System.err.println("Weather API not available, using fallback production.");
            e.printStackTrace();
            return 0.01 + random.nextDouble() * 0.04;
        }
    }
}
