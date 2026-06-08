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

### US-006: RAG Search Service with Question-Derived Filters [ai]  8 pts *(re-scoped Jun 8)*
**Assignee:** vagrover
**Status:**  In Progress  re-scoped after `ChatRequest` contract change
**Blocker:**  Fully blocked on US-004 (Qdrant collection must exist) AND US-005 (data must be ingested  can't test similarity search on an empty collection).

#### What this story does (plain English)

Right now, when a user asks Catify a question, the LLM answers from generic knowledge  it has no idea what's actually in our CAT question bank. **This story makes the LLM answer from *real* CAT data** by looking up relevant past-year questions in Qdrant and feeding them into the prompt as context. That's RAG (Retrieval-Augmented Generation).

To do that well, we have to figure out what the user is asking about. Real users type natural sentences, not form fields  so the system has to *read* the question and pull out structured intent before searching.

#### How it works  end to end

> User types: *"give me hard quant questions from CAT 2023"*

1. **Understander** (`CatQueryFilterExtractor`)  reads the sentence, extracts `{year=2023, topic=quant, difficulty=hard}`.
2. **Searcher** (`CatRagService`)  runs a semantic search in Qdrant for that question, *filtered* to only documents tagged with year 2023, topic quant, difficulty hard. Returns up to 5 matching CAT documents.
3. **Chat service** (`CatChatService`)  builds a prompt: *"Here is real CAT context: [those 5 docs]. Question: [user's sentence]. Answer using only this context."* Calls Gemini.
4. **Response**  the user gets an answer grounded in real PYQ data, plus a `sources` list showing which documents were used (citations).

If the user types *"hello"* or *"explain a permutation problem"* and the understander can't extract any filters, the searcher falls back to pure semantic search (no filters). The user still gets a grounded answer  just without the precision boost of structured filtering.

If the understander **fails** (LLM down, weird response), the searcher also falls back to pure semantic. **The chat call must never crash because of an extraction failure.**

#### The two services this story builds

**1. `CatQueryFilterExtractor`  the "understander"**
- Single method: `QueryFilters extract(String question)` returning `(Integer year, String topic, String difficulty)`, all optional.
- Implemented as a small Spring AI `ChatClient` call  built **without** `.defaultSystem(...)` so it doesn't inherit the Catify chat persona and confuse itself.
- Use `BeanOutputConverter<QueryFilters>` so the LLM returns structured JSON we can deserialize directly  no manual parsing.
- Cache results in Redis with `@Cacheable` (key = hash of question, TTL = 24h). The same question always extracts the same filters, so we should pay the LLM once per unique question, not on every retry.
- The extraction prompt locks the vocabulary:
  - `topic`  one of `quant | varc | dilr` (or null)
  - `difficulty`  one of `easy | medium | hard` (or null)
  - `year`  a 4-digit integer or null
- Output values are normalized to lowercase to match the Qdrant payload exactly (mirrors `DataIngestionService.safeLower(...)`).
- Any failure  return `QueryFilters.empty()`. Never throw upward.

**2. `CatRagService`  the "searcher"**
- `retrieve(question, year, topic, difficulty)` accepts the extracted filters.
- Always runs a semantic similarity search: `topK=5`, `threshold=0.7`.
- Uses `FilterExpressionBuilder` to AND-combine whichever filters are non-null into a single `Filter.Expression`. If all are null, no filter expression is attached (pure semantic).
- Returns `RagContext { contextText, sources }`  never null. Use `RagContext.empty()` when nothing matches.
- `sources` are formatted from `Document.getMetadata()` so the API response can cite which documents were used.

**3. Wiring in `CatChatService.ask(...)`**
```
QueryFilters f = extractor.extract(req.question());
RagContext rag = catRagService.retrieve(req.question(), f.year(), f.topic(), f.difficulty());
String prompt = rag.isEmpty() ? req.question() : RAG_TEMPLATE.formatted(rag.contextText(), req.question());
ChatClientResponse r = chatClient.prompt().user(prompt).call().chatClientResponse();
return new ChatResponse(answer, rag.sources(), tokensUsed, ai);
```

#### Acceptance Criteria

- [x] `ChatRequest` no longer carries `year` / `topic` / `difficulty` (done Jun 8)
- [ ] `CatQueryFilterExtractor` extracts `(year, topic, difficulty)` from a natural-language question via a structured-output `ChatClient` call
- [ ] Extraction results cached in Redis (24h TTL)
- [ ] Extraction failure falls back to pure semantic search; chat still succeeds end-to-end
- [ ] `CatRagService` runs similarity search with `topK=5`, `threshold=0.7`; non-null filters are AND-combined via `FilterExpressionBuilder`
- [ ] Empty Qdrant result  empty `RagContext`, never null; chat still succeeds (LLM gets the raw question with no context block)
- [ ] `CatChatService` wires extractor  RAG  prompt augmentation; populates `ChatResponse.sources`
- [ ] Unit tests for `CatRagService` (mocked `VectorStore`) and `CatQueryFilterExtractor` (mocked `ChatClient`)
- [ ] Integration test: real extraction + retrieval against the `cat-questions-test` collection seeded via `DataIngestionService`

**Done when:** A natural-language question like *"give me hard quant questions from CAT 2023"* drives the pipeline end-to-end: extractor returns `{year=2023, topic=quant, difficulty=hard}`, `CatRagService` runs a filtered Qdrant search, the LLM answers grounded in retrieved context, and `ChatResponse.sources` cites which documents were used.

#### Why this story is bigger than the original (5  8 pts)

The old version was a passthrough  the API client supplied year/topic and we forwarded them to Qdrant. With the contract change, we now build a small *understanding* layer (its own LLM call, its own cache, its own failure handling) on top of the search layer. That's a meaningful uplift, hence +3 points.

#### Technical Notes

- `ChatRequest` is now `{ question, sessionId }`  the structured-filter columns were removed because real users type natural language, not key/value pairs.
- Build the extraction `ChatClient` from `ChatClient.Builder` **without** a system prompt  the user prompt fully specifies the task. Reusing `CatChatService`'s ChatClient would inherit the Catify persona and corrupt extraction.
- `BeanOutputConverter<QueryFilters>` is the Spring AI 2.x preferred path for structured output. Avoid hand-rolling JSON parsing.
- Cache TTL of 24h is intentional: extraction is deterministic per question, so the LLM bill drops to ~zero on repeats.
- **Topic vocabulary contract (the #1 risk):** the extractor MUST emit values that match the Qdrant payload vocabulary exactly (`quant | varc | dilr`). If `DataIngestionService` ingests `topic="reading comprehension"` the strict-equality filter will silently miss. Either constrain ingest to the same vocabulary OR run a translation table in the extractor. Decide in implementation; document in an ADR.
- **Co-design with US-007 (guardrail):** both stories run an LLM check on the question. In a Sprint 3 cleanup, consider merging into a single LLM call returning `{ isCatRelated, year?, topic?, difficulty? }`  saves one round-trip per request. Keep them separate for now; tag the optimization for retro.
- Wire `CatRagService` into the existing `CatChatService`; keep RAG, extraction, and chat concerns in separate classes (single responsibility).
- Runs on default profile  no profile-specific config needed.
- The PYQ ingestion side (`DataIngestionController`, `DataIngestionService`, `PyqIngestionRequest`) seeds Qdrant for retrieval testing.

#### Sprint timing (Jun 8  sprint end day)

The full extraction + retrieval scope will not land today. Architect-recommended split:
- **Today (Sprint 2 close-out):** ship `CatRagService` with **pure semantic search** (no filter expression) wired into `CatChatService` + `ChatResponse.sources` populated. Mark this half of US-006 as **Done**.
- **Sprint 3 (carry-over):** implement `CatQueryFilterExtractor` + Redis cache + integration test, and wire it in front of `CatRagService`. Either keep it under US-006 as the carried half, or split into a new **US-018 Query Filter Extraction** in the backlog. PO call.

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
- [ ] Similarity search returns relevant CAT questions (pure semantic  filter extraction carried to Sprint 3)
- [ ] RAG context is injected into LLM prompt via `CatRagService`
- [ ] `ChatResponse.sources` is populated when RAG context is non-empty
- [ ] Guardrail rejects non-CAT questions before they reach the LLM
- [ ] Redis caches guardrail results
- [ ] All endpoints return consistent JSON error responses
- [ ] Unit tests written for `CatRagService` and `CatGuardrailService`
- [ ] `SCRUM_BOARD.md` updated with final status; US-006 carry-over (filter extraction) tracked for Sprint 3

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
- **`ChatRequest` contract simplified (Jun 8):** `{ question, sessionId }` only. Structured filters (`year`, `topic`, `difficulty`) were removed because real users type natural language, not key/value pairs. Filter intent is now derived from the question itself via `CatQueryFilterExtractor` (US-006 carry-over to Sprint 3).
- **Topic vocabulary contract:** the extractor and the ingestion path MUST share the same lowercase vocabulary (`quant | varc | dilr`) for `FilterExpressionBuilder.eq("topic", ...)` to match. Any drift = silent zero-match. Encode in an ADR before merging the extractor.
- **Co-design opportunity (US-007 + US-006 extractor):** both run an LLM check on the question. In Sprint 3, merge into one structured-output call returning `{ isCatRelated, year?, topic?, difficulty? }`. Saves a round-trip per request.
