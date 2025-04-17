package com.newsRetreiver.newsRetreiver.Model;

import lombok.Data;
import java.util.List;

@Data
public class LLMResponse {
    private List<String> entities;

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    private String intent;
}
