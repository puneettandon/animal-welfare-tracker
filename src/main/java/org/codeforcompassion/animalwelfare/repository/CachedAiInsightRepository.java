package org.codeforcompassion.animalwelfare.repository;

import org.codeforcompassion.animalwelfare.model.CachedAiInsight;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CachedAiInsightRepository extends
        MongoRepository<CachedAiInsight, String>,
        CustomInsightAggregationRepository {

    // url is the ID
    Optional<CachedAiInsight> findByUrl(String url);

    List<CachedAiInsight> findByPublishedDateAfter(LocalDate date);

    List<CachedAiInsight> findByPublishedDateBetween(LocalDate from, LocalDate to);

    Optional<CachedAiInsight> findByTitleAndPublishedDate(String title, LocalDate publishedDate);

}
