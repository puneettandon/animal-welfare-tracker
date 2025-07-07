package org.codeforcompassion.animalwelfare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.codeforcompassion.animalwelfare.model.CachedAiInsight;
import org.codeforcompassion.animalwelfare.repository.CachedAiInsightRepository;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OpenAiService {

    @Autowired
    private CachedAiInsightRepository cacheRepo;

    @Value("${openrouter.api.key}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isAnimalWelfareRelevant(ArticleDTO article) throws IOException {
        // Check cache first
        Optional<CachedAiInsight> cached = cacheRepo.findByUrl(article.getUrl());
        if (cached.isPresent()) {
            log.info("Relevance check (from cache) for: {}", article.getTitle());
            // If we trust cached sentiment means relevance was true earlier
            return true;
        }

        String prompt = "Is the following news article about animal welfare in India? Answer YES or NO.\n\n" +
                "Title: " + article.getTitle() + "\n" +
                "Summary: " + article.getSummary();

        String responseText = callOpenAi(prompt);
        return responseText.trim().toUpperCase().startsWith("YES");
    }

    public void enrichArticleWithInsights(ArticleDTO article) throws IOException {
        Optional<CachedAiInsight> cachedOpt = cacheRepo.findByUrl(article.getUrl());
        if (cachedOpt.isPresent()) {
            CachedAiInsight cached = cachedOpt.get();
            log.info("Using cached AI insight for: {}", article.getTitle());

            article.setSentiment(cached.getSentiment());
            article.setThemes(cached.getThemes().toArray(String[]::new));
            article.setTone(cached.getTone());
            article.setAuthorities(cached.getAuthorities());
            article.setLocation(cached.getLocation());
            article.setFestivalLinked(cached.isFestivalLinked());
            article.setFestivalName(cached.getFestivalName());
            return;
        }
        log.info("Calling OpenAI for article: {}", article.getTitle());

        String prompt = """
        You are an AI assistant analyzing Indian news articles related to animal welfare.
        Your task is to extract structured insights based on the content. Focus on the **perspective toward animal welfare**, **harm/cruelty**, or **protection efforts**.
        Based on the article title and summary, extract the following in JSON format:
        {
          "sentiment": "POSITIVE | NEGATIVE | NEUTRAL",
          "themes": ["theme1", "theme2", "theme3"],
          "tone": "Informative | Alarming | Supportive | Critical | Neutral",
          "authorities": ["Police", "Court", "NGO", ...]  // or empty list
          "location": "City or state mentioned in the article, or 'Unknown'"
        }

        Title: %s

        Summary: %s
        """.formatted(article.getTitle(), article.getSummary());

        String response = callOpenAi(prompt);
        JsonNode root = mapper.readTree(response);
        article.setSentiment(root.get("sentiment").asText());
        article.setThemes(mapper.convertValue(root.get("themes"), String[].class));
        article.setTone(root.get("tone").asText());
        article.setAuthorities(mapper.convertValue(root.get("authorities"), List.class));
        article.setLocation(root.get("location").asText());

        boolean isLinked = isFestivalRelated(article);
        article.setFestivalLinked(isLinked);

        // Save to cache
        CachedAiInsight insight = CachedAiInsight.builder()
                .url(article.getUrl())
                .title(article.getTitle())
                .summary(article.getSummary())
                .publishedDate(article.getPublishedDate())
                .sentiment(article.getSentiment())
                .themes(List.of(article.getThemes()))
                .tone(article.getTone())
                .authorities(article.getAuthorities())
                .location(article.getLocation())
                .festivalLinked(article.isFestivalLinked())
                .festivalName(article.getFestivalName())
                .cachedAt(LocalDateTime.now())
                .build();
        cacheRepo.save(insight);
        log.info("Cached AI insight for: {}", article.getTitle());
    }


    private String callOpenAi(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        MediaType mediaType = MediaType.parse("application/json");

        // Build request payload
        String jsonBody = mapper.writeValueAsString(Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        ));

        RequestBody body = RequestBody.create(jsonBody, mediaType);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseBody);
            return jsonNode.get("choices").get(0).get("message").get("content").asText();
        }
    }

    public boolean isFestivalRelated(ArticleDTO article) {
        try {
            Optional<CachedAiInsight> cached = cacheRepo.findByUrl(article.getUrl());
            if (cached.isPresent()) {
                log.info("Festival check (from cache) for: {}", article.getTitle());
                article.setFestivalLinked(cached.get().isFestivalLinked());
                article.setFestivalName(cached.get().getFestivalName());
                return cached.get().isFestivalLinked();
            }
            String prompt = """
                You are an AI assistant analyzing Indian news articles.
                
                Determine if the article is related to **any festival or cultural/religious event**, including those that involve **controversial practices, animal sacrifice, or religious traditions**. 
                
                Include festivals even if they are criticized or debated — for example: 
                - Bakra Eid
                - Gadhimai Festival
                - Jallikattu
                - Diwali, Holi, Durga Puja
                - World Animal Day
                - National Animal Rights Day (NARD)
                - Awareness days, animal rights protests linked to festivals
                
                Respond in strict JSON format:
                {
                  "festivalLinked": true/false,
                  "festivalName": "Bakra Eid" or null
                }
                
                Title: %s
                
                Summary: %s
                """.formatted(article.getTitle(), article.getSummary());


            String response = callOpenAi(prompt);
            JsonNode root = mapper.readTree(response);

            boolean isLinked = root.get("festivalLinked").asBoolean();
            String festivalName = root.has("festivalName") && !root.get("festivalName").isNull()
                    ? root.get("festivalName").asText()
                    : null;
            article.setFestivalLinked(isLinked);
            article.setFestivalName(festivalName);

            // Store festival info in cache
            CachedAiInsight insight = CachedAiInsight.builder()
                    .url(article.getUrl())
                    .title(article.getTitle())
                    .summary(article.getSummary())
                    .publishedDate(article.getPublishedDate())
                    .sentiment(article.getSentiment())
                    .themes(article.getThemes() != null ? List.of(article.getThemes()) : List.of())
                    .tone(article.getTone())
                    .authorities(article.getAuthorities())
                    .location(article.getLocation())
                    .festivalLinked(isLinked)
                    .festivalName(festivalName)
                    .cachedAt(LocalDateTime.now())
                    .build();

            cacheRepo.save(insight);
            return isLinked;

        } catch (Exception e) {
            log.error("Festival check failed for: {}", article.getTitle(), e);
            return false;
        }
    }

}