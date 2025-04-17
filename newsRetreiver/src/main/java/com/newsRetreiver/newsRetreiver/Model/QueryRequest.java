package com.newsRetreiver.newsRetreiver.Model;

import lombok.Data;

@Data
public class QueryRequest {
    private String query;

    public String getQuery() {
        return query;
    }
}
