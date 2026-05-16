# CAT AI Assistant — Java Spring Boot (Production-Grade)

> Production-ready architecture for a domain-specific CAT India exam AI assistant.
> Uses RAG with Qdrant vector database, Spring AI, and Claude API.

---

## 📌 Project Goal

Build a **production-grade, scalable** AI assistant in Java Spring Boot that:

- Answers only CAT India exam related questions
- Is fed with CAT past year questions as its knowledge base
- Politely rejects any non-CAT questions
- Handles concurrent users with rate limiting, caching, and observability
- Is deployable to cloud with zero migration pain

---

## 🧠 Core Concept — RAG (Retrieval-Augmented Generation)

You do **NOT** train a new model from scratch. Instead:

1. Feed CAT questions into a vector database (Qdrant)
2. When user asks something → search for relevant CAT content
3. Pass that context to an LLM with strict instructions: "Only answer CAT-related questions"
4. LLM answers using only your CAT data

---

## 🏗️ Architecture Overview

```
User Question
      ↓
Spring Boot API  (/api/v1/cat/ask)
      ↓
① Auth + Rate Limiting (Security Filter)
      ↓
② Is it CAT-related? (Guardrail — keyword check → LLM fallback)
      ↓
③ Search CAT question bank (Qdrant — with metadata filters)
      ↓
④ Send: question + relevant CAT context + chat history → LLM
      ↓
⑤ Stream answer back (SSE) grounded in your CAT data
      ↓
⑥ Log metrics (latency, tokens, cost) → Observability
```

---

## 🧰 Tech Stack

| Layer              | Technology                                  |
|--------------------|---------------------------------------------|
| Backend Framework  | Java 21 + Spring Boot 3.x                   |
| AI Orchestration   | Spring AI (official Spring library)          |
| LLM                | Claude API (Anthropic)                       |
| Vector Database    | **Qdrant** (purpose-built, scalable)         |
| Embeddings         | OpenAI `text-embedding-3-small` or Ollama    |
| Relational DB      | PostgreSQL (users, chat history, audit logs) |
| Caching            | Redis (embedding cache, guardrail cache)     |
| Rate Limiting      | Bucket4j or Resilience4j                     |
| Observability      | Actuator + Micrometer + OpenTelemetry        |
| Containerization   | Docker + Docker Compose                      |
| Deployment         | Kubernetes (or Docker Compose for dev)       |

---

## ❓ Why Qdrant Over pgvector?

| Aspect              | pgvector                              | Qdrant                                        |
|---------------------|---------------------------------------|-----------------------------------------------|
| **Purpose**         | Plugin bolted onto PostgreSQL         | Purpose-built vector search engine            |
| **Scaling**         | Limited by PostgreSQL's architecture  | Native sharding + replication                 |
| **Performance**     | Degrades past ~1M vectors             | Handles millions with consistent latency      |
| **Filtering**       | SQL WHERE (post-filter, slow)         | Native payload filtering (pre-filter, fast)   |
| **HNSW Tuning**     | Basic                                 | Full control: `m`, `ef_construct`, `ef`       |
| **Dashboard**       | None                                  | Built-in UI at `:6333/dashboard`              |
| **Quantization**    | Not available                         | Scalar + Product quantization for cost saving |
| **Cloud Offering**  | Self-managed only                     | Qdrant Cloud (managed, free tier available)   |

**Bottom line:** pgvector is fine for prototypes. Qdrant avoids a painful migration later.

---

## 📁 Project Structure (Production-Grade)

```
cat-ai-assistant/
├── src/main/java/com/catai/assistant/
│   ├── CatAiApplication.java
│   │
│   ├── config/
│   │   ├── QdrantConfig.java               ← Vector store configuration
│   │   ├── SecurityConfig.java             ← API key / JWT auth
│   │   ├── RateLimitConfig.java            ← Per-user request throttling
│   │   ├── CacheConfig.java                ← Redis caching setup
│   │   └── OpenTelemetryConfig.java        ← Distributed tracing
│   │
│   ├── controller/
│   │   └── ChatController.java             ← REST + SSE streaming endpoints
│   │
│   ├── dto/
│   │   ├── ChatRequest.java                ← Request body (question, sessionId, filters)
│   │   ├── ChatResponse.java               ← Response body (answer, sources, tokensUsed)
│   │   └── IngestionRequest.java           ← Bulk ingestion request
│   │
│   ├── service/
│   │   ├── CatGuardrailService.java        ← Keyword check → LLM fallback
│   │   ├── CatRagService.java              ← Qdrant search with metadata filters
│   │   ├── CatChatService.java             ← LLM call with chat memory + streaming
│   │   └── TokenTrackingService.java       ← Track API usage and costs
│   │
│   ├── ingestion/
│   │   ├── CatDataIngestionService.java    ← Batch ingestion with dedup
│   │   ├── PdfParserService.java           ← Parse CAT PDFs (Apache Tika)
│   │   └── ChunkingService.java            ← Text splitting with overlap
│   │
│   ├── model/
│   │   ├── CatQuestion.java                ← JPA entity (PostgreSQL)
│   │   └── ChatSession.java                ← Chat history entity
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java     ← @ControllerAdvice
│   │   ├── NonCatQuestionException.java    ← Custom exception
│   │   └── VectorStoreException.java       ← Qdrant errors
│   │
│   └── security/
│       └── ApiKeyAuthFilter.java           ← API key validation filter
│
├── src/main/resources/
│   ├── application.yml                      ← Default config
│   ├── application-dev.yml                  ← Dev profile
│   ├── application-prod.yml                 ← Prod profile
│   └── logback-spring.xml                   ← Structured JSON logging
│
├── src/test/java/
│   ├── service/
│   │   ├── CatGuardrailServiceTest.java
│   │   └── CatRagServiceTest.java
│   └── integration/
│       └── ChatIntegrationTest.java         ← Testcontainers (Qdrant + PG)
│
├── docker-compose.yml
├── Dockerfile
├── k8s/                                     ← Kubernetes manifests
│   ├── deployment.yml
│   ├── service.yml
│   ├── configmap.yml
│   └── hpa.yml                              ← Horizontal Pod Autoscaler
└── pom.xml
```

---

## 🔄 How LLM API is Called

### What You Send (Request)

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1000,
  "system": "You are a CAT exam assistant. Only answer CAT-related questions.",
  "messages": [
    { "role": "user", "content": "What is the syllabus for CAT 2024?" }
  ]
}
```

### What You Get Back (Response)

```json
{
  "content": [
    {
      "type": "text",
      "text": "CAT 2024 syllabus covers three sections: VARC, DILR, and Quantitative Aptitude..."
    }
  ]
}
```

---

## 🛠️ Infrastructure Setup

### Docker Compose (Full Local Stack)

```yaml
# docker-compose.yml
services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: cat-qdrant
    ports:
      - "6333:6333"   # REST API + Dashboard
      - "6334:6334"   # gRPC (used by Spring AI)
    volumes:
      - qdrant-data:/qdrant/storage

  postgres:
    image: postgres:16
    container_name: cat-postgres
    environment:
      POSTGRES_DB: catdb
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - pg-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: cat-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  qdrant-data:
  pg-data:
  redis-data:
```

```bash
docker-compose up -d
```

> **Qdrant Dashboard:** Open `http://localhost:6333/dashboard` to explore your vectors visually.

---

### application.yml (Full Production Config)

```yaml
spring:
  profiles:
    active: dev

  # --- Relational DB (users, chat history, audit) ---
  datasource:
    url: jdbc:postgresql://localhost:5432/catdb
    username: admin
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate       # use Flyway/Liquibase in prod
    show-sql: false

  # --- Qdrant Vector Store ---
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334               # gRPC port
        collection-name: cat-questions
        initialize-schema: true
    # --- Claude LLM ---
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}    # NEVER hardcode
      chat:
        options:
          model: claude-sonnet-4-20250514
          max-tokens: 1024
          temperature: 0.3       # Lower = more factual
    # --- Embedding Model ---
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small  # 1536 dims, cheaper

  # --- Redis Cache ---
  data:
    redis:
      host: localhost
      port: 6379

  # --- Actuator ---
  management:
    endpoints:
      web:
        exposure:
          include: health,metrics,info,prometheus
    health:
      qdrant:
        enabled: true
```

---

### pom.xml (All Dependencies)

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring AI — Qdrant Vector Store -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-qdrant-store-spring-boot-starter</artifactId>
</dependency>

<!-- Spring AI — Anthropic (Claude) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- Spring AI — OpenAI (Embeddings only) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- PDF Parsing -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.1</version>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>

<!-- Observability -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>qdrant</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

---

## ☕ Key Service Classes

### DTOs

```java
// ChatRequest.java
public record ChatRequest(
    @NotBlank String question,
    String sessionId,           // for conversation memory
    Integer year,               // optional filter: 2018, 2019...
    String topic                // optional filter: VARC, DILR, Quant
) {}

// ChatResponse.java
public record ChatResponse(
    String answer,
    List<String> sources,       // relevant questions used as context
    int tokensUsed
) {}
```

### ChatController.java (with versioning + streaming)

```java
@RestController
@RequestMapping("/api/v1/cat")
public class ChatController {

    private final CatChatService chatService;

    public ChatController(CatChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.ask(request));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody ChatRequest request) {
        return chatService.askStream(request);
    }
}
```

### CatChatService.java (with memory + streaming)

```java
@Service
public class CatChatService {

    private final ChatClient chatClient;
    private final CatRagService ragService;
    private final CatGuardrailService guardrailService;

    public CatChatService(ChatClient.Builder builder,
                          CatRagService ragService,
                          CatGuardrailService guardrailService,
                          ChatMemory chatMemory) {
        this.ragService = ragService;
        this.guardrailService = guardrailService;
        this.chatClient = builder
            .defaultSystem("""
                You are a CAT India exam assistant.
                ONLY answer CAT exam related questions using the provided context.
                If the context doesn't contain relevant information, say so.
                If the question is not related to CAT, politely decline.
                Always cite which year/topic the information comes from.
                """)
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
    }

    public ChatResponse ask(ChatRequest request) {
        if (!guardrailService.isCatRelated(request.question())) {
            return new ChatResponse(
                "I can only help with CAT India exam questions. Please ask something related to CAT.",
                List.of(), 0
            );
        }

        String context = ragService.findRelevantContext(
            request.question(), request.year(), request.topic()
        );

        String enrichedPrompt = """
            Context from CAT question bank:
            %s

            User question: %s
            """.formatted(context, request.question());

        String answer = chatClient.prompt()
            .user(enrichedPrompt)
            .advisors(a -> a.param("chat_memory_conversation_id", request.sessionId()))
            .call()
            .content();

        return new ChatResponse(answer, List.of(context), 0);
    }

    public Flux<String> askStream(ChatRequest request) {
        // Same logic but with .stream().content()
        String context = ragService.findRelevantContext(
            request.question(), request.year(), request.topic()
        );

        return chatClient.prompt()
            .user(context + "\n\nQuestion: " + request.question())
            .stream()
            .content();
    }
}
```

### CatRagService.java (with Qdrant metadata filters)

```java
@Service
public class CatRagService {

    private final VectorStore vectorStore;

    public CatRagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String findRelevantContext(String userQuestion, Integer year, String topic) {
        var searchBuilder = SearchRequest
            .query(userQuestion)
            .withTopK(5)
            .withSimilarityThreshold(0.7);

        // Qdrant payload filtering — scoped search
        if (year != null || topic != null) {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            Expression filter = null;
            if (year != null && topic != null) {
                filter = b.and(b.eq("year", year), b.eq("topic", topic)).build();
            } else if (year != null) {
                filter = b.eq("year", year).build();
            } else {
                filter = b.eq("topic", topic).build();
            }
            searchBuilder = searchBuilder.withFilterExpression(filter);
        }

        List<Document> results = vectorStore.similaritySearch(searchBuilder);

        return results.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n---\n"));
    }
}
```

### CatGuardrailService.java (cost-optimized: keyword first, LLM fallback)

```java
@Service
public class CatGuardrailService {

    private final ChatClient chatClient;

    private static final Set<String> CAT_KEYWORDS = Set.of(
        "cat", "iim", "varc", "dilr", "quant", "quantitative",
        "verbal", "reasoning", "data interpretation", "mba",
        "percentile", "slot", "cat 2024", "cat 2023", "cat exam",
        "reading comprehension", "para jumble", "syllogism"
    );

    public CatGuardrailService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Cacheable(value = "guardrail", key = "#question.hashCode()")
    public boolean isCatRelated(String question) {
        // Fast keyword check — avoids LLM call for obvious cases
        String lower = question.toLowerCase();
        if (CAT_KEYWORDS.stream().anyMatch(lower::contains)) {
            return true;
        }

        // LLM fallback for ambiguous questions
        try {
            String response = chatClient.prompt()
                .user("""
                    Is this question related to the CAT India MBA entrance exam?
                    Question: %s
                    Reply only YES or NO.
                    """.formatted(question))
                .call()
                .content();
            return response.trim().toUpperCase().startsWith("YES");
        } catch (Exception e) {
            return false; // fail-safe: reject if LLM is down
        }
    }
}
```

### CatDataIngestionService.java (batch + dedup + chunking)

```java
@Service
public class CatDataIngestionService {

    private final VectorStore vectorStore;

    public CatDataIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Batch ingest questions with metadata stored as Qdrant payload.
     * Qdrant will auto-filter on these fields during search.
     */
    public void ingestBatch(List<CatQuestion> questions) {
        List<Document> docs = questions.stream()
            .map(q -> new Document(
                q.getQuestionText() + "\nAnswer: " + q.getAnswer()
                    + "\nExplanation: " + q.getExplanation(),
                Map.of(
                    "year", q.getYear(),
                    "topic", q.getTopic(),
                    "difficulty", q.getDifficulty(),
                    "hash", q.contentHash()   // for dedup
                )
            ))
            .toList();

        // Batch in chunks of 50
        List<List<Document>> batches = partition(docs, 50);
        for (List<Document> batch : batches) {
            vectorStore.add(batch);
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
```

### GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NonCatQuestionException.class)
    public ResponseEntity<Map<String, String>> handleNonCat(NonCatQuestionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Something went wrong. Please try again."));
    }
}
```

---

## 📊 Database Schema

### PostgreSQL (Relational Data Only)

```sql
-- Chat history for conversation memory
CREATE TABLE chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID REFERENCES chat_sessions(id),
    role        VARCHAR(20) NOT NULL,   -- 'user' or 'assistant'
    content     TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- API usage tracking
CREATE TABLE api_usage (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(100),
    tokens_in   INTEGER,
    tokens_out  INTEGER,
    cost_usd    DECIMAL(10,6),
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### Qdrant (Vector Data)

Qdrant stores vectors + payload automatically. No SQL schema needed.

```
Collection: cat-questions
├── Vector: 1536 dimensions (text-embedding-3-small)
└── Payload fields (auto-indexed, filterable):
    ├── year: integer        → filter by exam year
    ├── topic: keyword       → filter by VARC/DILR/Quant
    ├── difficulty: keyword  → filter by Easy/Medium/Hard
    ├── answer: text
    └── hash: keyword        → deduplication
```

---

## 🔄 Data Flow — Two Phases

### Phase 1 — Ingestion (One Time + Incremental)

```
CAT PDFs / CSVs
      ↓
Apache Tika parses PDFs → raw text
      ↓
ChunkingService splits into overlapping chunks (512 tokens, 50 overlap)
      ↓
Dedup check (content hash)
      ↓
Embedding API → vector [0.23, 0.87...]
      ↓
Batch insert into Qdrant (50 docs/batch) with payload metadata
```

### Phase 2 — User Query

```
User: "Solve this permutation problem from CAT 2023"
      ↓
API Key auth → Rate limit check
      ↓
Guardrail: keyword match ✅ ("cat", "permutation")
      ↓
Embed question → vector [0.11, 0.76...]
      ↓
Qdrant search: top 5 similar + filter(year=2023, topic=Quant)
      ↓
Load chat history from memory (sessionId)
      ↓
Send: context + history + question → Claude
      ↓
Stream answer back via SSE
      ↓
Log: latency, tokens, cost → Prometheus
```

---

## 🗂️ CAT Data Files

```
cat-data/
├── quant/
│   ├── cat_2018_quant.csv
│   ├── cat_2019_quant.csv
├── varc/
│   ├── cat_2018_varc.csv
└── dilr/
    └── cat_2018_dilr.csv
```

**CSV format:**

```csv
question_text,answer,explanation,topic,difficulty
"If x + y = 10...","C","Step 1: ...","Algebra","Medium"
```

---

## 🚀 Step-by-Step Build Plan

| Step | What to do                                                    |
|------|---------------------------------------------------------------|
| 1    | Set up Spring Boot project + all dependencies (pom.xml)       |
| 2    | Docker Compose: Qdrant + PostgreSQL + Redis                   |
| 3    | Integrate Claude API via Spring AI (basic chat)               |
| 4    | Add Qdrant vector store + embedding model                     |
| 5    | Build data ingestion (CSV parser + batch + dedup)             |
| 6    | Build RAG search with Qdrant metadata filters                 |
| 7    | Build guardrail (keyword + LLM fallback + caching)            |
| 8    | Add conversation memory (MessageChatMemoryAdvisor)            |
| 9    | Add streaming (SSE endpoint)                                  |
| 10   | Add security (API key auth)                                   |
| 11   | Add rate limiting (Bucket4j)                                  |
| 12   | Add observability (Actuator + Prometheus metrics)             |
| 13   | Add exception handling (@ControllerAdvice)                    |
| 14   | Write tests (unit + integration with Testcontainers)          |
| 15   | Dockerize app (multi-stage Dockerfile)                        |
| 16   | Kubernetes manifests + HPA                                    |

---

## 🌍 Deployment Environments

| Environment | Vector DB             | Relational DB         | Config              |
|-------------|-----------------------|-----------------------|---------------------|
| Local Dev   | Docker Qdrant         | Docker PostgreSQL     | `application-dev.yml` |
| Testing     | Testcontainers Qdrant | Testcontainers PG     | `application-test.yml` |
| Production  | Qdrant Cloud (managed)| AWS RDS / Supabase    | `application-prod.yml` |

---

## 💡 Production Tips

1. **Never hardcode API keys** — use `${ENV_VAR}` in YAML, or Spring Cloud Vault
2. **Guardrail cost optimization** — keyword check first avoids LLM calls for 80%+ of requests
3. **Cache guardrail results** — same question = same answer, save LLM cost with `@Cacheable`
4. **Embedding model choice** — `text-embedding-3-small` (1536 dims) is cost-effective; or run `all-MiniLM-L6-v2` via Ollama for zero API cost (384 dims)
5. **Qdrant tuning** — for <50K questions: `m=16`, `ef_construct=100` is optimal; enable quantization only past 500K vectors
6. **Use Spring Profiles** — separate configs for `dev`, `test`, `prod`
7. **Database migrations** — use Flyway or Liquibase instead of `ddl-auto: update` in production

---

## 📌 Key Takeaways

- You **don't** train a new model — you use RAG + an existing LLM
- **Qdrant** = purpose-built vector DB that scales without migration pain
- **PostgreSQL** = relational data only (chat history, users, audit)
- **Redis** = caching layer for embeddings and guardrail results
- The system prompt + guardrail enforce CAT-only behavior
- Spring AI makes LLM + vector store integration seamless in Java
- Production-ready = auth + rate limiting + caching + observability + error handling

---

> **Continue this conversation with Claude by sharing this file and saying:**
>
> *"Here are my notes from a previous chat. I'm building a production-grade CAT AI assistant in Java Spring Boot with Qdrant. Let's continue from here."*
