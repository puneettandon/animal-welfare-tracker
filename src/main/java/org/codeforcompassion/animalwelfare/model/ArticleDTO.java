package org.codeforcompassion.animalwelfare.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Setter
@ToString
@Getter
public class ArticleDTO {

    private String title;
    private String url;
    private String source;
    private String summary;
    private LocalDate publishedDate;
    private String sentiment;
    private String[] themes;
    private boolean festivalLinked;
    private String festivalName;
    private String tone;
    private List<String> authorities;
    private String location;
}
