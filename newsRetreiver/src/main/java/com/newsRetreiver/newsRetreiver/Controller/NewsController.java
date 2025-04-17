package com.newsRetreiver.newsRetreiver.Controller;

import com.newsRetreiver.newsRetreiver.Service.GeminiService;
import com.newsRetreiver.newsRetreiver.Service.NewsService;
import com.newsRetreiver.newsRetreiver.Model.ApiResponse;
import com.newsRetreiver.newsRetreiver.Model.NewsArticle;
import com.newsRetreiver.newsRetreiver.Model.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<NewsArticle>> getByCategory(@RequestParam String name) {
        try {
            List<NewsArticle> articles = newsService.getByCategory(name);
            return ResponseEntity.ok(new ApiResponse<>(articles, "category:" + name, articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, "category:" + name, 0));
        }
    }

    @GetMapping("/score")
    public ResponseEntity<ApiResponse<NewsArticle>> getByScore(@RequestParam(defaultValue = "0.7") double threshold) {
        try {
            List<NewsArticle> articles = newsService.getByScore(threshold);
            return ResponseEntity.ok(new ApiResponse<>(articles, "score:" + threshold, articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, "score:" + threshold, 0));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<NewsArticle>> search(@RequestParam String query) {
        try {
            List<NewsArticle> articles = newsService.searchArticles(query);
            return ResponseEntity.ok(new ApiResponse<>(articles, query, articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, query, 0));
        }
    }

    @GetMapping("/source")
    public ResponseEntity<ApiResponse<NewsArticle>> getBySource(@RequestParam String name) {
        try {
            List<NewsArticle> articles = newsService.getBySource(name);
            return ResponseEntity.ok(new ApiResponse<>(articles, "source:" + name, articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, "source:" + name, 0));
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<NewsArticle>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double radius) {
        try {
            List<NewsArticle> articles = newsService.getNearby(lat, lon, radius);
            return ResponseEntity.ok(new ApiResponse<>(articles, "nearby:lat=" + lat + ",lon=" + lon + ",radius=" + radius, articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, "nearby:lat=" + lat + ",lon=" + lon, 0));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<NewsArticle>> processQuery(@RequestBody QueryRequest request) {
        try {
            List<NewsArticle> articles = newsService.processUserQuery(request.getQuery());
            return ResponseEntity.ok(new ApiResponse<>(articles, request.getQuery(), articles.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(null, request.getQuery(), 0));
        }
    }

    @GetMapping("/summary/{id}")
    public ResponseEntity<?> getArticleSummary(@PathVariable String id) {
        try {
            String summary = newsService.generateSummaryById(id);
            return ResponseEntity.ok(Map.of("id", id, "llmSummary", summary));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Article not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate summary");
        }
    }
}