package com.energy.usage_service;

import com.energy.usage_service.dto.EnergyMessageDto;
import com.energy.usage_service.dto.EnergyUpdateDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class MessageListener {

    private final EnergyUsageRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public MessageListener(EnergyUsageRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.ENERGY_MESSAGES_QUEUE)
    public void handleEnergyMessage(EnergyMessageDto message) {
        try {
            // Extract hour from datetime
            String hourKey = message.getDatetime().substring(0, 13) + ":00:00";

            // Load or create energy usage record for this hour
            EnergyUsage usage = repository.findById(hourKey)
                    .orElseGet(() -> new EnergyUsage(hourKey));

            // Update based on message type
            if ("PRODUCER".equals(message.getType())) {
                usage.setCommunityProduced(usage.getCommunityProduced() + message.getKwh());
            } else if ("USER".equals(message.getType())) {
                double available = Math.max(0, usage.getCommunityProduced() - usage.getCommunityUsed());

                if (available >= message.getKwh()) {
                    // Use community energy
                    usage.setCommunityUsed(usage.getCommunityUsed() + message.getKwh());
                } else {
                    // Use remaining community energy and grid
                    usage.setCommunityUsed(usage.getCommunityUsed() + available);
                    usage.setGridUsed(usage.getGridUsed() + (message.getKwh() - available));
                }
            }

            // Save to database
            EnergyUsage saved = repository.save(usage);

            // Send update message to queue
            sendUpdateMessage(saved);

            System.out.println("Processed message: " + message.getType() + " - " + message.getKwh() + " kWh at " + hourKey);

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendUpdateMessage(EnergyUsage usage) {
        EnergyUpdateDto updateMessage = new EnergyUpdateDto(
                "UPDATE",
                "COMMUNITY",
                0.0,
                usage.getHour()
        );

        rabbitTemplate.convertAndSend(
                RabbitConfig.ENERGY_UPDATES_QUEUE,
                updateMessage
        );
    }
}


