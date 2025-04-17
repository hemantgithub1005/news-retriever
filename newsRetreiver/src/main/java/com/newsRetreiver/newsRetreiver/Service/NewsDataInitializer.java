package com.newsRetreiver.newsRetreiver.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsRetreiver.newsRetreiver.Repository.NewsArticleRepository;
import com.newsRetreiver.newsRetreiver.Model.NewsArticle;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.InputStream;
import java.util.List;

@Service
public class NewsDataInitializer {



    @Autowired
    private NewsArticleRepository repository;

    @PostConstruct
    public void init() {
        try {
            if (repository.count() == 0) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                InputStream is = getClass().getClassLoader().getResourceAsStream("news_data.json");
                List<NewsArticle> articles = mapper.readValue(is, new TypeReference<>() {});
                repository.saveAll(articles);
                System.out.println("News data loaded into MongoDB.");
            } else {
                System.out.println("Data already exists, skipping load.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
