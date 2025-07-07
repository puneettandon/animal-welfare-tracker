package org.codeforcompassion.animalwelfare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "news.fetch.limit")
public class FetchLimitConfig {
    private int perfeed;
    private int total;
}
