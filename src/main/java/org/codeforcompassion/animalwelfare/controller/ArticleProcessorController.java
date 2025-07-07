package org.codeforcompassion.animalwelfare.controller;

import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.codeforcompassion.animalwelfare.scheduler.ArticleProcessorScheduler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/articles")
public class ArticleProcessorController {

    private final ArticleProcessorScheduler scheduler;

    public ArticleProcessorController(ArticleProcessorScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/process")
    public ResponseEntity<String> triggerProcessing(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(defaultValue = "1000") int totalLimit,
            @RequestParam(defaultValue = "100") int perFeedLimit
    ) {
        if (fromDate == null) {
            // Default to 3 years ago for historical data fetch
            fromDate = LocalDateTime.now().minusYears(3);
        }

        scheduler.fetchAndProcessArticles(totalLimit, perFeedLimit, fromDate);
        return ResponseEntity.ok("Historical article fetch triggered from: " + fromDate);
    }

    @GetMapping
    public ResponseEntity<List<ArticleDTO>> getArticles() {
        return ResponseEntity.ok(scheduler.getProcessedArticles());
    }

}
