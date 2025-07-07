package org.codeforcompassion.animalwelfare.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "cached_ai_insights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CachedAiInsight {

    @Id
    private String url;

    private String title;
    private String summary;
    private LocalDate publishedDate;
    private String sentiment;
    private List<String> themes;
    private boolean festivalLinked;
    private String festivalName;
    private String tone;
    private List<String> authorities;
    private String location;

    private LocalDateTime cachedAt;
}
