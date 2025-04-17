package com.newsRetreiver.newsRetreiver.Repository;

import com.newsRetreiver.newsRetreiver.Model.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {
    // You’ll add custom queries later if needed
}
