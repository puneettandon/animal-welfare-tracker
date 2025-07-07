package org.codeforcompassion.animalwelfare.repository.impl;

import lombok.RequiredArgsConstructor;
import org.codeforcompassion.animalwelfare.model.FestivalTrendDTO;
import org.codeforcompassion.animalwelfare.repository.CustomInsightAggregationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class CustomInsightAggregationRepositoryImpl implements CustomInsightAggregationRepository {

    private final MongoTemplate mongoTemplate;

   /* @Override
    public List<FestivalTrendDTO> aggregateFestivalTrends(LocalDate fromDate) {
        MatchOperation match = match(Criteria.where("festivalLinked").is(true)
                .and("festivalName").ne(null)
                .and("publishedDate").gte(fromDate));

        GroupOperation group = group("festivalName")
                .first("festivalName").as("event")
                .sum(ConditionalOperators
                        .when(Criteria.where("sentiment").regex("^positive$", "i"))
                        .then(1).otherwise(0)).as("positive")
                .sum(ConditionalOperators
                        .when(Criteria.where("sentiment").regex("^negative$", "i"))
                        .then(1).otherwise(0)).as("negative");

        ProjectionOperation project = project("event", "positive", "negative");

        Aggregation aggregation = newAggregation(match, group, project);

        AggregationResults<FestivalTrendDTO> aggregationResults =  mongoTemplate.aggregate(aggregation, "cached_ai_insights", FestivalTrendDTO.class);
        System.out.println("Aggregation Result: " + aggregationResults);
        System.out.println("Aggregation Raw Results: " + aggregationResults.getRawResults());
        System.out.println("Aggregation Map Result: " + aggregationResults.getMappedResults());
        return aggregationResults.getMappedResults();
    }
*/
   @Override
   public List<FestivalTrendDTO> aggregateFestivalTrends(LocalDate fromDate) {
       MatchOperation match = match(Criteria.where("festivalLinked").is(true)
               .and("festivalName").ne(null)
               .and("publishedDate").gte(fromDate));

       GroupOperation group = group("festivalName")
               .first("festivalName").as("event")
               .sum(ConditionalOperators.when(Criteria.where("sentiment").is("POSITIVE")).then(1).otherwise(0)).as("positive")
               .sum(ConditionalOperators.when(Criteria.where("sentiment").is("NEGATIVE")).then(1).otherwise(0)).as("negative");

       ProjectionOperation project = project("event", "positive", "negative");

       Aggregation aggregation = newAggregation(match, group, project);

       return mongoTemplate.aggregate(aggregation, "cached_ai_insights", FestivalTrendDTO.class).getMappedResults();
   }


}
