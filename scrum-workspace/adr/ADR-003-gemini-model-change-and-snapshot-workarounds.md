# ADR-003: Gemini Model Change & Spring AI Configuration Learnings

**Status:** Accepted  
**Date:** 2026-05-21  
**Decider:** Architect  

---

## Context

During Sprint 1, while implementing US-003 (LLM Integration) and US-004 (Qdrant Setup), several issues were discovered with the original plan (ADR-001) due to:
1. Google changing free tier quotas — `gemini-2.0-flash` now has 0 RPM/RPD on free tier
2. Spring AI 2.0.0-SNAPSHOT embedding configuration requiring a separate `api-key` per component
3. Running the app outside Docker while Docker containers are active

---

## Decisions Made

### 1. Model Changed: `gemini-2.0-flash` → `gemini-3.1-flash-lite`

**Reason:** Google revoked free tier access to `gemini-2.0-flash` (RPM = 0, RPD = 0). 

**New model:** `gemini-3.1-flash-lite`  
- 15 RPM (requests per minute)
- 250K TPM (tokens per minute)  
- 500 RPD (requests per day)
- Best free tier quota for development

**Alternatives available on free tier:**

| Model | RPM | RPD | Notes |
|-------|-----|-----|-------|
| gemini-2.5-flash | 5 | 20 | Low RPD |
| gemini-2.5-flash-lite | 10 | 20 | Low RPD |
| **gemini-3.1-flash-lite** | **15** | **500** | ✅ Best for dev |
| gemma-4-26b | 15 | 1500 | Open model, less capable |

**Embedding models available:**
- `gemini-embedding-002` — 100 RPM, 30K TPM, 1K RPD

---

### 2. Spring AI Dependency: `spring-ai-starter-model-google-genai`

**Old (ADR-001):** `spring-ai-vertex-ai-gemini-spring-boot-starter`  
**New:** `spring-ai-starter-model-google-genai`

**Reason:** Spring AI 2.0 renamed all artifact IDs. The old name doesn't exist in 2.0.0-SNAPSHOT.

**Why SNAPSHOT?** Spring Boot 4.0.6 requires Spring AI 2.x. There is no stable release of Spring AI 2.0 yet — only `2.0.0-SNAPSHOT` from `repo.spring.io/snapshot`.

---

### 3. Embedding Configuration — Two Separate `api-key` Entries Required

**Key learning:** In Spring AI 2.0.0-SNAPSHOT, the chat and embedding components use **separate configuration property classes with separate prefixes**. Setting only the top-level `api-key` is NOT sufficient.

| Component | Property class | Config prefix |
|-----------|---------------|---------------|
| Chat | `GoogleGenAiConnectionProperties` | `spring.ai.google.genai` |
| Embedding | `GoogleGenAiEmbeddingConnectionProperties` | `spring.ai.google.genai.embedding` |

**Required config in `application-gemini.yaml`:**
```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY}           # chat autoconfig reads this
        chat:
          model: gemini-3.1-flash-lite
          temperature: 0.3
        embedding:
          api-key: ${GEMINI_API_KEY}         # embedding autoconfig reads THIS separately
          model: gemini-embedding-002
```

**Investigation history:** We initially thought this was a library bug requiring autoconfigure excludes. After reading the source code of `GoogleGenAiEmbeddingConnectionAutoConfiguration`, we identified the root cause: the `if (apiKey present)` check reads from `GoogleGenAiEmbeddingConnectionProperties.getApiKey()` — a completely separate bean from chat. Once `embedding.api-key` was set, all excludes were removed and everything worked.

**No autoconfigure excludes are needed.** The excludes added during investigation have been removed.

---

### 4. Configuration Architecture

```
application.yaml          → shared config, localhost defaults for all services
application-gemini.yaml   → Gemini chat + embedding config (dev profile)
application-prod.yaml     → Claude + OpenAI embeddings (production profile)
.env                      → secrets only (gitignored)
```

**`application.yaml` service host defaults (changed to `localhost`):**
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/catify}
data:
  redis:
    host: ${REDIS_HOST:localhost}
ai:
  vectorstore:
    qdrant:
      host: ${QDRANT_HOST:localhost}
```

**Why `localhost` defaults?** When running from IntelliJ, Docker-mapped ports are accessible at `localhost`. When running inside Docker Compose, the `docker-compose.yml` injects `QDRANT_HOST=qdrant`, `REDIS_HOST=redis` etc. as env vars which override the defaults. Both scenarios work correctly.

**`.env` contents (dev machine — secrets only):**
```dotenv
GEMINI_API_KEY=<your-key-from-aistudio.google.com>
```

---

### 5. API Key Source

API key obtained from [aistudio.google.com](https://aistudio.google.com) → Google AI Studio Developer API (free).

This is **NOT Vertex AI**. The two products differ:
- **Google AI Studio (Developer API)** — free, API key auth, limited quotas
- **Vertex AI** — paid, requires Google Cloud project + service account, enterprise quotas

Our `spring.ai.google.genai.api-key` uses Developer API mode.

---

## Known Technical Debt

| ID | Description | Impact | Resolution |
|----|-------------|--------|-----------|
| TD-001 | Spring AI 2.0.0-SNAPSHOT — no stable release yet | Possible breaking changes between SNAPSHOTs | Upgrade to 2.0.0 GA when released |
| TD-002 | Free tier quotas can change without notice | Google can revoke models anytime | Keep fallback models list updated |

---

## Future Risks

1. **Google may further reduce free tier** — monitor `ai.dev/rate-limit` monthly
2. **Spring AI 2.0.0 GA may change config prefixes** — property namespaces could change between SNAPSHOT and GA
3. **Embedding dimension mismatch** — `gemini-embedding-002` dims may differ from prod's `text-embedding-3-small` (1536 dims). Dev and prod Qdrant collections must remain separate.
4. **Spring Boot 4.x is bleeding edge** — if forced to downgrade to Spring Boot 3.x, Spring AI 1.x is stable and fully documented

---

## How to Run the Project (Quick Reference)

### Prerequisites
- Java 21 (Amazon Corretto)
- Docker Desktop running
- IntelliJ IDEA

### Steps

1. Start Docker infra containers:
   ```bash
   docker-compose up -d postgres qdrant redis pgadmin
   ```

2. Ensure `.env` file exists at project root with:
   ```dotenv
   GEMINI_API_KEY=<your-key-from-aistudio.google.com>
   ```

3. Run from IntelliJ:
   - Active profiles: `gemini`
   - Or use: `--spring.profiles.active=gemini`

4. Test chat:
   ```bash
   curl -X POST http://localhost:8080/api/v1/cat/ask \
     -H "Content-Type: application/json" \
     -d '{"question": "What is the CAT exam syllabus?"}'
   ```

---

## Related

- Supersedes parts of: ADR-001 (Gemini model name, artifact ID, free tier assumptions)
- Affects: US-003 ✅, US-004 🔨 In Progress
- Watch: [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) for 2.0.0 GA
