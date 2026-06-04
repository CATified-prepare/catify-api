# Knowledge Transfer (KT) Document — Catify API

**Last Updated:** June 5, 2026
**Author:** Architect

---

## 1. Project Overview

**Catify** is a CAT India MBA entrance exam AI assistant API. It answers CAT-related questions using an LLM (Gemini by default, Ollama locally) and is being wired up for RAG (Retrieval-Augmented Generation) over a CAT question bank stored in Qdrant. The production profile (Claude + OpenAI embeddings) is planned but deferred until the core RAG + guardrail pipeline lands.

---

## 2. Current State (Sprint 2 — June 5, 2026)

| Component | Status | Notes |
|-----------|--------|-------|
| Spring Boot app | ✅ Running | Spring Boot 4.0.6 + Java 21 |
| Chat endpoint | ✅ Working | `POST /api/v1/cat/ask` → Gemini (returns answer + AI metadata + token usage) |
| Gemini LLM | ✅ Connected | Model: `gemini-3.1-flash-lite`, `temperature: 0.3` (configured in `application.yaml`) |
| Embedding model | ✅ Configured | Model: `gemini-embedding-002` (free tier) — separate `api-key` in `application.yaml` |
| Ollama LLM | ✅ Working | Profile: `ollama` → `POST /api/v1/local/ask` (US-017 done) |
| Qdrant | ✅ Connected | gRPC port `6334`, collection `cat-questions`, `initialize-schema: true` |
| Qdrant health indicator | ✅ Added | `QdrantHealthIndicator` reports collection + indexed vector count via `/actuator/health` |
| PostgreSQL | ✅ Connected | Docker container, `5432` |
| Redis | ✅ Connected | Docker container, `6379` |
| pgAdmin | ✅ Running | http://localhost:5050 (auto-imports Catify Postgres server) |
| PYQ Data Ingestion | 🔄 In Progress (US-005/US-006) | `POST /api/v1/ingest/pyq` writes to Qdrant via `VectorStore.add()`; CSV loader still pending |
| RAG retrieval | ⏳ Not yet wired | US-006 — `CatRagService` + similarity search into `CatChatService` still TODO |
| Global exception handling | ✅ In place (US-012 core) | `GlobalExceptionHandler` + `CatifyException` + `ApiError` record |
| Bean validation | ✅ Wired | `@Valid` on `ChatRequest`; field errors surfaced via handler |
| AI metadata mapping | ✅ Added | `AiMetadataMapper` (MapStruct) → `AiMetadata` (prompt/completion/total tokens) |
| Guardrail | ❌ Not built yet | US-007 — Sprint 2 |
| Conversation memory | ❌ Not built yet | Sprint 3 |
| Streaming (SSE) | ❌ Not built yet | Sprint 3 |
| Prometheus metrics | ✅ Exposed | `/actuator/prometheus` (Micrometer + `micrometer-registry-prometheus`) |
| OpenAPI / Swagger UI | ✅ Available | `springdoc-openapi-starter-webmvc-ui` |

---

## 3. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (Amazon Corretto) |
| Framework | Spring Boot | 4.0.6 |
| Web stack | Spring MVC (`spring-boot-starter-webmvc`) + WebFlux (`WebClient` only) | 4.0.6 |
| AI Library | Spring AI | 2.0.0-SNAPSHOT |
| Chat LLM (default) | Google Gemini | `gemini-3.1-flash-lite` |
| Chat LLM (`ollama` profile) | Ollama | configurable via `OLLAMA_CHAT_MODEL` |
| Chat LLM (prod — deferred) | Anthropic Claude | `claude-sonnet-4-20250514` |
| Embedding (default) | Google Gemini | `gemini-embedding-002` |
| Embedding (`ollama` profile) | Ollama | configurable via `OLLAMA_EMBEDDING_MODEL` |
| Embedding (prod — deferred) | OpenAI | `text-embedding-3-small` |
| Vector DB | Qdrant (gRPC) | latest (Docker) |
| Relational DB | PostgreSQL | 16-alpine (Docker) |
| Cache | Redis | 7-alpine (Docker) |
| DTO mapping | MapStruct + Lombok binding | 1.5.5.Final |
| API docs | springdoc-openapi | 3.0.2 |
| Metrics | Micrometer + Prometheus registry | runtime |
| Build | Maven | via `mvnw` wrapper |
| IDE | IntelliJ IDEA | 2025.3.5 |

Notable Spring AI artifacts in `pom.xml`:
- `spring-ai-starter-model-google-genai` (chat)
- `spring-ai-google-genai-embedding` (embedding)
- `spring-ai-starter-model-ollama` (chat + embedding for `ollama` profile)
- `spring-ai-starter-vector-store-qdrant` (provides `VectorStore` + `QdrantClient` beans)
- `spring-ai-advisors-vector-store` (for upcoming RAG advisor wiring)

---

## 4. Project Structure

```
catify-api/
├── src/main/java/com/catify/catify_api/
│   ├── CatifyApiApplication.java               # entry point
│   ├── config/
│   │   ├── SecurityConfig.java                 # permitAll on /api/v1/**
│   │   ├── OllamaWebClientConfig.java          # HTTP client config for Ollama profile
│   │   └── QdrantHealthIndicator.java          # custom /actuator/health contributor
│   ├── constant/
│   │   └── CatifyConstants.java                # ErrorMessageConstants + CatifyGenericConstants
│   ├── controller/
│   │   ├── ChatController.java                 # POST /api/v1/cat/ask
│   │   └── DataIngestionController.java        # POST /api/v1/ingest/pyq
│   ├── dto/
│   │   ├── request/
│   │   │   ├── chat/                           # ChatRequest, AiMetadata
│   │   │   └── ingestion/                      # PyqIngestionRequest, QuestionGroup, Question
│   │   └── response/
│   │       ├── chat/                           # ChatResponse(answer, sources, tokensUsed, ai)
│   │       └── ingestion/                      # PyqIngestionResponse(message, ingestedCount)
│   ├── exception/
│   │   ├── CatifyException.java                # base RuntimeException w/ code + HttpStatus
│   │   ├── ApiError.java                       # standard error record
│   │   └── GlobalExceptionHandler.java         # @RestControllerAdvice
│   ├── mapper/
│   │   └── AiMetadataMapper.java               # MapStruct: ChatResponseMetadata → AiMetadata
│   └── service/
│       ├── CatChatService.java                 # ChatClient + system prompt + metadata mapping
│       └── DataIngestionService.java           # PYQ → Document(s) → VectorStore.add(...)
├── src/main/resources/
│   ├── application.yaml                        # default profile: Gemini + Qdrant + Postgres + Redis
│   ├── application-ollama.yaml                 # ollama profile (overrides chat + embedding)
│   └── examples/
│       ├── api-v1-cat-ask-request.json
│       ├── api-v1-ingest-pyq-request.json
│       └── README.txt
├── src/test/java/com/catify/catify_api/
│   ├── controller/                             # controller tests
│   ├── service/                                # service tests
│   └── IntgrTest/                              # integration tests
├── src/test/resources/
│   └── application-integration-test.yaml
├── .env                                         # GEMINI_API_KEY (gitignored)
├── docker-compose.yml                           # api, postgres, qdrant, redis, pgadmin
├── Dockerfile
├── pom.xml                                      # Spring Boot 4.0.6 + Spring AI 2.0.0-SNAPSHOT
└── scrum-workspace/                             # PM docs (adr/, backlog/, sprints/, roles/)
```

> ⚠️ **There is no `application-gemini.yaml`** — Gemini config lives directly in `application.yaml` and is active by default (no profile flag needed).

---

## 5. Spring Profiles

| Profile | Activated by | Config file | LLM | Embedding |
|---------|-------------|-------------|-----|-----------|
| **default** | (no flag — always active) | `application.yaml` | Gemini `gemini-3.1-flash-lite` | Gemini `gemini-embedding-002` |
| **ollama** | `--spring.profiles.active=ollama` | `application-ollama.yaml` | Ollama (`$OLLAMA_CHAT_MODEL`) | Ollama (`$OLLAMA_EMBEDDING_MODEL`) |
| **prod** *(deferred)* | `--spring.profiles.active=prod` | `application-prod.yaml` *(not yet created)* | Claude | OpenAI |

**Key rule:** Only one chat model provider should be active at a time. The default profile excludes Ollama autoconfigure; the `ollama` profile excludes Gemini autoconfigure. This avoids bean-conflict errors when both starters are on the classpath.

---

## 6. Configuration Files Explained

### `application.yaml` (default — always loaded, Gemini active)

Highlights:
- Excludes Ollama autoconfigure: `OllamaApiAutoConfiguration`, `OllamaChatAutoConfiguration`, `OllamaEmbeddingAutoConfiguration`.
- Datasource → `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/catify}`
- Redis → `${REDIS_HOST:localhost}:${REDIS_PORT:6379}`
- Qdrant (gRPC) → `${QDRANT_HOST:localhost}:${QDRANT_GRPC_PORT:${QDRANT_PORT:6334}}`, collection `cat-questions`, `initialize-schema: true`.
- Multipart uploads up to 100MB (`spring.servlet.multipart.*` + `server.tomcat.max-swallow-size`) for future PDF/CSV ingestion.
- `.env` loading via `spring.config.import: optional:file:.env[.properties]`.
- Gemini chat: `spring.ai.google.genai.chat.model: gemini-3.1-flash-lite`, `temperature: 0.3`.
- Gemini embedding: `spring.ai.google.genai.embedding.model: gemini-embedding-002`.
- Actuator: health details always shown; `health,info,metrics,prometheus` exposed.

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: ${QDRANT_HOST:localhost}
        port: ${QDRANT_GRPC_PORT:${QDRANT_PORT:6334}}   # gRPC (6334), NOT HTTP (6333)
        use-tls: ${QDRANT_USE_TLS:false}
        api-key: ${QDRANT_API_KEY:}
        collection-name: ${QDRANT_COLLECTION:cat-questions}
        initialize-schema: true
    google:
      genai:
        api-key: ${GEMINI_API_KEY}             # used by chat autoconfig
        chat:
          model: gemini-3.1-flash-lite
          temperature: 0.3
        embedding:
          api-key: ${GEMINI_API_KEY}           # MUST be set separately
          model: gemini-embedding-002
```

**Key learnings:**
1. Chat and embedding have **separate property classes** with separate prefixes — both need `api-key` explicitly.
2. Qdrant Spring AI starter talks **gRPC**, so the port is **6334**, not the dashboard HTTP port 6333. The yaml accepts `QDRANT_GRPC_PORT` first, falling back to `QDRANT_PORT` for backward compatibility.

### `application-ollama.yaml` (ollama profile)
- Activates on `--spring.profiles.active=ollama`.
- Disables Gemini autoconfigure classes (chat + embedding).
- Configures Ollama base URL, chat model, embedding model via env vars.
- Longer HTTP timeouts via `OllamaWebClientConfig` for large local models.

### `.env` (secrets — gitignored)
```dotenv
GEMINI_API_KEY=<your-key>
```

---

## 7. API Surface

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/cat/ask` | None (permitAll) | Ask a CAT question via Gemini. Body: `ChatRequest { question }`. Returns `ChatResponse { answer, sources, tokensUsed, ai }`. |
| POST | `/api/v1/local/ask` | None (permitAll) | Same as above but routes through Ollama (only when `ollama` profile is active). |
| POST | `/api/v1/ingest/pyq` | None (permitAll) | Ingest PYQ question groups into Qdrant. Body: `PyqIngestionRequest { questionGroups: [...] }`. Returns `201 Created` with `PyqIngestionResponse { message, ingestedCount }`. |
| GET | `/actuator/health` | None | Aggregates Spring health + custom `QdrantHealthIndicator` (collection name + indexed vector count + status). |
| GET | `/actuator/prometheus` | None | Prometheus scrape endpoint. |
| GET | `/swagger-ui/` | None | API docs (springdoc-openapi). |

### Sample request payloads
Both example payloads live under `src/main/resources/examples/`:
- `api-v1-cat-ask-request.json`
- `api-v1-ingest-pyq-request.json`

### Error response format (from `GlobalExceptionHandler`)
```json
{
  "timestamp": "2026-06-05T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/cat/ask",
  "details": { "fieldErrors": { "question": "must not be blank" } }
}
```

Handler matrix:

| Exception | HTTP | Code |
|-----------|------|------|
| `CatifyException` | from `ex.getStatus()` | from `ex.getCode()` (e.g. `400`, `500`) |
| `MethodArgumentNotValidException` | `400` | `VALIDATION_ERROR` (with `fieldErrors`) |
| `NonTransientAiException` (Spring AI) | `502` | `AI_PROVIDER_ERROR` |
| `Exception` (catch-all) | `500` | `INTERNAL_ERROR` (generic message, no stack trace leak) |

---

## 8. PYQ Ingestion Pipeline (US-005/US-006 scaffolding)

`DataIngestionService.ingestPyq(...)` flow:
1. Validate `PyqIngestionRequest.questionGroups` is non-empty → otherwise throws `CatifyException(400, "Empty request received to ingest data")`.
2. For each `QuestionGroup` (section / topic / subtopic / optional context), build one `Document` per `Question`:
   - Text = header + type/difficulty + context + question stem + lettered options + answer.
   - Metadata (Qdrant payload) = `section`, `topic`, `subtopic`, `type`, `difficulty` — all driven by constants in `CatifyConstants.CatifyGenericConstants`.
3. `VectorStore.add(documents)` performs embedding (Gemini) and upserts into Qdrant collection `cat-questions`.
4. Returns `PyqIngestionResponse("PYQ data ingested successfully", ingestedCount)`.

> Deduplication via content hash and CSV-driven ingestion (the original US-005 scope) are **not yet implemented**; the current branch covers the JSON-payload path used to seed Qdrant for RAG testing.

---

## 9. Chat Flow (Gemini default)

`CatChatService.ask(...)`:
1. Builds a `ChatClient` once, with a CAT-only system prompt that politely refuses off-topic questions.
2. Calls `chatClient.prompt().user(request.question()).call().chatClientResponse()`.
3. If the answer is null/empty → `CatifyException(500, AI_EMPTY_RESPONSE_MESSAGE, INTERNAL_SERVER_ERROR)`.
4. Maps `ChatResponseMetadata` to `AiMetadata` via MapStruct (`AiMetadataMapper`) → exposes `promptTokens`, `completionTokens`, `totalTokens`.
5. Returns `ChatResponse(answer, sources=[], tokensUsed, ai)`. `sources` is reserved for RAG (US-006).

---

## 10. How to Run

### Prerequisites
- Java 21 installed
- Docker Desktop running
- IntelliJ IDEA (or any Java IDE)
- `GEMINI_API_KEY` available (in `.env` or environment)

### Default Profile (Gemini — no profile flag needed)

```powershell
# 1. Start infra (compose ports: postgres 5432, qdrant 6333/6334, redis 6379, pgadmin 5050)
docker-compose up -d postgres qdrant redis pgadmin

# 2. Run app — Gemini is the default, no profile flag required
.\mvnw.cmd spring-boot:run

# 3. Smoke test the chat endpoint
curl -X POST http://localhost:8080/api/v1/cat/ask `
  -H "Content-Type: application/json" `
  -d '{"question": "What is the CAT exam syllabus?"}'

# 4. Smoke test the ingest endpoint
curl -X POST http://localhost:8080/api/v1/ingest/pyq `
  -H "Content-Type: application/json" `
  --data "@src/main/resources/examples/api-v1-ingest-pyq-request.json"
```

### Ollama Profile (local LLM)

```powershell
# 1. Ensure Ollama is running (WSL or native) and pull a model:
ollama pull llama3.2

# 2. Run app with the ollama profile
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=ollama"

# 3. Test
curl -X POST http://localhost:8080/api/v1/local/ask `
  -H "Content-Type: application/json" `
  -d '{"question": "What is VARC in CAT?"}'
```

### Full stack (containerized API)

```powershell
docker-compose up -d --build
# API on :8080, container talks to qdrant:6334 (gRPC), postgres:5432, redis:6379
```

---

## 11. Key Decisions & Learnings

| Decision | Detail |
|----------|--------|
| Model change | `gemini-2.0-flash` → `gemini-3.1-flash-lite` (old model has 0 free quota). See ADR-003. |
| Gemini in `application.yaml` | No separate `application-gemini.yaml` — Gemini config is in the default profile file. |
| Ollama as separate profile | `application-ollama.yaml` activates only with `--spring.profiles.active=ollama` and excludes Gemini autoconfigure. |
| Prod profile deferred | Claude + OpenAI embeddings will be configured when ready to deploy; not in scope for Sprint 2. |
| Embedding dual api-key | `spring.ai.google.genai.embedding.api-key` must be set separately from chat. |
| Qdrant uses gRPC | Spring AI Qdrant starter talks gRPC on port **6334** (not the HTTP dashboard on 6333). Compose exposes both. |
| Localhost defaults | `application.yaml` defaults to `localhost`; Docker Compose overrides via env vars (`QDRANT_HOST=qdrant`, `DB_HOST=postgres`, `REDIS_HOST=redis`). |
| MapStruct for AI metadata | Spring AI `ChatResponseMetadata` → DTO `AiMetadata` via `AiMetadataMapper(componentModel="spring")`. Lombok + MapStruct binding configured in `pom.xml`. |
| Custom `QdrantHealthIndicator` | Reports collection name, indexed vectors, and status with a 2s timeout — surfaces Qdrant outages clearly via `/actuator/health`. |
| Generic `GlobalExceptionHandler` | Single advice catches `CatifyException`, validation errors, Spring AI `NonTransientAiException`, and unknown exceptions; returns a uniform `ApiError` record. |
| Spring AI version | `2.0.0-SNAPSHOT` — required by Spring Boot 4.0.6, no GA yet. Spring milestone + snapshot repos pinned in `pom.xml`. |

---

## 12. Free Tier Quotas (Gemini — as of June 2026)

| Model | RPM | RPD | Use |
|-------|-----|-----|-----|
| `gemini-3.1-flash-lite` | 15 | 500 | Chat |
| `gemini-embedding-002` | 100 | 1000 | Embeddings |

Monitor at: https://ai.dev/rate-limit

---

## 13. Docker Services

| Container | Image | Host Port | Purpose |
|-----------|-------|-----------|---------|
| catify-api | (built from `Dockerfile`) | 8080 | App (compose only) |
| catify-postgres | postgres:16-alpine | 5432 | Relational data |
| catify-qdrant | qdrant/qdrant:latest | 6333 (HTTP), 6334 (gRPC) | Vector store |
| catify-redis | redis:7-alpine | 6379 | Caching |
| catify-pgadmin | dpage/pgadmin4:latest | 5050 | DB admin UI (auto-imports `Catify Postgres`) |

**Dev workflow:** Run infra containers only, run the app from IntelliJ for hot reload + debugger.

---

## 14. What's Next

| Sprint | Stories | Goal |
|--------|---------|------|
| Sprint 2 (current) | US-004, US-005, US-006, US-007, US-012 | Qdrant wiring ✅, PYQ ingestion 🔄, RAG retrieval ⏳, guardrails ⏳, exception handling ✅ |
| Sprint 3 | US-008, US-009, US-010 | Conversation memory, SSE streaming, API key auth |
| Sprint 4 | US-011, US-013, US-014, US-015 | Rate limiting, metrics dashboards, Docker hardening, full test coverage |

---

## 15. Known Risks

1. **Spring AI 2.0.0-SNAPSHOT** — unstable, property prefixes may change at GA.
2. **Free tier quotas can change** — Google may revoke models without notice.
3. **Spring Boot 4.x is bleeding edge** — limited community support; most blogs cover 3.x.
4. **No conversation memory yet** — every request is stateless (no history).
5. **Prod profile not yet created** — `application-prod.yaml` (Claude + OpenAI) is deferred; do not reference it in code until Sprint 3/4.
6. **Ingestion has no dedup yet** — repeated `POST /api/v1/ingest/pyq` calls will create duplicate vectors. Content-hash dedup is part of the remaining US-005 scope.
7. **All `/api/v1/**` endpoints are `permitAll`** — fine for dev; auth comes in US-010 (Sprint 3). Do not deploy publicly until then.

---

## 16. Reference Documents

| Document | Path | Purpose |
|----------|------|---------|
| ADR-001 | `scrum-workspace/adr/ADR-001-dev-llm-google-gemini.md` | Original LLM decision |
| ADR-002 | `scrum-workspace/adr/ADR-002-postgres-relational-db.md` | Postgres as relational store |
| ADR-003 | `scrum-workspace/adr/ADR-003-gemini-model-change-and-snapshot-workarounds.md` | Model change + Spring AI snapshot learnings |
| Product Backlog | `scrum-workspace/backlog/PRODUCT_BACKLOG.md` | All user stories |
| Scrum Board | `scrum-workspace/SCRUM_BOARD.md` | Current sprint status |
| Sprint 1 | `scrum-workspace/sprints/SPRINT_1.md` | Sprint 1 details (closed) |
| Sprint 2 | `scrum-workspace/sprints/SPRINT_2.md` | Sprint 2 details (active) |
