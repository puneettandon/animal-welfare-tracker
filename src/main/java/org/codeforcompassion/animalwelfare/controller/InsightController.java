package org.codeforcompassion.animalwelfare.controller;

import lombok.RequiredArgsConstructor;
import org.codeforcompassion.animalwelfare.model.CachedAiInsight;
import org.codeforcompassion.animalwelfare.model.FestivalTrendDTO;
import org.codeforcompassion.animalwelfare.repository.CachedAiInsightRepository;
import org.codeforcompassion.animalwelfare.service.InsightService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final CachedAiInsightRepository cachedAiInsightRepository;

    private final InsightService insightService;

    @GetMapping
    public List<CachedAiInsight> getInsightsByTimeRange(
            @RequestParam(defaultValue = "all") String range // week, month, year, all
    ) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = switch (range.toLowerCase()) {
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusMonths(3); // fetch all
        };
        return cachedAiInsightRepository.findByPublishedDateAfter(fromDate);
    }

    @GetMapping("/by-date")
    public List<CachedAiInsight> getInsightsBetweenDates(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return cachedAiInsightRepository.findByPublishedDateBetween(from, to);
    }

    @GetMapping("/filter")
    public Page<CachedAiInsight> filterInsights(
            @RequestParam(defaultValue = "all") String range,
            @RequestParam(defaultValue = "") String sentiment,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String theme,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = switch (range.toLowerCase()) {
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusMonths(6); // allow all
        };

        List<CachedAiInsight> filtered = cachedAiInsightRepository.findByPublishedDateAfter(fromDate)
                .stream()
                .filter(i -> sentiment.isBlank() || sentiment.equalsIgnoreCase(i.getSentiment()))
                .filter(i -> location.isBlank() || i.getLocation() != null && i.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(i -> theme.isBlank() || i.getThemes() != null && i.getThemes().stream().anyMatch(t -> t.toLowerCase().contains(theme.toLowerCase())))
                .sorted(Comparator.comparing(CachedAiInsight::getPublishedDate).reversed())
                .toList();

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());

        return new PageImpl<>(
                filtered.subList(start, end),
                PageRequest.of(page, size),
                filtered.size()
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummaryStats(
            @RequestParam(defaultValue = "all") String range
    ) {
        System.out.println("Summary API Called");
        LocalDate now = LocalDate.now();
        LocalDate fromDate;
        String rangeLabel;

        switch (range.toLowerCase()) {
            case "week" -> {
                fromDate = now.minusWeeks(1);
                rangeLabel = "Last 1 Week";
            }
            case "month" -> {
                fromDate = now.minusMonths(1);
                rangeLabel = "Last 1 Month";
            }
            case "year" -> {
                fromDate = now.minusYears(1);
                rangeLabel = "Last 1 Year";
            }
            case "all" -> {
                fromDate = now.minusYears(3);
                rangeLabel = "Last 3 Years";
            }
            default -> {
                fromDate = now.minusMonths(6);
                rangeLabel = "Last 6 Months";
            }
        }

        List<CachedAiInsight> insights = cachedAiInsightRepository.findByPublishedDateAfter(fromDate);

        long positive = insights.stream().filter(i -> "POSITIVE".equalsIgnoreCase(i.getSentiment())).count();
        long negative = insights.stream().filter(i -> "NEGATIVE".equalsIgnoreCase(i.getSentiment())).count();

        Map<String, Long> themeCounts = insights.stream()
                .flatMap(i -> i.getThemes() != null ? i.getThemes().stream() : Stream.empty())
                .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()));

        Map<String, Long> locationCounts = insights.stream()
                .filter(i -> i.getLocation() != null)
                .collect(Collectors.groupingBy(i -> i.getLocation().toLowerCase(), Collectors.counting()));

        Map<String, Object> response = new HashMap<>();
        response.put("positive", positive);
        response.put("negative", negative);
        response.put("themeCounts", themeCounts);
        response.put("locationCounts", locationCounts);
        response.put("rangeLabel", rangeLabel);  // ✅ Add this line

        return response;
    }


    @GetMapping("/festival-summary")
    public Map<String, Object> getFestivalTrends(@RequestParam(defaultValue = "6months") String range) {
        List<FestivalTrendDTO> festivalTrendList = insightService.getFestivalTrends(range);

        String label = switch (range.toLowerCase()) {
            case "year" -> "Last 1 Year";
            case "3months" -> "Last 3 Months";
            case "month" -> "Last 1 Month";
            default -> "Last 6 Months";
        };

        Map<String, Object> response = new HashMap<>();
        response.put("data", festivalTrendList);
        response.put("rangeLabel", label);
        return response;
    }

    @GetMapping({"/", "/{path:^(?!api|actuator|admin|images|css|js|static|docs|swagger-ui).*$}"})
    public String forwardToFrontend() {
        return "forward:/index.html";
    }

}
