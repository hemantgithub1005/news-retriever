package com.newsRetreiver.newsRetreiver.Model;

import java.util.List;

public class ApiResponse<T> {
    private List<T> articles;
    private String query;
    private int totalResults;

    public ApiResponse(List<T> articles, String query, int totalResults) {
        this.articles = articles;
        this.query = query;
        this.totalResults = totalResults;
    }

    public List<T> getArticles() { return articles; }
    public void setArticles(List<T> articles) { this.articles = articles; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
}