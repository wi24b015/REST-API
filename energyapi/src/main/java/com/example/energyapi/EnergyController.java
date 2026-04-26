package com.example.energyapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
public class EnergyController {

    @GetMapping("/energy/current")
    public CurrentEnergy getCurrentEnergy() {
        return new CurrentEnergy("2025-01-10T14:00:00", 78.54, 7.23);
    }

    @GetMapping("/energy/historical")
    public List<HistoricalEnergy> getHistoricalEnergy(
            @RequestParam String start,
            @RequestParam String end
    ) {
        return List.of(
                new HistoricalEnergy("2025-01-10T14:00:00", 143.024, 130.101, 14.75),
                new HistoricalEnergy("2025-01-10T15:00:00", 120.500, 110.250, 9.80)
        );
    }
}