package com.example.energyapi;

import com.example.energyapi.dto.CurrentEnergyDto;
import com.example.energyapi.dto.HistoricalEnergyDto;
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

    public CurrentEnergyDto getCurrentEnergy() {
        return currentPercentageRepository.findTopByOrderByHourDesc()
                .map(current -> new CurrentEnergyDto(
                        current.getHour(),
                        current.getCommunityDepleted(),
                        current.getGridPortion()
                ))
                .orElseGet(() -> new CurrentEnergyDto(
                        LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toString(),
                        0.0,
                        0.0
                ));
    }

    public List<HistoricalEnergyDto> getHistoricalEnergy(LocalDateTime start, LocalDateTime end) {
        return energyUsageRepository
                .findByHourBetweenOrderByHourAsc(start.toString(), end.toString())
                .stream()
                .map(usage -> new HistoricalEnergyDto(
                        usage.getHour(),
                        usage.getCommunityProduced(),
                        usage.getCommunityUsed(),
                        usage.getGridUsed()
                ))
                .toList();
    }
}
