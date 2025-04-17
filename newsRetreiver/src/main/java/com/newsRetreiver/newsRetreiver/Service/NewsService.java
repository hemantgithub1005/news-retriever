package com.newsRetreiver.newsRetreiver.Service;

import com.newsRetreiver.newsRetreiver.Repository.NewsArticleRepository;
import com.newsRetreiver.newsRetreiver.Model.LLMResponse;
import com.newsRetreiver.newsRetreiver.Model.NewsArticle;
import com.newsRetreiver.newsRetreiver.Util.Haversine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsArticleRepository repository;

    @Autowired
    private GeminiService geminiService;

    private static final int MAX_ARTICLES = 5;
    private static final int MAX_RETRIES = 2;

    public List<NewsArticle> getByCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        return repository.findAll().stream()
                .filter(a -> a.getCategory() != null && a.getCategory().contains(name))
                .sorted(Comparator.comparing(NewsArticle::getPublicationDate).reversed())
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());
    }

    public List<NewsArticle> getByScore(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Relevance score threshold must be between 0 and 1");
        }
        return repository.findAll().stream()
                .filter(a -> a.getRelevanceScore() >= threshold)
                .sorted(Comparator.comparingDouble(NewsArticle::getRelevanceScore).reversed())
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());
    }

    public List<NewsArticle> searchArticles(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        LLMResponse llm = geminiService.extractIntentAndEntitiesParsed(query);
        List<String> entities = llm != null && llm.getEntities() != null ? llm.getEntities() : List.of(query);

        return repository.findAll().stream()
                .filter(article -> {
                    if (article.getTitle() == null && article.getDescription() == null) return false;
                    return entities.stream().anyMatch(entity ->
                            (article.getTitle() != null && article.getTitle().toLowerCase().contains(entity.toLowerCase())) ||
                                    (article.getDescription() != null && article.getDescription().toLowerCase().contains(entity.toLowerCase())));
                })
                .sorted((a, b) -> {
                    double scoreA = calculateTextMatchScore(a, entities) + a.getRelevanceScore();
                    double scoreB = calculateTextMatchScore(b, entities) + b.getRelevanceScore();
                    return Double.compare(scoreB, scoreA);
                })
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());
    }

    public List<NewsArticle> getBySource(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Source name cannot be empty");
        }
        return repository.findAll().stream()
                .filter(a -> a.getSourceName() != null && a.getSourceName().equalsIgnoreCase(name))
                .sorted(Comparator.comparing(NewsArticle::getPublicationDate).reversed())
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());
    }

    public List<NewsArticle> getNearby(double lat, double lon, double radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values");
        }
        return repository.findAll().stream()
                .filter(a -> a.getLatitude() != null && a.getLongitude() != null)
                .filter(a -> Haversine.calculateDistance(lat, lon, a.getLatitude(), a.getLongitude()) <= radius)
                .sorted(Comparator.comparingDouble(a -> Haversine.calculateDistance(lat, lon, a.getLatitude(), a.getLongitude())))
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());
    }

    public List<NewsArticle> processUserQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        LLMResponse llm = geminiService.extractIntentAndEntitiesParsed(query);
        if (llm == null || llm.getEntities() == null || llm.getEntities().isEmpty()) {
            System.out.println("❌ Entity extraction failed or no entities found for query: " + query);
            return Collections.emptyList();
        }

        List<NewsArticle> allArticles = repository.findAll();

        List<NewsArticle> filtered = allArticles.stream()
                .filter(article -> {
                    if (article.getTitle() == null && article.getDescription() == null) return false;
                    return llm.getEntities().stream().anyMatch(entity ->
                            (article.getTitle() != null && article.getTitle().toLowerCase().contains(entity.toLowerCase())) ||
                                    (article.getDescription() != null && article.getDescription().toLowerCase().contains(entity.toLowerCase()))
                    );
                })
                .sorted(Comparator.comparing(NewsArticle::getPublicationDate).reversed())
                .limit(MAX_ARTICLES)
                .map(this::enrichArticle)
                .collect(Collectors.toList());

        return filtered;
    }

    public String generateSummaryById(String id) {
        NewsArticle article = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No article with id " + id));
        String text = article.getDescription() != null ? article.getDescription() : article.getTitle();
        return geminiService.summarizeArticle(text);
    }

    private NewsArticle enrichArticle(NewsArticle article) {
        article.setLlmSummary("Summary unavailable.");

        String contentToSummarize = article.getDescription() != null ? article.getDescription() : article.getTitle();
        if (contentToSummarize == null || contentToSummarize.trim().isEmpty()) {
            System.out.println("❌ No content to summarize for article: " + article.getTitle());
            return article;
        }

        boolean rateLimitHit = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String summary = geminiService.summarizeArticle(contentToSummarize);
                if (summary == null || summary.trim().isEmpty()) {
                    System.out.println("❌ Summary failed for article: " + article.getTitle() + " (Content: " + contentToSummarize + ") - Empty response");
                    rateLimitHit = true;
                    break;
                }

                if (!summary.trim().startsWith("{") && summary.length() > 20 && !summary.toLowerCase().contains("error")) {
                    article.setLlmSummary(summary);
                    System.out.println("✅ Used plain text summary for article: " + article.getTitle() + " (Summary: " + summary + ")");
                    break;
                } else {
                    System.out.println("❌ Invalid summary format for article: " + article.getTitle() + " (Response: " + summary + ")");
                    article.setLlmSummary("Summary unavailable (invalid format).");
                    break;
                }
            } catch (Exception e) {
                System.out.println("❌ Summary failed for article: " + article.getTitle() + " (Content: " + contentToSummarize + ") - Attempt " + attempt + " of " + MAX_RETRIES);
                System.out.println("Error: " + e.getMessage());
                rateLimitHit = e.getMessage() != null && e.getMessage().contains("429");
                if (attempt == MAX_RETRIES) {
                    article.setLlmSummary("Summary unavailable.");
                }
                try {
                    Thread.sleep(2000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    article.setLlmSummary("Summary unavailable.");
                    break;
                }
            }
        }

        if (rateLimitHit && article.getLlmSummary().startsWith("Summary unavailable")) {
            String fallbackSummary = (article.getDescription() != null && article.getDescription().length() > 20)
                    ? article.getDescription().substring(0, Math.min(100, article.getDescription().length())) + "..."
                    : article.getTitle();
            article.setLlmSummary(fallbackSummary);
            System.out.println("⚠️ Used fallback summary for article: " + article.getTitle() + " (Summary: " + fallbackSummary + ")");
        }

        return article;
    }

    private double calculateTextMatchScore(NewsArticle article, List<String> entities) {
        double score = 0.0;
        for (String entity : entities) {
            if (article.getTitle() != null && article.getTitle().toLowerCase().contains(entity.toLowerCase())) {
                score += 0.5;
            }
            if (article.getDescription() != null && article.getDescription().toLowerCase().contains(entity.toLowerCase())) {
                score += 0.3;
            }
        }
        return score;
    }
}