package org.codeforcompassion.animalwelfare.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.codeforcompassion.animalwelfare.repository.CachedAiInsightRepository;
import org.codeforcompassion.animalwelfare.util.SourceExtractorUtil;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class RssFeedService {

    @Autowired
    private CachedAiInsightRepository cachedAiInsightRepository;

    /*public List<ArticleDTO> fetchArticles(String feedUrl) throws Exception {
        URL url = new URL(feedUrl);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));
        List<ArticleDTO> articles = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            ArticleDTO article = new ArticleDTO();
            article.setTitle(entry.getTitle());
            article.setUrl(entry.getLink());
            System.out.println("Article Description: " + entry.getDescription().getValue());
            article.setSummary(entry.getDescription() != null ? entry.getDescription().getValue() : "");
            article.setPublishedDate(entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            articles.add(article);
        }
        return articles;
    }*/

    public List<ArticleDTO> fetchArticles(String feedUrl) throws Exception {
        URL url = new URL(feedUrl);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));
        List<ArticleDTO> articles = new ArrayList<>();

        LocalDate threeYearsAgo = LocalDate.now().minusYears(3);

        for (SyndEntry entry : feed.getEntries()) {
            if (entry.getPublishedDate() == null) continue;

            LocalDate publishedDate = entry.getPublishedDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // 🔥 Filter old articles
            if (publishedDate.isBefore(threeYearsAgo)) {
                continue;
            }

            ArticleDTO article = new ArticleDTO();
            article.setTitle(entry.getTitle());
            article.setUrl(entry.getLink());
            String summary = entry.getDescription() != null ? entry.getDescription().getValue() : "";
            String cleanSummary = Jsoup.parse(summary).text();
            article.setSummary(cleanSummary);
            article.setPublishedDate(publishedDate);
            String source = SourceExtractorUtil.extractSource(feedUrl, summary, entry.getLink());
            article.setSource(source);
            articles.add(article);
        }
        return articles;
    }

    public boolean articleExistsInMongo(String url) {
        return cachedAiInsightRepository.existsById(url);
    }


    public boolean articleExistsInMongoWithUrlOrTitle(String normalizedUrl, ArticleDTO article) {
        return  cachedAiInsightRepository.findById(normalizedUrl).isPresent()
                || cachedAiInsightRepository.findByTitleAndPublishedDate(article.getTitle(), article.getPublishedDate()).isPresent();

    }
}
