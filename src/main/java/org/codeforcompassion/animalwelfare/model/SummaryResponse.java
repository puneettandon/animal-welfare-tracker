package org.codeforcompassion.animalwelfare.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SummaryResponse {
    private long positive;
    private long negative;
    private Map<String, Long> themeCounts;
    private Map<String, Long> locationCounts;
}
