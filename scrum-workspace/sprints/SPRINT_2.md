#  Sprint 2  Qdrant Integration + RAG Core + Guardrails

> **Sprint Duration:** 2 weeks
> **Sprint Goal:** Complete Qdrant vector store, build RAG search service, implement guardrail, and add global exception handling  making the API production-ready for CAT Q&A
> **Total Points:** 26
> **Team Capacity:** 2 devs  50 hrs = 100 hrs

---

##  Sprint Dates

- **Start:** May 25, 2026
- **End:** June 8, 2026
- **Demo:** June 8, 2026
- **Retro:** June 8, 2026

---

##  Sprint Goal

> *By the end of Sprint 2, we should be able to ingest CAT questions into Qdrant, retrieve them via similarity search, run a guardrail check on every incoming question, and return consistent error responses across all endpoints.*

---

##  Sprint Stories

### US-004: Qdrant Vector Store Setup [ai]  5 pts *(Carried from Sprint 1)*
**Assignee:** vagrover
**Status:** Done

Connect to Qdrant and verify embedding pipeline:
- Configure `spring-ai-starter-vector-store-qdrant` in `application.yaml` (default profile)
- Gemini embedding (`gemini-embedding-002`) is already configured in `application.yaml`  verify it works end-to-end
- `cat-questions` collection auto-created on startup (`initialize-schema: true`)
- Programmatically add a `Document` and retrieve it via similarity search
- Verify collection appears in Qdrant dashboard (`http://localhost:6333/dashboard`)
- Handle connection failure gracefully (log error, don't crash)
- ~~`application-prod.yml`: OpenAI `text-embedding-3-small`~~  **deferred, prod profile out of scope for Sprint 2**

**Done when:** Can add a document to Qdrant and retrieve it via similarity search using the default (Gemini) profile; collection visible in dashboard.

**Technical Notes:**
- Gemini chat + embedding are both configured in `application.yaml` (default profile  no profile flag needed to run)
- Chat and embedding still use **separate config prefixes**  both have `api-key` set explicitly in `application.yaml`:
  - `spring.ai.google.genai.api-key`  chat
  - `spring.ai.google.genai.embedding.api-key`  embedding
- `application-ollama.yaml` activates on `--spring.profiles.active=ollama` and disables Gemini autoconfigure
- Prod profile (`application-prod.yaml`) will handle Claude + OpenAI embeddings  **out of scope for now**
- See ADR-003 for full investigation history

---

### US-005: Data Ingestion  CSV Loader [data]  8 pts
**Assignee:** prateekarora7
**Status:**  In Progress
**Blocker:**  Partially blocked on US-004  `VectorStore.add()` cannot be called until vagrover confirms Qdrant collection is auto-created and embeddings return vectors. All other tasks (CSV reader, `Document` mapper, dedup hash, sample CSV, endpoint scaffold) can proceed immediately.

Ingest CAT questions from CSV files into Qdrant:
- `CatDataIngestionService` reads CSV files from `cat-data/` directory
- Each question is converted to a `Document` with metadata (year, topic, difficulty)
- Documents are batch-inserted into Qdrant (50 per batch)
- Content hash computed for deduplication (skip if hash already exists)
- Ingestion endpoint: `POST /api/v1/cat/ingest` (protected, admin only)
- Logs: total questions processed, duplicates skipped, errors
- At least one sample CSV file provided with 10+ questions

**Done when:** `POST /api/v1/cat/ingest` loads CSV data into Qdrant; duplicates are skipped; collection shows vectors in dashboard.

**Technical Notes:**
- Depends on US-004 (Qdrant collection must exist before ingesting)
- Use OpenCSV or Spring Batch for CSV reading
- Admin protection can be a simple static API key header for now (US-010 covers proper auth later)
- No profile switch needed  runs against default profile (Gemini embeddings via `application.yaml`)

---

### US-006: RAG Search Service [ai]  5 pts
**Assignee:** prateekarora7
**Status:**  In Progress
**Blocker:**  Fully blocked on US-004 (Qdrant must be connected + collection created) AND US-005 (data must be ingested  can't test similarity search on an empty collection).

Build the retrieval layer that grounds LLM answers in real CAT data:
- Create `CatRagService` with similarity search on Qdrant (`topK=5`, `threshold=0.7`)
- Support optional metadata filters: `year` (integer) and `topic` (string)
- Use `FilterExpressionBuilder` for Qdrant payload filtering
- Return concatenated context string from matched documents
- Return empty string (not null) when no results found
- Unit test with mocked `VectorStore`

**Done when:** `CatRagService` retrieves relevant CAT questions from Qdrant and passes them as context to the LLM.

**Technical Notes:**
- This story depends on US-004 being complete (Qdrant must have data)
- Wire `CatRagService` into the existing `CatChatService`  augment the prompt with retrieved context
- Keep RAG and chat concerns separate (single responsibility)
- Runs on default profile  no profile-specific config needed for Sprint 2
- Current branch work has started the ingestion side with `DataIngestionController`, `DataIngestionService`, and `PyqIngestionRequest` so Qdrant can be seeded with PYQ data before retrieval is wired in.

---

### US-007: Guardrail Service [ai]  5 pts
**Assignee:** prateekarora7
**Status:**  To Do
**Blocker:**  None  Redis is already running (US-002 done). Can start any time.

Prevent off-topic questions from hitting the LLM:
- Create `CatGuardrailService` with two-stage check:
  1. Fast path: keyword set check (e.g., "CAT", "VARC", "DILR", "quant", "MBA", etc.)
  2. Fallback: LLM-based classification (prompt returns YES/NO)
- Cache guardrail results with `@Cacheable` (Redis) to avoid repeated LLM calls
- If LLM is down  fail-safe: reject the question
- Non-CAT response: `"I can only help with CAT India exam questions. Please ask about VARC, DILR, or Quantitative Aptitude."`
- Unit tests: CAT question  pass, non-CAT  reject, LLM failure  reject

**Done when:** Non-CAT questions are rejected before reaching the main LLM; Redis caches the classification result.

**Technical Notes:**
- Inject `CatGuardrailService` into `ChatController`  check before calling `CatChatService`
- Redis must be running (`docker-compose up -d`) for caching to work
- Keyword list should be configurable via `application.yml` (not hardcoded)

---

### US-012: Global Exception Handling [api]  3 pts
**Assignee:** prateekarora7
**Status:**  To Do
**Blocker:**  None  fully independent. Best story to start on Day 1.

Consistent error responses across all endpoints:
- Create `GlobalExceptionHandler` with `@RestControllerAdvice`
- Handle:
  - `NonCatQuestionException`  `400 Bad Request`
  - `VectorStoreException`  `503 Service Unavailable`
  - `Exception` (catch-all)  `500 Internal Server Error`
- Error response format: `{ "error": "message", "code": "ERROR_CODE", "timestamp": "..." }`
- Validation errors (`@Valid`)  `400` with field-level details
- No stack traces in production responses (use `spring.mvc.log-request-details=false`)

**Done when:** All error paths return the standard JSON error format; no raw stack traces exposed.

**Technical Notes:**
- Can be developed in parallel  no dependency on US-004/006/007
- `NonCatQuestionException` and `VectorStoreException` should be custom exceptions in the `exception/` package
- Good first story to tackle early in the sprint

---

##  Dependencies Between Stories

```
US-004 (Qdrant setup  collection + embeddings working)  US-006 (RAG needs Qdrant with data)
US-004 (Qdrant  collection created)   US-005 VectorStore.add() call only (partial dependency)
US-005 (Data ingestion)                US-006 (RAG needs questions in Qdrant)
US-005 scaffolding (CSV, mapper, hash)  Can start immediately, parallel with US-004
US-012 (exception handling)             Independent  start any time
US-007 (guardrail)                      Needs Redis running (US-002 done )
```

**Recommended order:**
1. **Day 12:** vagrover works US-004 (Qdrant wiring + collection setup); prateekarora7 works US-012 (independent) AND starts US-005 scaffolding (CSV reader, `Document` mapper, dedup hash, sample CSV  no Qdrant needed yet)
2. **Day 3 (join point):** Once vagrover confirms collection is up + embeddings work  prateekarora7 plugs `VectorStore.add()` into US-005 and runs end-to-end ingestion
3. **Day 37:** vagrover starts US-007 (guardrail  uses existing ChatClient + Redis); prateekarora7 finishes US-005
4. **Day 610:** vagrover starts US-006 (depends on US-004 done + US-005 has data in Qdrant); wire RAG + guardrail into controller
5. **Day 1114:** Integration testing, curl smoke tests, demo prep

>  **US-004 + US-005 can be worked in parallel with a join point on Day 3.** prateekarora7 should build everything *except* the `VectorStore.add()` call first, then plug it in once vagrover confirms Qdrant + embeddings are green.

---

##  Sprint 2 Definition of Done

- [ ] Qdrant collection `cat-questions` is created on startup and accepts documents
- [ ] CSV ingestion loads CAT questions into Qdrant via `POST /api/v1/cat/ingest`
- [ ] Similarity search returns relevant CAT questions
- [ ] RAG context is injected into LLM prompt via `CatRagService`
- [ ] Guardrail rejects non-CAT questions before they reach the LLM
- [ ] Redis caches guardrail results
- [ ] All endpoints return consistent JSON error responses
- [ ] Unit tests written for `CatRagService` and `CatGuardrailService`
- [ ] `SCRUM_BOARD.md` updated with final status

---

##  Architect Notes (FYI)

- **Active profiles for Sprint 2:**
  - **Default (no profile flag):** Gemini chat + embedding  configured entirely in `application.yaml`. This is the primary dev profile.
  - **`ollama` profile:** `application-ollama.yaml` activates on `--spring.profiles.active=ollama`. Disables Gemini autoconfigure, enables Ollama chat + embedding.
  - **Prod profile:** Out of scope for Sprint 2. Will be addressed when we're ready to deploy.
- **No `application-gemini.yaml` exists anymore**  Gemini config lives in `application.yaml` (always loaded). Do not reference it in code, docs, or config.
- **RAG wiring:** `ChatController`  `CatGuardrailService`  `CatRagService`  `CatChatService`. Keep each service focused.
- **Qdrant collection for Sprint 2:** Use a single collection (`cat-questions`) against the default (Gemini) profile. Prod collection naming to be addressed with prod profile later.
- **Redis for guardrail caching:** Key = hash of the question string. TTL = 1 hour. Prevents repeated LLM calls for the same question.
- **Exception hierarchy:** `CatifyException` (base)  `NonCatQuestionException`, `VectorStoreException`. All extend `RuntimeException`.
