package com.newsRetreiver.newsRetreiver.Service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsRetreiver.newsRetreiver.Model.LLMResponse;

import java.util.concurrent.TimeUnit;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-001:generateContent?key=";

    /**
     * Extract structured intent and entities from a user query.
     */
    public LLMResponse extractIntentAndEntitiesParsed(String query) {
        String prompt = "Extract entities and user intent from the following query:\n\"" + query + "\"\n"
                + "Return a JSON with keys 'entities' (list of strings) and 'intent' (string).";

        String jsonResponse = callGeminiForJson(prompt);
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            System.out.println("❌ No valid response from Gemini for query: " + query);
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResponse, LLMResponse.class);
        } catch (Exception e) {
            System.out.println("⚠️ Failed to parse LLM JSON:\n" + jsonResponse);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Summarize a news article using Gemini.
     */
    public String summarizeArticle(String text) {
        try {
            String prompt = "Summarize the following news article in 2 short lines:\n" + text;
            return callGeminiForText(prompt);
        } catch (Exception e) {
            System.out.println("⚠️ Summary failed for article:\n" + text);
            return "Summary not available.";
        }
    }

    private String callGeminiForText(String prompt) {
        try {
            // Prepare Gemini request body
            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("text", prompt)))));

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL + apiKey)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("❌ Gemini API call failed: " + response.code() + " - " + response.message());
                    return null;
                }

                String raw = response.body().string();
                if (raw == null || raw.trim().isEmpty()) {
                    System.out.println("❌ Gemini returned empty response body");
                    return null;
                }

                JSONObject json = new JSONObject(raw);
                System.out.println("🔍 Raw Gemini Response:\n" + json.toString(2));

                JSONArray candidates = json.optJSONArray("candidates");
                if (candidates == null || candidates.length() == 0) {
                    System.out.println("❌ No candidates returned");
                    return null;
                }

                JSONObject firstCandidate = candidates.getJSONObject(0);

                // 🛡️ SAFETY Check
                if ("SAFETY".equals(firstCandidate.optString("finishReason"))) {
                    System.out.println("⚠️ Gemini blocked response due to safety filters");
                    return "Summary not available due to content restrictions.";
                }

                if (!firstCandidate.has("content")) {
                    System.out.println("❌ 'content' not found in Gemini response");
                    return null;
                }

                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.optJSONArray("parts");
                if (parts == null || parts.length() == 0) {
                    System.out.println("❌ No parts found inside content");
                    return null;
                }

                return parts.getJSONObject(0).getString("text").trim();

            }

        } catch (Exception e) {
            System.out.println("❌ Exception while calling Gemini: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private String callGeminiForJson(String prompt) {
        String text = callGeminiForText(prompt);
        if (text == null) return null;

        // Clean code blocks (markdown style)
        if (text.startsWith("```json")) {
            text = text.replace("```json", "").replace("```", "").trim();
        } else if (text.startsWith("```")) {
            text = text.replace("```", "").trim();
        }

        try {
            new JSONObject(text); // Validate it's actually JSON
            return text;
        } catch (Exception e) {
            System.out.println("Invalid JSON from Gemini:\n" + text);
            return null;
        }
    }
}
