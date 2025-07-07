package org.codeforcompassion.animalwelfare.repository;

import org.codeforcompassion.animalwelfare.model.FestivalTrendDTO;

import java.time.LocalDate;
import java.util.List;

public interface CustomInsightAggregationRepository {
    List<FestivalTrendDTO> aggregateFestivalTrends(LocalDate fromDate);
}
