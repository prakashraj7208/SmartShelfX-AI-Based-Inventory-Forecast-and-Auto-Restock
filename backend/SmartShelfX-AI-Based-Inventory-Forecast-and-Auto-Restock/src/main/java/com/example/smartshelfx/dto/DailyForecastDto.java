package com.example.smartshelfx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyForecastDto {
    private LocalDate date;
    private Integer predicted;
}
