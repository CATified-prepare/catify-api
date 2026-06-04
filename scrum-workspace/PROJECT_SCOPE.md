# Project Scope — Catify (CAT AI Assistant API)

**Version:** 1.0
**Last Updated:** June 5, 2026
**Owner:** Architect (Catify team)
**Status:** Active — Sprint 2 in flight

---

## 1. Document Purpose

This document defines the **full scope** of the Catify project: what we are building, what we are **not** building, who it is for, the architecture we have committed to, the work breakdown across sprints, and the constraints / risks we accept. It is the single source of truth for scope decisions; all backlog items, sprint plans, and architecture decisions must trace back to this document.

Reference companions:
- `KT_DOCUMENT.md` — current state of the codebase.
- `projectStart.md` — original architecture brief.
- `backlog/PRODUCT_BACKLOG.md` — story-level breakdown.
- `sprints/SPRINT_*.md` — sprint plans.
- `adr/ADR-*.md` — architectural decision records.

---

## 2. Vision & Problem Statement

### 2.1 Problem
CAT (Common Admission Test) aspirants in India rely on scattered prep material — PDFs of past year questions (PYQs), coaching notes, forum threads, YouTube. There is no single conversational assistant that:
1. Is **strictly grounded** in real CAT exam content (not generic LLM hallucinations).
2. **Refuses** off-topic questions to keep cost and quality predictable.
3. Can be self-hosted or run on free-tier LLM APIs by an indie team.

### 2.2 Vision
> *Catify is a production-grade, domain-specific AI API that answers CAT questions accurately, cites the source PYQ section/topic, runs cheaply on free tiers during dev, and scales to a managed Claude + Qdrant Cloud deployment without rewrites.*

### 2.3 Elevator pitch
A Spring Boot REST API that wraps an LLM with:
- A **CAT-only system prompt** + **guardrail** layer.
- A **Qdrant-backed RAG** pipeline seeded from PYQ data.
- **Pluggable LLM providers** (Gemini for dev, Ollama for offline, Claude for prod).
- **Production hardening**: auth, rate limits, observability, structured errors.

---

## 3. Goals & Non-Goals

### 3.1 In-scope goals (what we **will** deliver)

| # | Goal | How we measure it |
|---|------|-------------------|
| G1 | Domain-restricted Q&A over CAT content | Guardrail rejects ≥95% of synthetic non-CAT prompts in test set |
| G2 | RAG-grounded answers with citations | Each chat response includes `sources[]` with section/topic/year |
| G3 | Multi-provider LLM via Spring AI profiles | Identical request works on `default` (Gemini), `ollama`, and (future) `prod` (Claude) |
| G4 | PYQ ingestion pipeline | `POST /api/v1/ingest/pyq` (JSON) and CSV loader populate Qdrant; idempotent via content hash |
| G5 | Conversational memory | Same `sessionId` retains last 20 messages across calls |
| G6 | Streaming UX | `POST /api/v1/cat/ask/stream` returns SSE token stream |
| G7 | API security | API-key auth + per-key rate limiting (20 RPM) |
| G8 | Observability | Prometheus metrics + custom Qdrant health indicator |
| G9 | Container deploy | Single `docker-compose up` for full local stack; image < 300 MB |
| G10 | Test coverage on critical paths | Unit tests for services + Testcontainers integration test for ingest→search |

### 3.2 Out-of-scope (what we will **not** build in v1)

- ❌ A web/mobile UI (this project is **API-only**; consumers build their own UI).
- ❌ User accounts, sign-up flows, OAuth, or social login (API-key only).
- ❌ Payment / subscription billing.
- ❌ Fine-tuning or training a custom CAT model — we only use RAG + commercial LLMs.
- ❌ Speech-to-text or text-to-speech.
- ❌ Multilingual support beyond English (Hindi/regional comes post-v1).
- ❌ Image / PDF rendering of math equations (we accept LaTeX strings as-is).
- ❌ Coaching center management (cohorts, mock-test scheduling, leaderboards).
- ❌ Mobile push notifications, email delivery.
- ❌ Multi-tenant SaaS (one deployment = one tenant for v1).
- ❌ Kubernetes manifests in v1 (US-016 is P3, deferred to backlog).

### 3.3 Deferred to a future release (post-v1)

- Conversation export / share links.
- Adaptive difficulty (track user accuracy and bias retrieval).
- A/B testing framework for prompt variations.
- Section-wise mock-test generation from the PYQ corpus.
- Admin dashboard for ingestion + usage analytics.

---

## 4. Target Users / Personas

| Persona | Description | Primary need |
|---------|-------------|--------------|
| **Aarav — CAT aspirant (final year undergrad)** | Self-prepping at home, limited budget for coaching. | Quick, accurate answers to PYQs with source citation. |
| **Riya — Working professional** | Limited prep time (1–2 hrs/night), targeting CAT 2026. | Topic-filtered Q&A (`year`, `topic`) and multi-turn explanations. |
| **Coaching center developer** | Wants to embed Catify into a custom learning portal. | Stable REST API, API-key auth, rate limits, OpenAPI docs. |
| **Catify ops/admin** | The two devs maintaining the system. | Health checks, metrics, structured error logs, predictable costs. |

We are **not** targeting school students, GRE/GMAT/IELTS aspirants, or general MBA admissions counselling.

---

## 5. Functional Scope

### 5.1 Core capabilities (must-have, P0/P1)

| ID | Capability | Endpoint(s) | Story |
|----|------------|-------------|-------|
| F-01 | Ask a CAT question | `POST /api/v1/cat/ask` | US-003 ✅ |
| F-02 | Ask via local Ollama LLM | `POST /api/v1/local/ask` | US-017 ✅ |
| F-03 | Ingest PYQ data into Qdrant (JSON) | `POST /api/v1/ingest/pyq` | US-005/US-006 🔄 |
| F-04 | Ingest PYQs from CSV (admin) | `POST /api/v1/cat/ingest` | US-005 ⏳ |
| F-05 | RAG retrieval with metadata filters (`year`, `topic`) | (internal) `CatRagService` | US-006 ⏳ |
| F-06 | Reject non-CAT questions | (internal) `CatGuardrailService` | US-007 ⏳ |
| F-07 | Multi-turn conversation memory | `sessionId` on `ChatRequest` | US-008 (Sprint 3) |
| F-08 | Stream answers via SSE | `POST /api/v1/cat/ask/stream` | US-009 (Sprint 3) |
| F-09 | API-key auth | `X-API-Key` header | US-010 (Sprint 3) |
| F-10 | Per-key rate limiting | (filter) | US-011 (Sprint 4) |
| F-11 | Standard error responses | All endpoints | US-012 ✅ |
| F-12 | Health + Prometheus metrics | `/actuator/health`, `/actuator/prometheus` | US-013 ✅ (partial) |
| F-13 | OpenAPI / Swagger UI | `/swagger-ui/` | (built-in) ✅ |

### 5.2 Request/response contracts (canonical)

**Chat request** (`POST /api/v1/cat/ask`)
```json
{
  "question": "Solve this permutation problem from CAT 2023...",
  "sessionId": "optional-uuid",
  "year": 2023,
  "topic": "Quant"
}
```

**Chat response**
```json
{
  "answer": "...grounded answer with citation...",
  "sources": [
    { "section": "Quant", "topic": "Permutations", "subtopic": "...", "year": 2023 }
  ],
  "tokensUsed": 482,
  "ai": { "promptTokens": 312, "completionTokens": 170, "totalTokens": 482 }
}
```

**Ingest request** (`POST /api/v1/ingest/pyq`)
```json
{
  "questionGroups": [
    {
      "section": "VARC",
      "topic": "Reading Comprehension",
      "subtopic": "Inference",
      "context": "Optional shared passage",
      "questions": [
        { "type": "MCQ", "difficulty": "Medium", "problem": "...", "options": ["A","B","C","D"], "ans": "B" }
      ]
    }
  ]
}
```

**Standard error envelope** (all endpoints)
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

### 5.3 Behavior rules

- The system prompt MUST politely refuse off-topic questions with a fixed message.
- RAG context, when retrieved, MUST be passed to the LLM with explicit instructions to **only** use it.
- An empty similarity-search result MUST NOT crash; it returns a polite "no relevant CAT context found" answer.
- Token usage and AI provider metadata MUST be returned on every successful chat call (cost transparency).
- All write paths (ingestion) MUST be idempotent under content-hash dedup (post-US-005).

---

## 6. Non-Functional Requirements (NFRs)

| Category | Requirement | Target |
|----------|-------------|--------|
| **Performance** | p50 chat latency (non-streaming, RAG on, Gemini) | < 4 s |
| | p95 chat latency | < 8 s |
| | Vector similarity search latency (Qdrant local) | < 100 ms for top-5 over 50 K vectors |
| | Ingest throughput | ≥ 50 docs/batch, ≥ 500 docs/min |
| **Scalability** | Vector store size (v1 target) | up to 50 K vectors |
| | Concurrent users (v1) | 50 RPS sustained on a 2-vCPU container |
| **Availability** | Local dev | best-effort |
| | Production target (post-deploy) | 99.0% monthly |
| **Security** | Secrets | only via env vars / `.env` (gitignored); never committed |
| | Auth | API-key (`X-API-Key`) starting Sprint 3 |
| | Transport | TLS terminated at the gateway/ingress in prod |
| **Cost** | Dev cost | $0 (Gemini free tier + Ollama local) |
| | Prod cost ceiling per 1 K requests | < $1 (Claude Haiku class or Sonnet capped) |
| **Observability** | Metrics | Prometheus scrape on `/actuator/prometheus` |
| | Health | `/actuator/health` reports Qdrant, Postgres, Redis |
| | Logging | Structured JSON in prod profile (post-deploy) |
| **Compliance / data** | PII | none stored beyond optional `sessionId` (UUID) and chat transcripts |
| | Data retention (chat history) | 90 days default, configurable |
| | Question bank licensing | only PYQs that are publicly available / cleared for educational use |
| **Portability** | Profile switch (LLM provider) | config-only, no code change |
| **Maintainability** | Test coverage on services | ≥ 70% line coverage on `service/` package |
| | Build time | `mvn clean verify` < 3 min on dev laptop |

---

## 7. Architecture & Tech Stack (committed)

### 7.1 High-level flow
```
Client → API-Key filter (Sprint 3) → Rate Limiter (Sprint 4)
       → ChatController → CatGuardrailService → CatRagService (Qdrant)
       → CatChatService (ChatClient + ChatMemory)
       → LLM provider (Gemini default | Ollama | Claude prod)
       → ChatResponse (answer + sources + AI metadata)
```

### 7.2 Tech stack (final for v1)

| Layer | Choice | Why |
|-------|--------|-----|
| Language / runtime | Java 21 (Amazon Corretto) | Modern features (records, pattern matching), LTS. |
| Framework | Spring Boot 4.0.6 | Aligned with Spring AI 2.x. |
| Web | Spring MVC + WebFlux (`WebClient` only) | MVC for endpoints; WebFlux client for Ollama. |
| AI orchestration | Spring AI 2.0.0-SNAPSHOT | Provider-agnostic `ChatClient` + `VectorStore`. |
| LLM (dev default) | Google Gemini `gemini-3.1-flash-lite` | Free tier, sufficient quota for dev. |
| LLM (offline) | Ollama (any model) | Zero-cost local inference. |
| LLM (prod, deferred) | Anthropic Claude `claude-sonnet-4-20250514` | Best quality at cost target. |
| Embeddings (dev) | Gemini `gemini-embedding-002` | Free tier. |
| Embeddings (prod, deferred) | OpenAI `text-embedding-3-small` (1536 dims) | Cheap, well-supported in Qdrant. |
| Vector DB | Qdrant (gRPC) | Native filtering, payload indexing, scales beyond pgvector. ADR-002 / projectStart.md §"Why Qdrant". |
| Relational DB | PostgreSQL 16 | Chat history, audit, future user data. |
| Cache | Redis 7 | Guardrail cache, embedding cache. |
| DTO mapping | MapStruct 1.5.5 + Lombok | Compile-time, no reflection. |
| API docs | springdoc-openapi 3.0.2 | Auto-generated Swagger UI. |
| Metrics | Micrometer + Prometheus registry | Industry standard. |
| Build | Maven (`mvnw` wrapper) | Spring Boot default. |
| Containerization | Docker + Compose | One-command local stack. |
| Orchestration (post-v1) | Kubernetes (manifests in backlog as US-016) | Deferred. |

### 7.3 Spring profiles

| Profile | LLM | Embedding | Activation |
|---------|-----|-----------|------------|
| `default` | Gemini | Gemini | (none — always loaded) |
| `ollama` | Ollama | Ollama | `--spring.profiles.active=ollama` |
| `prod` *(deferred)* | Claude | OpenAI | `--spring.profiles.active=prod` |

### 7.4 Module boundaries (intended)

| Package | Responsibility | May not depend on |
|---------|----------------|-------------------|
| `controller` | HTTP I/O, validation, no business logic | `service` internals beyond public methods |
| `service` | Domain logic (chat, RAG, guardrail, ingestion) | `controller`, `dto` of unrelated bounded contexts |
| `dto` | Pure records / POJOs | anything else |
| `mapper` | DTO ↔ domain mapping | `controller`, `service` |
| `exception` | Error model + handler | `controller`, `service` |
| `config` | Beans, security, health | `controller`, `service` |
| `constant` | Constants only | nothing |

---

## 8. Data Scope

### 8.1 What goes into Qdrant
- One **vector per question** (not per group).
- Document text = section + topic + subtopic + (optional) shared context + question stem + lettered options + answer.
- Payload (filterable): `section`, `topic`, `subtopic`, `type`, `difficulty`, (post-US-005) `year`, `hash`.

### 8.2 What goes into PostgreSQL
- `chat_sessions`, `chat_messages` (US-008).
- `api_usage` (US-013 / US-011).
- `api_keys` (US-010, post-MVP — env-var list initially).
- **No** PYQ content goes into Postgres (vector store only).

### 8.3 What goes into Redis
- Guardrail classification cache (key = hash of question, TTL 1 h).
- (Future) embedding cache, rate-limit buckets.

### 8.4 Data sources
- **Sprint 2:** hand-curated JSON PYQ payloads (see `src/main/resources/examples/api-v1-ingest-pyq-request.json`).
- **Sprint 2/3:** CSV ingestion of CAT 2018–2023 PYQs from public archives (US-005).
- **Post-v1:** PDF ingestion via Apache Tika (deferred).

### 8.5 Data we will NOT store
- User real names, emails, phone numbers (we only accept opaque `sessionId`).
- Payment details.
- Anything not directly tied to question retrieval or operational metrics.

---

## 9. Deliverables

### 9.1 Code
- Spring Boot service in `catify-api/` repo.
- Two active profiles (`default`, `ollama`); `prod` profile by end of Sprint 4.
- Test suite (unit + Testcontainers integration).
- `Dockerfile` (multi-stage) + `docker-compose.yml`.

### 9.2 Documentation (lives in `scrum-workspace/`)
- ✅ `KT_DOCUMENT.md` — current snapshot.
- ✅ `PROJECT_SCOPE.md` (this document).
- ✅ `projectStart.md` — original architecture brief.
- ✅ `backlog/PRODUCT_BACKLOG.md` — stories.
- ✅ `sprints/SPRINT_*.md` — sprint plans.
- ✅ `adr/ADR-*.md` — architectural decisions.
- ⏳ `docs/API.md` — published OpenAPI export (post-Sprint 3).
- ⏳ `docs/RUNBOOK.md` — operational runbook (post-Sprint 4).

### 9.3 Operational artifacts (Sprint 4)
- Prometheus scrape config example.
- Sample Grafana dashboard JSON.
- Smoke-test `curl` script.

---

## 10. Roadmap & Milestones

> 4 sprints × 2 weeks = 8 weeks of execution. Spans May 11 → Jul 6, 2026.

| Sprint | Window | Theme | Exit criteria |
|--------|--------|-------|---------------|
| **Sprint 1** ✅ | May 11 – May 24, 2026 | Foundation | App boots, Gemini chat works, infra in Compose, Ollama profile. |
| **Sprint 2** 🔄 | May 25 – Jun 8, 2026 | Qdrant + RAG + Guardrails | Qdrant collection live, PYQ ingest works, RAG injects context, guardrail rejects non-CAT, uniform errors. |
| **Sprint 3** ⏳ | Jun 9 – Jun 22, 2026 | Memory + Streaming + Auth | Multi-turn chat via `sessionId`, SSE streaming, API-key auth, Postgres-backed history. |
| **Sprint 4** ⏳ | Jun 23 – Jul 6, 2026 | Hardening + Release | Rate limiting, full metrics, Dockerfile finalized, integration tests in CI, prod profile (Claude+OpenAI). |
| **Post-v1 / Backlog** | TBD | Scale | Kubernetes manifests (US-016), Qdrant Cloud migration, PDF ingestion, admin dashboard. |

### 10.1 Milestone-level "Definition of Done"

| Milestone | Done when |
|-----------|-----------|
| **M1 — Foundation (end Sprint 1)** | Chat endpoint returns Gemini answer; infra in Compose; Ollama profile works. |
| **M2 — Grounded MVP (end Sprint 2)** | RAG retrieves real PYQ context, guardrail rejects off-topic, all errors uniform. |
| **M3 — Conversational MVP (end Sprint 3)** | Multi-turn streaming chat behind API-key auth. |
| **M4 — Production-ready v1 (end Sprint 4)** | Rate-limited, metricked, Dockerized, tested, prod profile validated against Claude. |

---

## 11. Story → Scope Traceability

Every backlog story maps to one of the goals in §3.1.

| Story | Goal | Sprint | Status |
|-------|------|--------|--------|
| US-001 Spring Boot Project Setup | foundation | 1 | ✅ |
| US-002 Docker Compose Infrastructure | foundation | 1 | ✅ |
| US-003 LLM Integration (Gemini/Claude) | G3 | 1 | ✅ (Gemini); prod deferred |
| US-017 Local LLM (Ollama) | G3 | 1 | ✅ |
| US-004 Qdrant Vector Store Setup | G2, G4 | 2 | ✅ wiring; prod embedding deferred |
| US-005 Data Ingestion — CSV Loader | G4 | 2 | 🔄 (JSON path live; CSV+dedup pending) |
| US-006 RAG Search Service | G2 | 2 | 🔄 |
| US-007 Guardrail Service | G1 | 2 | ⏳ |
| US-012 Global Exception Handling | F-11 | 2 | ✅ core in place |
| US-008 Conversation Memory | G5 | 3 | ⏳ |
| US-009 Streaming Responses (SSE) | G6 | 3 | ⏳ |
| US-010 API Key Authentication | G7 | 3 | ⏳ |
| US-011 Rate Limiting | G7 | 4 | ⏳ |
| US-013 Health Checks & Metrics | G8 | 4 | ✅ partial (Qdrant indicator + Prometheus) |
| US-014 Dockerize Application | G9 | 4 | ⏳ (Dockerfile exists; needs hardening) |
| US-015 Integration Tests | G10 | 4 | ⏳ |
| US-016 Kubernetes Manifests | post-v1 | Backlog | ❄️ deferred |

---

## 12. Constraints & Assumptions

### 12.1 Hard constraints
- **Team:** 2 part-time developers (~5 hrs/day each, ~50 hrs/sprint each → 100 hrs/sprint capacity).
- **Budget:** zero cloud spend during dev; ≤ $50/month during prod pilot.
- **Stack lock-in:** Java + Spring Boot is non-negotiable (developer familiarity).
- **Spring AI version:** locked to 2.0.0-SNAPSHOT until GA; we accept breakage risk.
- **Free-tier limits:** Gemini 15 RPM / 500 RPD chat, 100 RPM / 1 K RPD embedding.

### 12.2 Working assumptions
- The PYQ corpus we ingest is small (< 50 K vectors), so a single Qdrant node suffices.
- Users hit the API in bursts (study sessions), not as constant traffic.
- Question text is English-only; LaTeX/math is passed through as-is.
- Initial deployment is single-tenant (one Catify instance per customer).

---

## 13. Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R1 | Spring AI 2.x property prefixes change at GA | High | Medium | Pin to a snapshot; centralize config; ADR-003 documents workarounds. |
| R2 | Gemini free-tier quotas tightened/revoked | Medium | High | Ollama profile as fallback; `prod` profile (Claude) ready by Sprint 4. |
| R3 | Qdrant SNAPSHOT incompatibility with Spring AI | Low | High | Pin Qdrant image; integration test in CI on every PR (US-015). |
| R4 | RAG hallucinations despite system prompt | Medium | High | Strict prompt + guardrail + cite-or-refuse instruction; eval set of 50 known answers. |
| R5 | PYQ licensing ambiguity | Medium | High | Only ingest PYQs that are publicly published; document source per ingest batch. |
| R6 | No content-hash dedup yet → duplicate vectors | High (today) | Medium | Land US-005 dedup before v1; for now, manual collection wipes. |
| R7 | All endpoints `permitAll` until Sprint 3 | Certain | High in prod | Do not deploy publicly before US-010 is merged. |
| R8 | Single-Qdrant-node = SPOF | Medium | Medium | Acceptable for v1; plan replication when corpus > 50 K vectors. |
| R9 | Cost spike from rate-limit-bypass abuse | Medium | High | Per-key Bucket4j (US-011) + Cloudflare/WAF in front of prod. |
| R10 | Snapshot-based builds slow CI | Medium | Low | Cache Maven repo in CI; run integration tests only on `main` and PRs to `main`. |

---

## 14. Success Metrics (post-launch)

| Metric | Target (90 days post-launch) |
|--------|------------------------------|
| API uptime | ≥ 99.0% |
| Median chat latency (p50) | < 4 s |
| Guardrail precision (CAT vs non-CAT) | ≥ 95% |
| RAG citation rate (answers w/ at least one source) | ≥ 90% |
| Cost per 1 000 requests | < $1 USD |
| Test coverage on `service/` | ≥ 70% line |
| Zero P0 security incidents (key leaks, public data exposure) | required |

---

## 15. Acceptance / Sign-off

This scope is considered **accepted** when:
1. All P0 stories (US-001…US-007, US-012) are Done with passing tests.
2. P1 stories (US-008, US-009, US-010, US-013, US-017) are Done.
3. The system runs end-to-end against the `prod` profile (Claude + OpenAI embeddings) in a staging environment.
4. The four documents in §9.2 are current as of the release commit.

Sign-off: **Architect + both developers** must approve the v1 release tag.

---

## 16. Change Control

Scope changes follow this rule:
- **Editorial fixes** (typos, clarifications) → direct PR, no review needed.
- **Adding/removing a story** → update `PRODUCT_BACKLOG.md` + this scope doc + open an ADR if it changes architecture.
- **Tech-stack change** → mandatory ADR + Architect approval.
- **Re-prioritizing P0** → Product Owner + Architect approval; record in the next sprint file.

History:

| Version | Date | Author | Notes |
|---------|------|--------|-------|
| 1.0 | 2026-06-05 | Architect | Initial consolidated scope drawn from `projectStart.md`, backlog, and current code state. |

