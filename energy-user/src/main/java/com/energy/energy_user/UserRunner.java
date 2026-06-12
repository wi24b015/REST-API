package com.energy.energy_user;

import com.energy.energy_user.dto.EnergyMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
@Profile("!test")
public class UserRunner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    public UserRunner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            double kwh = calculateUsageKwh();

            EnergyMessageDto message = new EnergyMessageDto(
                    "USER",
                    "COMMUNITY",
                    kwh,
                    LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.ENERGY_MESSAGES_QUEUE,
                    message
            );

            System.out.println("Sent user message: " + kwh + " kWh");

            int sleepSeconds = 1 + random.nextInt(5);
            Thread.sleep(sleepSeconds * 1000L);
        }
    }

    private double calculateUsageKwh() {
        int hour = LocalTime.now().getHour();

        if ((hour >= 6 && hour <= 9) || (hour >= 17 && hour <= 22)) {
            return 0.025 + random.nextDouble() * 0.035;
        }

        if (hour >= 0 && hour <= 5) {
            return 0.003 + random.nextDouble() * 0.010;
        }

        return 0.010 + random.nextDouble() * 0.020;
    }
}
