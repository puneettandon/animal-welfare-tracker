package org.codeforcompassion.animalwelfare.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FestivalTrendDTO {
    private String event;
    private int positive;
    private int negative;
}