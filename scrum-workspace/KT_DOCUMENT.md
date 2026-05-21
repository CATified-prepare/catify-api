# 📘 Knowledge Transfer (KT) Document — Catify API

**Last Updated:** May 21, 2026  
**Author:** Architect

---

## 1. Project Overview

**Catify** is a CAT India MBA entrance exam AI assistant API. It answers CAT-related questions using LLM (Gemini in dev, Claude in production) and will use RAG (Retrieval-Augmented Generation) with a CAT question bank stored in Qdrant vector database.

---

## 2. Current State (Sprint 1 — May 21, 2026)

| Component | Status | Notes |
|-----------|--------|-------|
| Spring Boot app | ✅ Running | Spring Boot 4.0.6 + Java 21 |
| Chat endpoint | ✅ Working | `POST /api/v1/cat/ask` → Gemini responds |
| Gemini LLM | ✅ Connected | Model: `gemini-3.1-flash-lite` (free tier) |
| Embedding model | ✅ Configured | Model: `gemini-embedding-002` (free tier) |
| Qdrant | ✅ Connected | Docker container, localhost:6333 |
| PostgreSQL | ✅ Connected | Docker container, localhost:5432 |
| Redis | ✅ Connected | Docker container, localhost:6379 |
| pgAdmin | ✅ Running | http://localhost:5050 |
| RAG pipeline | ❌ Not built yet | Sprint 2 |
| Conversation memory | ❌ Not built yet | Sprint 3 |
| Streaming (SSE) | ❌ Not built yet | Sprint 3 |

---

## 3. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (Amazon Corretto) |
| Framework | Spring Boot | 4.0.6 |
| AI Library | Spring AI | 2.0.0-SNAPSHOT |
| Chat LLM (dev) | Google Gemini | `gemini-3.1-flash-lite` |
| Chat LLM (prod) | Anthropic Claude | `claude-sonnet-4-20250514` |
| Embedding (dev) | Google Gemini | `gemini-embedding-002` |
| Embedding (prod) | OpenAI | `text-embedding-3-small` |
| Vector DB | Qdrant | latest (Docker) |
| Relational DB | PostgreSQL | 16-alpine (Docker) |
| Cache | Redis | 7-alpine (Docker) |
| Build | Maven | via `mvnw` wrapper |
| IDE | IntelliJ IDEA | 2025.3.5 |

---

## 4. Project Structure

```
catify-api/
├── src/main/java/com/catify/catify_api/
│   ├── CatifyApiApplication.java          ← entry point
│   ├── config/
│   │   └── SecurityConfig.java            ← permitAll on /api/v1/**
│   ├── controller/
│   │   └── ChatController.java            ← POST /api/v1/cat/ask
│   ├── dto/
│   │   ├── ChatRequest.java               ← { "question": "..." }
│   │   └── ChatResponse.java              ← { "answer": "...", ... }
│   └── service/
│       └── CatChatService.java            ← ChatClient + system prompt
├── src/main/resources/
│   ├── application.yaml                   ← shared config, localhost defaults
│   ├── application-gemini.yaml            ← Gemini chat + embedding config
│   ├── application-prod.yaml              ← Claude + OpenAI config
│   └── application-dev.yml                ← (legacy, not used)
├── .env                                   ← GEMINI_API_KEY (gitignored)
├── docker-compose.yml                     ← postgres, qdrant, redis, pgadmin, api
├── pom.xml                                ← Spring Boot 4.0.6 + Spring AI 2.0.0-SNAPSHOT
└── scrum-workspace/                       ← project management docs
    ├── adr/                               ← architecture decision records
    ├── backlog/                            ← product backlog
    ├── sprints/                            ← sprint details
    └── roles/                             ← role instructions for AI assistants
```

---

## 5. Configuration Files Explained

### `application.yaml` (shared, always loaded)
- Datasource → `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/catify}`
- Redis → `${REDIS_HOST:localhost}:${REDIS_PORT:6379}`
- Qdrant → `${QDRANT_HOST:localhost}:${QDRANT_PORT:6333}`
- `.env` loading → `spring.config.import: optional:file:.env[.properties]`

**Defaults are `localhost`** — works from IntelliJ with Docker containers running. Docker Compose overrides via env vars.

### `application-gemini.yaml` (dev profile — free tier)
```yaml
spring.ai.google.genai.api-key: ${GEMINI_API_KEY}
spring.ai.google.genai.chat.model: gemini-3.1-flash-lite
spring.ai.google.genai.embedding.api-key: ${GEMINI_API_KEY}   # MUST be set separately
spring.ai.google.genai.embedding.model: gemini-embedding-002
```

**Key learning:** Chat and embedding have **separate property classes** with separate prefixes. Both need `api-key` explicitly.

### `.env` (secrets — gitignored)
```dotenv
GEMINI_API_KEY=<your-key>
```

---

## 6. How to Run

### Prerequisites
- Java 21 installed
- Docker Desktop running
- IntelliJ IDEA

### Steps

```bash
# 1. Start infra
docker-compose up -d postgres qdrant redis pgadmin

# 2. Run app from IntelliJ with active profile: gemini
#    OR from terminal:
.\mvnw.cmd spring-boot:run --spring.profiles.active=gemini

# 3. Test
curl -X POST http://localhost:8080/api/v1/cat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the CAT exam syllabus?"}'
```

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/cat/ask` | None (permitAll) | Ask a CAT question |
| GET | `/actuator/health` | None | Health check |
| GET | `/swagger-ui/` | None | API docs |

---

## 7. Key Decisions & Learnings (ADR-003)

| Decision | Detail |
|----------|--------|
| Model change | `gemini-2.0-flash` → `gemini-3.1-flash-lite` (old model has 0 free quota) |
| Embedding dual api-key | `spring.ai.google.genai.embedding.api-key` must be set separately from chat |
| Localhost defaults | `application.yaml` defaults to `localhost`; Docker Compose overrides via env vars |
| No autoconfigure excludes | Previously thought needed, but resolved by correct config |
| Spring AI version | `2.0.0-SNAPSHOT` — required by Spring Boot 4.0.6, no stable release yet |

---

## 8. Free Tier Quotas (Gemini — as of May 2026)

| Model | RPM | RPD | Use |
|-------|-----|-----|-----|
| `gemini-3.1-flash-lite` | 15 | 500 | Chat |
| `gemini-embedding-002` | 100 | 1000 | Embeddings |

Monitor at: https://ai.dev/rate-limit

---

## 9. Docker Services

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| catify-postgres | postgres:16-alpine | 5432 | Relational data |
| catify-qdrant | qdrant/qdrant:latest | 6333 | Vector store |
| catify-redis | redis:7-alpine | 6379 | Caching |
| catify-pgadmin | dpage/pgadmin4:latest | 5050 | DB admin UI |
| catify-api | (built from Dockerfile) | 8080 | App (prod/CI only) |

**Dev workflow:** Run infra containers only, run app from IntelliJ for hot reload + debugger.

---

## 10. What's Next

| Sprint | Stories | Goal |
|--------|---------|------|
| Sprint 1 (current) | US-003, US-004 remaining | Complete chat + Qdrant wiring |
| Sprint 2 | US-005, US-006, US-007, US-012 | RAG pipeline + guardrails |
| Sprint 3 | US-008, US-009, US-010 | Memory, streaming, auth |
| Sprint 4 | US-011, US-013, US-014, US-015 | Rate limiting, metrics, Docker, tests |

---

## 11. Known Risks

1. **Spring AI 2.0.0-SNAPSHOT** — unstable, property prefixes may change at GA
2. **Free tier quotas can change** — Google may revoke models without notice
3. **Spring Boot 4.x is bleeding edge** — limited community support, most blogs cover 3.x
4. **No conversation memory yet** — every request is stateless (no history)

---

## 12. Reference Documents

| Document | Path | Purpose |
|----------|------|---------|
| ADR-001 | `scrum-workspace/adr/ADR-001-dev-llm-google-gemini.md` | Original LLM decision |
| ADR-003 | `scrum-workspace/adr/ADR-003-gemini-model-change-and-snapshot-workarounds.md` | Model change + learnings |
| Product Backlog | `scrum-workspace/backlog/PRODUCT_BACKLOG.md` | All user stories |
| Scrum Board | `scrum-workspace/SCRUM_BOARD.md` | Current sprint status |
| Sprint 1 | `scrum-workspace/sprints/SPRINT_1.md` | Sprint 1 details |

