# 📰 Contextual News Data Retrieval System

A Spring Boot backend project that retrieves and ranks news articles from a local database, enriched with summaries and filtered using LLM-powered query understanding. This system simulates multiple API endpoints such as category, source, relevance score, full-text search, and location-based search.

---

## 🚀 Features

- 🌐 RESTful API endpoints
- 🧠 Integration with Gemini to:
  - Extract entities & user intent from queries
  - Generate summaries of news articles
- 📚 Article filtering by:
  - Category
  - Relevance Score
  - Text Search (via LLM)
  - Source
  - Location (Haversine formula)
- 📦 Returns JSON with top 5 articles + query metadata

---

## 🛠️ Tech Stack

- Java 17
- Spring Boot
- Maven
- MongoDB
- Gemini/OpenAI API
- Postman (for testing)

---

## 📡 API Endpoints

| Method | Endpoint                            | Description                                 |
|--------|-------------------------------------|---------------------------------------------|
| GET    | `/api/v1/news/category?name=world`  | Filter news by category                     |
| GET    | `/api/v1/news/score?threshold=0.7`  | Filter by relevance score                   |
| GET    | `/api/v1/news/search?query=Dhoni`   | Search articles using full-text + LLM       |
| GET    | `/api/v1/news/source?name=News18`   | Get articles by source                      |
| GET    | `/api/v1/news/nearby?...`           | Get news within a radius from coordinates   |

📥 Example JSON response:
```json
{
  "articles": [ ... ],
  "query": "category:world",
  "totalResults": 5
}
