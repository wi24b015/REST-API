package com.energy.current_percentage_service;

import com.energy.current_percentage_service.dto.EnergyUpdateDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PercentageListener {

    private final EnergyUsageRepository usageRepository;
    private final CurrentPercentageRepository percentageRepository;

    public PercentageListener(EnergyUsageRepository usageRepository,
                              CurrentPercentageRepository percentageRepository) {
        this.usageRepository = usageRepository;
        this.percentageRepository = percentageRepository;
    }

    @RabbitListener(queues = RabbitConfig.ENERGY_UPDATES_QUEUE)
    public void handleUpdateMessage(EnergyUpdateDto updateMessage) {
        try {
            if (updateMessage == null || updateMessage.getDatetime() == null) {
                System.err.println("Ignored update message without datetime.");
                return;
            }

            String hour = normalizeHour(updateMessage.getDatetime());

            EnergyUsage usage = usageRepository.findById(hour)
                    .orElse(null);

            if (usage == null) {
                System.err.println("No energy_usage entry found for hour: " + hour);
                return;
            }

            CurrentPercentage currentPercentage = calculatePercentage(usage);

            // Specification: current_percentage table only contains current hour data.
            percentageRepository.deleteAll();
            percentageRepository.save(currentPercentage);

            System.out.println("Updated current percentage for " + hour
                    + ": community_depleted=" + currentPercentage.getCommunityDepleted()
                    + ", grid_portion=" + currentPercentage.getGridPortion());

        } catch (Exception e) {
            System.err.println("Error processing percentage update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private CurrentPercentage calculatePercentage(EnergyUsage usage) {
        double communityDepleted = 0.0;
        if (usage.getCommunityProduced() > 0) {
            communityDepleted = usage.getCommunityUsed() / usage.getCommunityProduced() * 100.0;
            communityDepleted = Math.min(communityDepleted, 100.0);
        }

        double totalUsed = usage.getCommunityUsed() + usage.getGridUsed();
        double gridPortion = 0.0;
        if (totalUsed > 0) {
            gridPortion = usage.getGridUsed() / totalUsed * 100.0;
        }

        return new CurrentPercentage(
                usage.getHour(),
                roundTwoDecimals(communityDepleted),
                roundTwoDecimals(gridPortion)
        );
    }

    private String normalizeHour(String datetime) {
        if (datetime.length() >= 13) {
            return datetime.substring(0, 13) + ":00:00";
        }
        return datetime;
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
