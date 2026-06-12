package com.example.energyapi;

import com.example.energyapi.dto.CurrentEnergyDto;
import com.example.energyapi.dto.HistoricalEnergyDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/energy/current")
    public CurrentEnergyDto getCurrentEnergy() {
        return energyService.getCurrentEnergy();
    }

    @GetMapping("/energy/historical")
    public List<HistoricalEnergyDto> getHistoricalEnergy(
            @RequestParam String start,
            @RequestParam String end
    ) {
        return energyService.getHistoricalEnergy(
                parseStart(start),
                parseEnd(end)
        );
    }

    private LocalDateTime parseStart(String value) {
        return parseDateTimeOrDate(value);
    }

    private LocalDateTime parseEnd(String value) {
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }

        return LocalDate.parse(value).plusDays(1).atStartOfDay();
    }

    private LocalDateTime parseDateTimeOrDate(String value) {
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }

        return LocalDate.parse(value).atStartOfDay();
    }
}
