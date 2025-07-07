
package org.codeforcompassion.animalwelfare.service;

import lombok.RequiredArgsConstructor;
import org.codeforcompassion.animalwelfare.model.FestivalTrendDTO;
import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.codeforcompassion.animalwelfare.repository.CachedAiInsightRepository;
import org.codeforcompassion.animalwelfare.scheduler.ArticleProcessorScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final CachedAiInsightRepository cachedAiInsightRepository;

    public List<FestivalTrendDTO> getFestivalTrends(String range) {
        LocalDate fromDate = switch (range.toLowerCase()) {
            case "month" -> LocalDate.now().minusMonths(1);
            case "3months" -> LocalDate.now().minusMonths(3);
            case "6months" -> LocalDate.now().minusMonths(6);
            case "year" -> LocalDate.now().minusYears(1);
            default -> LocalDate.now().minusMonths(6);
        };

        return cachedAiInsightRepository.aggregateFestivalTrends(fromDate);
    }
}
