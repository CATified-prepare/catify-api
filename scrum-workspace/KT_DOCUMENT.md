#  Knowledge Transfer (KT) Document  Catify API

**Last Updated:** June 3, 2026
**Author:** Architect

---

## 1. Project Overview

**Catify** is a CAT India MBA entrance exam AI assistant API. It answers CAT-related questions using LLM (Gemini by default, Ollama locally) and will use RAG (Retrieval-Augmented Generation) with a CAT question bank stored in Qdrant vector database. Production profile (Claude + OpenAI embeddings) is planned but out of scope until the core RAG pipeline is complete.

---

## 2. Current State (Sprint 2  June 3, 2026)
| Component | Status | Notes |
|-----------|--------|-------|
| Spring Boot app |  Running | Spring Boot 4.0.6 + Java 21 |
| Chat endpoint |  Working | `POST /api/v1/cat/ask`  Gemini responds |
| Gemini LLM |  Connected | Model: `gemini-3.1-flash-lite` (free tier)  configured in `application.yaml` |
| Embedding model |  Configured | Model: `gemini-embedding-002` (free tier)  configured in `application.yaml` |
| Ollama LLM |  Working | Profile: `ollama`  `POST /api/v1/local/ask` (US-017 done) |
| Qdrant |  Connected | Docker container, localhost:6333 |
| PostgreSQL |  Connected | Docker container, localhost:5432 |
| Redis |  Connected | Docker container, localhost:6379 |
| pgAdmin |  Running | http://localhost:5050 |
| Data Ingestion |  In Progress | US-005  CSV  Qdrant, plus US-006 branch scaffolding for `POST /api/v1/ingest/pyq` |
| RAG pipeline |  In Progress | US-004 (Qdrant wiring) + US-006 (RAG service)  retrieval still pending |
| Guardrail |  Not built yet | US-007  Sprint 2 |
| Conversation memory |  Not built yet | Sprint 3 |
| Streaming (SSE) |  Not built yet | Sprint 3 |

---

## 3. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (Amazon Corretto) |
| Framework | Spring Boot | 4.0.6 |
| AI Library | Spring AI | 2.0.0-SNAPSHOT |
| Chat LLM (default) | Google Gemini | `gemini-3.1-flash-lite` |
| Chat LLM (ollama profile) | Ollama | configurable via `OLLAMA_CHAT_MODEL` |
| Chat LLM (prod  deferred) | Anthropic Claude | `claude-sonnet-4-20250514` |
| Embedding (default) | Google Gemini | `gemini-embedding-002` |
| Embedding (ollama profile) | Ollama | configurable via `OLLAMA_EMBEDDING_MODEL` |
| Embedding (prod  deferred) | OpenAI | `text-embedding-3-small` |
| Vector DB | Qdrant | latest (Docker) |
| Relational DB | PostgreSQL | 16-alpine (Docker) |
| Cache | Redis | 7-alpine (Docker) |
| Build | Maven | via `mvnw` wrapper |
| IDE | IntelliJ IDEA | 2025.3.5 |

---

## 4. Project Structure

```
catify-api/
 src/main/java/com/catify/catify_api/
    CatifyApiApplication.java           entry point
    config/
       SecurityConfig.java             permitAll on /api/v1/**
       OllamaWebClientConfig.java      HTTP client config for Ollama profile
    controller/
       ChatController.java             POST /api/v1/cat/ask
    dto/
       ChatRequest.java                { "question": "..." }
       ChatResponse.java               { "answer": "...", ... }
    service/
        CatChatService.java             ChatClient + system prompt
 src/main/resources/
    application.yaml                    shared config + Gemini chat/embedding (DEFAULT profile)
    application-ollama.yaml             Ollama chat/embedding (activate with --spring.profiles.active=ollama)
 .env                                    GEMINI_API_KEY (gitignored)
 docker-compose.yml                      postgres, qdrant, redis, pgadmin, api
 pom.xml                                 Spring Boot 4.0.6 + Spring AI 2.0.0-SNAPSHOT
 scrum-workspace/                        project management docs
     adr/                                architecture decision records
     backlog/                             product backlog
     sprints/                             sprint details
     roles/                              role instructions for AI assistants
```

>  **There is no `application-gemini.yaml`**  Gemini config lives directly in `application.yaml` and is active by default (no profile flag needed).

---

## 5. Spring Profiles

| Profile | Activated by | Config file | LLM | Embedding |
|---------|-------------|-------------|-----|-----------|
| **default** | (no flag  always active) | `application.yaml` | Gemini `gemini-3.1-flash-lite` | Gemini `gemini-embedding-002` |
| **ollama** | `--spring.profiles.active=ollama` | `application-ollama.yaml` | Ollama (`$OLLAMA_CHAT_MODEL`) | Ollama (`$OLLAMA_EMBEDDING_MODEL`) |
| **prod** *(deferred)* | `--spring.profiles.active=prod` | `application-prod.yaml` *(not yet created)* | Claude | OpenAI |

**Key rule:** Only one chat model provider should be active at a time. Each profile file uses `spring.autoconfigure.exclude` to disable the other provider's autoconfigure classes to prevent conflicts.

---

## 6. Configuration Files Explained

### `application.yaml` (default  always loaded, Gemini active)
- Datasource  `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/catify}`
- Redis  `${REDIS_HOST:localhost}:${REDIS_PORT:6379}`
- Qdrant  `${QDRANT_HOST:localhost}:${QDRANT_PORT:6333}`
- `.env` loading  `spring.config.import: optional:file:.env[.properties]`
- Gemini chat: `spring.ai.google.genai.chat.model: gemini-3.1-flash-lite`
- Gemini embedding: `spring.ai.google.genai.embedding.model: gemini-embedding-002`
- Ollama autoconfigure **excluded** here so it doesn't interfere with the default profile

```yaml
spring.ai.google.genai.api-key: ${GEMINI_API_KEY}
spring.ai.google.genai.chat.model: gemini-3.1-flash-lite
spring.ai.google.genai.embedding.api-key: ${GEMINI_API_KEY}   # MUST be set separately
spring.ai.google.genai.embedding.model: gemini-embedding-002
```

**Key learning:** Chat and embedding have **separate property classes** with separate prefixes. Both need `api-key` explicitly.

### `application-ollama.yaml` (ollama profile)
- Activates on `spring.profiles.active=ollama`
- Disables Gemini autoconfigure classes (chat + embedding)
- Configures Ollama base URL, chat model, embedding model via env vars
- Longer HTTP timeouts for large local models

### `.env` (secrets  gitignored)
```dotenv
GEMINI_API_KEY=<your-key>
```

---

## 7. How to Run

### Prerequisites
- Java 21 installed
- Docker Desktop running
- IntelliJ IDEA

### Default Profile (Gemini  no profile flag needed)

```bash
# 1. Start infra
docker-compose up -d postgres qdrant redis pgadmin

# 2. Run app  Gemini is the default, no profile flag required
.\mvnw.cmd spring-boot:run

# 3. Test
curl -X POST http://localhost:8080/api/v1/cat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the CAT exam syllabus?"}'
```

### Ollama Profile (local LLM)

```bash
# 1. Ensure Ollama is running locally (WSL or native)
#    Pull a model first: ollama pull llama3.2

# 2. Run app with ollama profile
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=ollama

# 3. Test
curl -X POST http://localhost:8080/api/v1/local/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is VARC in CAT?"}'
```

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/cat/ask` | None (permitAll) | Ask a CAT question (Gemini default) |
| POST | `/api/v1/local/ask` | None (permitAll) | Ask via local Ollama model |
| GET | `/actuator/health` | None | Health check |
| GET | `/swagger-ui/` | None | API docs |

---

## 8. Key Decisions & Learnings (ADR-003)

| Decision | Detail |
|----------|--------|
| Model change | `gemini-2.0-flash`  `gemini-3.1-flash-lite` (old model has 0 free quota) |
| Gemini in `application.yaml` | No separate `application-gemini.yaml`  Gemini config is in the default profile file |
| Ollama as separate profile | `application-ollama.yaml` activates only with `--spring.profiles.active=ollama`; excludes Gemini autoconfigure |
| Prod profile deferred | Claude + OpenAI embeddings will be configured when ready to deploy; not in scope for Sprint 2 |
| Embedding dual api-key | `spring.ai.google.genai.embedding.api-key` must be set separately from chat |
| Localhost defaults | `application.yaml` defaults to `localhost`; Docker Compose overrides via env vars |
| No autoconfigure excludes for Gemini | Resolved by correct config; Ollama excludes are in `application.yaml` (default) and vice versa in `application-ollama.yaml` |
| Spring AI version | `2.0.0-SNAPSHOT`  required by Spring Boot 4.0.6, no stable release yet |

---

## 9. Free Tier Quotas (Gemini  as of May 2026)

| Model | RPM | RPD | Use |
|-------|-----|-----|-----|
| `gemini-3.1-flash-lite` | 15 | 500 | Chat |
| `gemini-embedding-002` | 100 | 1000 | Embeddings |

Monitor at: https://ai.dev/rate-limit

---

## 10. Docker Services

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| catify-postgres | postgres:16-alpine | 5432 | Relational data |
| catify-qdrant | qdrant/qdrant:latest | 6333 | Vector store |
| catify-redis | redis:7-alpine | 6379 | Caching |
| catify-pgadmin | dpage/pgadmin4:latest | 5050 | DB admin UI |
| catify-api | (built from Dockerfile) | 8080 | App (prod/CI only) |

**Dev workflow:** Run infra containers only, run app from IntelliJ for hot reload + debugger.

---

## 11. What's Next

| Sprint | Stories | Goal |
|--------|---------|------|
| Sprint 2 (current) | US-004, US-005, US-006, US-007, US-012 | Qdrant wiring, data ingestion, RAG, guardrails, exception handling |
| Sprint 3 | US-008, US-009, US-010 | Memory, streaming, auth |
| Sprint 4 | US-011, US-013, US-014, US-015 | Rate limiting, metrics, Docker, tests |

---

## 12. Known Risks

1. **Spring AI 2.0.0-SNAPSHOT**  unstable, property prefixes may change at GA
2. **Free tier quotas can change**  Google may revoke models without notice
3. **Spring Boot 4.x is bleeding edge**  limited community support, most blogs cover 3.x
4. **No conversation memory yet**  every request is stateless (no history)
5. **Prod profile not yet created**  `application-prod.yaml` (Claude + OpenAI) is deferred; do not reference it in code until Sprint 3/4

---

## 13. Reference Documents

| Document | Path | Purpose |
|----------|------|---------|
| ADR-001 | `scrum-workspace/adr/ADR-001-dev-llm-google-gemini.md` | Original LLM decision |
| ADR-003 | `scrum-workspace/adr/ADR-003-gemini-model-change-and-snapshot-workarounds.md` | Model change + learnings |
| Product Backlog | `scrum-workspace/backlog/PRODUCT_BACKLOG.md` | All user stories |
| Scrum Board | `scrum-workspace/SCRUM_BOARD.md` | Current sprint status |
| Sprint 1 | `scrum-workspace/sprints/SPRINT_1.md` | Sprint 1 details (closed) |
| Sprint 2 | `scrum-workspace/sprints/SPRINT_2.md` | Sprint 2 details (active) |
