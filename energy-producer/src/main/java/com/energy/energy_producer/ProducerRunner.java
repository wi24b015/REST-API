package com.energy.energy_producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Component
public class ProducerRunner implements CommandLineRunner {

    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    public ProducerRunner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            double kwh = 0.01 + random.nextDouble() * 0.04;

            EnergyMessage message = new EnergyMessage(
                    "PRODUCER",
                    "COMMUNITY",
                    kwh,
                    LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).toString()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.ENERGY_MESSAGES_QUEUE,
                    message
            );

            System.out.println("Sent producer message: " + kwh + " kWh");

            int sleepSeconds = 1 + random.nextInt(5);
            Thread.sleep(sleepSeconds * 1000L);
        }
    }
}