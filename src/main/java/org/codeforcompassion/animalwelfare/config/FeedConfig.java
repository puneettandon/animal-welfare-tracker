package org.codeforcompassion.animalwelfare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news.feeds")
public class FeedConfig {
    private List<String> feedUrls;
}
