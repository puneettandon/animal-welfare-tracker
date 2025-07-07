package org.codeforcompassion.animalwelfare.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.codeforcompassion.animalwelfare.config.FeedConfig;
import org.codeforcompassion.animalwelfare.config.FetchLimitConfig;
import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.codeforcompassion.animalwelfare.service.GoogleSheetService;
import org.codeforcompassion.animalwelfare.service.OpenAiService;
import org.codeforcompassion.animalwelfare.service.RssFeedService;
import org.codeforcompassion.animalwelfare.util.UrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class ArticleProcessorScheduler {

    private final RssFeedService rssFeedService;
    private final OpenAiService openAiService;
    private final GoogleSheetService googleSheetService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FeedConfig feedConfig;

    @Autowired
    private  FetchLimitConfig fetchLimitConfig;


    private final List<ArticleDTO> processedArticles = new CopyOnWriteArrayList<>();



    public ArticleProcessorScheduler(RssFeedService rssFeedService,
                                     OpenAiService openAiService,
                                     GoogleSheetService googleSheetService,
                                     RestTemplate restTemplate,
                                     FeedConfig feedConfig,
                                     FetchLimitConfig fetchLimitConfig) {
        this.rssFeedService = rssFeedService;
        this.openAiService = openAiService;
        this.googleSheetService = googleSheetService;
        this.restTemplate = restTemplate;
        this.feedConfig = feedConfig;
        this.fetchLimitConfig = fetchLimitConfig;
    }


    @Scheduled(cron = "0 0 8 * * *") // Every day at 8:00 AM
    public void scheduledFetchAndProcess() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(1);
        fetchAndProcessArticles(fetchLimitConfig.getTotal(), fetchLimitConfig.getPerfeed(), fromDate);
    }


    // Manually callable with full control
    public void fetchAndProcessArticles(int totalLimit, int perFeedLimit, LocalDateTime fromDate) {
        int totalProcessed = 0;
        try {
            for(String feedUrl : feedConfig.getFeedUrls()) {
                if (totalProcessed >= totalLimit) break;
                List<ArticleDTO> articles = rssFeedService.fetchArticles(feedUrl);
                log.info("Articles list size: {} for feedUrl: {} ", articles.size(), feedUrl);
                int perFeedProcessed = 0;
                for (ArticleDTO article : articles) {
                    if (totalProcessed >= totalLimit || perFeedProcessed >= perFeedLimit) break;

                    String normalizedUrl = UrlUtils.normalize(article.getUrl());
                    boolean alreadyExists = rssFeedService.articleExistsInMongoWithUrlOrTitle(normalizedUrl,article);
                    if (alreadyExists) {
                        continue;
                    }

                    article.setUrl(normalizedUrl);

                    // Filter based on publish date
                    if (article.getPublishedDate() != null && fromDate != null &&
                            article.getPublishedDate().isBefore(fromDate.toLocalDate())) {
                        log.info("Skipping old article: {}", article.getTitle());
                        continue;
                    }


                    if (openAiService.isAnimalWelfareRelevant(article)) {
                        openAiService.enrichArticleWithInsights(article);

                        googleSheetService.appendArticle(article);

                        processedArticles.add(article);
                        totalProcessed++;
                        perFeedProcessed++;
                    } else {
                        log.info("Skipped (not relevant): {}", article.getTitle());
                    }
                }
            }
            log.info("Processed {} new articles. Seen URLs: {}", totalProcessed);
        } catch (Exception ex) {
            log.error("Error during article processing: {}", ex.getMessage(), ex);
            ex.printStackTrace();
        }
    }


    public List<ArticleDTO> getProcessedArticles() {
        return Collections.unmodifiableList(processedArticles);
    }

}
