package com.example.energyapi;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EnergyService {

    private final CurrentPercentageRepository currentPercentageRepository;
    private final EnergyUsageRepository energyUsageRepository;

    public EnergyService(CurrentPercentageRepository currentPercentageRepository,
                         EnergyUsageRepository energyUsageRepository) {
        this.currentPercentageRepository = currentPercentageRepository;
        this.energyUsageRepository = energyUsageRepository;
    }

    public CurrentEnergy getCurrentEnergy() {
        return currentPercentageRepository.findTopByOrderByHourDesc()
                .map(current -> new CurrentEnergy(
                        current.getHour(),
                        current.getCommunityDepleted(),
                        current.getGridPortion()
                ))
                .orElseGet(() -> new CurrentEnergy(
                        LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toString(),
                        0.0,
                        0.0
                ));
    }

    public List<HistoricalEnergy> getHistoricalEnergy(LocalDateTime start, LocalDateTime end) {
        return energyUsageRepository
                .findByHourBetweenOrderByHourAsc(start.toString(), end.toString())
                .stream()
                .map(usage -> new HistoricalEnergy(
                        usage.getHour(),
                        usage.getCommunityProduced(),
                        usage.getCommunityUsed(),
                        usage.getGridUsed()
                ))
                .toList();
    }
}