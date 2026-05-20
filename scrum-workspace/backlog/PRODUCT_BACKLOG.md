# 📋 Product Backlog — CAT AI Assistant

> Last updated: May 16, 2026  
> Total Stories: 16 | Total Points: 89

---

## Priority Legend

| Priority | Meaning                    |
|----------|----------------------------|
| **P0**   | Must have — MVP blocker    |
| **P1**   | Should have — core feature |
| **P2**   | Could have — enhancement   |
| **P3**   | Nice to have — future      |

---

## 🏗️ Epic 1: Project Foundation [infra]

### US-001: Spring Boot Project Setup [infra]

**Priority:** P0  
**Points:** 3  
**Assignee:** vagrover  

**As a** developer,  
**I want** a Spring Boot project with all required dependencies,  
**So that** I have a working skeleton to build features on.

**Acceptance Criteria:**
- [ ] Spring Boot 3.x project created with Java 21
- [ ] `pom.xml` includes all dependencies (Spring AI, Qdrant, PostgreSQL, Redis, Actuator, Security, Validation)
- [ ] Spring AI BOM is configured for version management
- [ ] Project compiles and starts (with `spring.main.allow-bean-definition-overriding=true` if needed)
- [ ] `application.yml`, `application-dev.yml`, `application-prod.yml` exist
- [ ] Package structure matches `projectStart.md`

---

### US-002: Docker Compose Infrastructure [infra]

**Priority:** P0  
**Points:** 3  
**Assignee:** prateekarora7  

**As a** developer,  
**I want** a Docker Compose setup with Qdrant, PostgreSQL, and Redis,  
**So that** I can run the full stack locally with one command.

**Acceptance Criteria:**
- [ ] `docker-compose.yml` includes Qdrant (ports 6333, 6334), PostgreSQL (port 5432), Redis (port 6379)
- [ ] All services use named volumes for data persistence
- [ ] PostgreSQL uses environment variables for credentials (not hardcoded)
- [ ] `docker-compose up -d` starts all services successfully
- [ ] Qdrant dashboard accessible at `http://localhost:6333/dashboard`
- [ ] `.env.example` file provided with placeholder values

---

## 🤖 Epic 2: AI Integration [ai]

### US-003: LLM Integration (Gemini Dev / Claude Prod) [ai]

**Priority:** P0  
**Points:** 5  
**Assignee:** vagrover  

**As a** developer,  
**I want** to integrate an LLM via Spring AI with profile-based provider switching,  
**So that** I can develop for free (Gemini) and deploy with Claude in production.

**Acceptance Criteria:**
- [x] `CatChatService` created with `ChatClient` using constructor injection
- [x] System prompt configured: CAT-only assistant behavior
- [x] `application-gemini.yaml` uses Google GenAI (`gemini-3.1-flash-lite`) with `${GEMINI_API_KEY}`
- [ ] `application-prod.yml` uses Claude API with `${ANTHROPIC_API_KEY}`
- [x] `spring-ai-starter-model-google-genai` in `pom.xml` (Spring AI 2.0.0-SNAPSHOT)
- [x] Basic `/api/v1/cat/ask` endpoint works with gemini profile
- [ ] Error handling: returns friendly message if LLM API is down
- [ ] Unit test with mocked ChatClient

**Technical Notes:**
- See ADR-001 for original decision rationale
- See ADR-003 for model change (`gemini-2.0-flash` → `gemini-3.1-flash-lite`) and SNAPSHOT workarounds
- Spring AI's `ChatClient` abstraction makes provider swapping config-only
- Free tier: 15 RPM, 500 RPD — sufficient for development
- Embedding autoconfig excluded due to SNAPSHOT bug (ADR-003)

---

### US-004: Qdrant Vector Store Setup [ai]

**Priority:** P0  
**Points:** 5  
**Assignee:** prateekarora7  
**Status:** 🔨 In Progress

**As a** developer,  
**I want** Qdrant configured as the vector store via Spring AI,  
**So that** I can store and search CAT question embeddings.

**Acceptance Criteria:**
- [x] `spring-ai-starter-vector-store-qdrant` configured in `pom.xml`
- [x] Qdrant connects successfully on startup (gemini profile → localhost:6333)
- [x] `application-gemini.yaml`: `gemini-embedding-002` with `embedding.api-key` set
- [ ] `application-prod.yml`: OpenAI `text-embedding-3-small` (1536 dims, paid)
- [ ] Collection `cat-questions` auto-created on startup (`initialize-schema: true`)
- [ ] Can programmatically add a `Document` to Qdrant and retrieve it via similarity search
- [ ] Qdrant dashboard shows the collection with vectors
- [ ] Connection failure handled gracefully (log error, don't crash)

**Technical Notes:**
- Chat and embedding use **separate config prefixes** — both must have `api-key` set explicitly:
  - `spring.ai.google.genai.api-key` → chat
  - `spring.ai.google.genai.embedding.api-key` → embedding (must be set separately)
- No autoconfigure excludes needed
- Dev embedding model: `gemini-embedding-002` (100 RPM, 1K RPD free tier)
- Prod: OpenAI `text-embedding-3-small` (1536 dims) — separate Qdrant collection from dev
- See ADR-003 for full investigation history

---

### US-005: Data Ingestion — CSV Loader [data]

**Priority:** P0  
**Points:** 8  
**Assignee:** prateekarora7  

**As a** developer,  
**I want** to ingest CAT questions from CSV files into Qdrant,  
**So that** the RAG system has a knowledge base to search.

**Acceptance Criteria:**
- [ ] `CatDataIngestionService` reads CSV files from `cat-data/` directory
- [ ] Each question is converted to a `Document` with metadata (year, topic, difficulty)
- [ ] Documents are batch-inserted into Qdrant (50 per batch)
- [ ] Content hash computed for deduplication (skip if hash already exists)
- [ ] Ingestion endpoint: `POST /api/v1/cat/ingest` (protected, admin only)
- [ ] Logs: total questions processed, duplicates skipped, errors
- [ ] At least one sample CSV file provided with 10+ questions

---

### US-006: RAG Search Service [ai]

**Priority:** P0  
**Points:** 5  
**Assignee:** vagrover  

**As a** CAT aspirant,  
**I want** my questions matched against the CAT question bank,  
**So that** I get answers grounded in real CAT exam data.

**Acceptance Criteria:**
- [ ] `CatRagService` performs similarity search on Qdrant with `topK=5`, `threshold=0.7`
- [ ] Supports optional metadata filters: `year` (integer) and `topic` (string)
- [ ] Uses `FilterExpressionBuilder` for Qdrant payload filtering
- [ ] Returns concatenated context string from matched documents
- [ ] If no results found, returns empty string (not null)
- [ ] Unit test with mocked VectorStore

---

### US-007: Guardrail Service [ai]

**Priority:** P0  
**Points:** 5  
**Assignee:** vagrover  

**As a** CAT aspirant,  
**I want** the assistant to reject non-CAT questions politely,  
**So that** the system stays focused and doesn't waste API credits.

**Acceptance Criteria:**
- [ ] `CatGuardrailService` first checks against keyword set (fast path)
- [ ] If no keyword match, falls back to LLM-based classification (YES/NO)
- [ ] Guardrail results are cached with `@Cacheable` (Redis)
- [ ] If LLM is down, fail-safe: reject the question
- [ ] Non-CAT response: "I can only help with CAT India exam questions..."
- [ ] Unit tests: CAT question → true, non-CAT → false, LLM failure → false

---

## 💬 Epic 3: Chat Features [api]

### US-008: Conversation Memory [ai]

**Priority:** P1  
**Points:** 5  
**Assignee:** prateekarora7  

**As a** CAT aspirant,  
**I want** the assistant to remember my previous messages in a session,  
**So that** I can have multi-turn conversations.

**Acceptance Criteria:**
- [ ] `ChatRequest` includes optional `sessionId` field
- [ ] `MessageChatMemoryAdvisor` configured with `ChatMemory` (in-memory or PostgreSQL-backed)
- [ ] Same `sessionId` → assistant remembers context from earlier messages
- [ ] New/null `sessionId` → fresh conversation
- [ ] Chat history stored in PostgreSQL (`chat_sessions` + `chat_messages` tables)
- [ ] Memory limited to last 20 messages per session

---

### US-009: Streaming Responses (SSE) [api]

**Priority:** P1  
**Points:** 5  
**Assignee:** vagrover  

**As a** CAT aspirant,  
**I want** answers to stream in real-time,  
**So that** I don't wait for the full response to appear.

**Acceptance Criteria:**
- [ ] New endpoint: `POST /api/v1/cat/ask/stream` with `produces = text/event-stream`
- [ ] Returns `Flux<String>` from `chatClient.stream().content()`
- [ ] Works with `curl` and browser EventSource
- [ ] Guardrail check still happens before streaming
- [ ] Spring WebFlux dependency added (or `spring-boot-starter-webflux`)
- [ ] Error during stream → send error event and close

---

## 🔒 Epic 4: Security & Reliability [security]

### US-010: API Key Authentication [security]

**Priority:** P1  
**Points:** 5  
**Assignee:** prateekarora7  

**As an** API consumer,  
**I want** endpoints protected by API key authentication,  
**So that** only authorized users can access the service.

**Acceptance Criteria:**
- [ ] `ApiKeyAuthFilter` reads `X-API-Key` header
- [ ] Valid keys stored in config (later: database)
- [ ] Invalid/missing key → `401 Unauthorized` with JSON error body
- [ ] `/actuator/health` is publicly accessible (no auth required)
- [ ] `SecurityConfig` configures the filter chain
- [ ] API key in config loaded from `${CAT_API_KEY}` env var

---

### US-011: Rate Limiting [security]

**Priority:** P2  
**Points:** 3  
**Assignee:** vagrover  

**As a** system administrator,  
**I want** per-user rate limiting,  
**So that** no single user can exhaust our LLM API budget.

**Acceptance Criteria:**
- [ ] Bucket4j configured: 20 requests/minute per API key
- [ ] Rate exceeded → `429 Too Many Requests` with `Retry-After` header
- [ ] Rate limit config externalized in `application.yml`
- [ ] Actuator metrics: rate limit hits counter

---

### US-012: Global Exception Handling [api]

**Priority:** P1  
**Points:** 3  
**Assignee:** prateekarora7  

**As a** developer,  
**I want** consistent error responses across all endpoints,  
**So that** API consumers get predictable error formats.

**Acceptance Criteria:**
- [ ] `GlobalExceptionHandler` with `@RestControllerAdvice`
- [ ] Handles: `NonCatQuestionException` → 400, `VectorStoreException` → 503, `Exception` → 500
- [ ] Error response format: `{ "error": "message", "code": "ERROR_CODE", "timestamp": "..." }`
- [ ] Validation errors (`@Valid`) → 400 with field-level details
- [ ] No stack traces in production responses

---

## 📊 Epic 5: Observability [observability]

### US-013: Health Checks & Metrics [observability]

**Priority:** P2  
**Points:** 3  
**Assignee:** vagrover  

**As a** system administrator,  
**I want** health checks and Prometheus metrics,  
**So that** I can monitor the system in production.

**Acceptance Criteria:**
- [ ] Spring Actuator enabled: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- [ ] Custom health indicator for Qdrant connectivity
- [ ] Micrometer metrics: LLM call latency, vector search latency, guardrail rejection rate
- [ ] `micrometer-registry-prometheus` dependency added
- [ ] Health endpoint returns status of: Qdrant, PostgreSQL, Redis

---

## 🚀 Epic 6: Deployment [deployment]

### US-014: Dockerize Application [deployment]

**Priority:** P2  
**Points:** 3  
**Assignee:** prateekarora7  

**As a** developer,  
**I want** a multi-stage Dockerfile for the Spring Boot app,  
**So that** I can deploy it as a container.

**Acceptance Criteria:**
- [ ] Multi-stage Dockerfile: build (Maven) → runtime (JRE 21 slim)
- [ ] Final image < 300MB
- [ ] Exposes port 8080
- [ ] Accepts environment variables for all secrets
- [ ] `.dockerignore` file present
- [ ] `docker build` and `docker run` work end-to-end

---

### US-015: Integration Tests [testing]

**Priority:** P2  
**Points:** 5  
**Assignee:** vagrover  

**As a** developer,  
**I want** integration tests with Testcontainers,  
**So that** I can verify the full flow against real Qdrant and PostgreSQL.

**Acceptance Criteria:**
- [ ] Testcontainers for Qdrant + PostgreSQL configured
- [ ] Test: ingest a document → search it → verify result
- [ ] Test: guardrail rejects non-CAT question
- [ ] Tests run in CI without external dependencies
- [ ] `application-test.yml` profile for test config

---

### US-016: Kubernetes Manifests [deployment]

**Priority:** P3  
**Points:** 5  
**Assignee:** Unassigned  

**As a** DevOps engineer,  
**I want** Kubernetes deployment manifests,  
**So that** the app can be deployed to a K8s cluster.

**Acceptance Criteria:**
- [ ] `k8s/deployment.yml` — Pod spec with resource limits, health probes
- [ ] `k8s/service.yml` — ClusterIP service on port 8080
- [ ] `k8s/configmap.yml` — Non-secret configuration
- [ ] `k8s/hpa.yml` — Horizontal Pod Autoscaler (CPU 70% → scale 2–5 pods)
- [ ] Secrets managed via K8s Secrets (not in manifests)

---

## 📊 Backlog Summary

| Story  | Title                          | Priority | Points | Sprint   |
|--------|--------------------------------|----------|--------|----------|
| US-001 | Spring Boot Project Setup      | P0       | 3      | Sprint 1 |
| US-002 | Docker Compose Infrastructure  | P0       | 3      | Sprint 1 |
| US-003 | LLM Integration (Gemini/Claude)| P0       | 5      | Sprint 1 |
| US-004 | Qdrant Vector Store Setup      | P0       | 5      | Sprint 1 |
| US-005 | Data Ingestion — CSV Loader    | P0       | 8      | Sprint 2 |
| US-006 | RAG Search Service             | P0       | 5      | Sprint 2 |
| US-007 | Guardrail Service              | P0       | 5      | Sprint 2 |
| US-008 | Conversation Memory            | P1       | 5      | Sprint 3 |
| US-009 | Streaming Responses (SSE)      | P1       | 5      | Sprint 3 |
| US-010 | API Key Authentication         | P1       | 5      | Sprint 3 |
| US-011 | Rate Limiting                  | P2       | 3      | Sprint 4 |
| US-012 | Global Exception Handling      | P1       | 3      | Sprint 2 |
| US-013 | Health Checks & Metrics        | P2       | 3      | Sprint 4 |
| US-014 | Dockerize Application          | P2       | 3      | Sprint 4 |
| US-015 | Integration Tests              | P2       | 5      | Sprint 4 |
| US-016 | Kubernetes Manifests           | P3       | 5      | Backlog  |

**Total: 76 story points across 4 sprints + backlog**
