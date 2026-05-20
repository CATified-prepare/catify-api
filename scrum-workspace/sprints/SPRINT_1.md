# 🏃 Sprint 1 — Project Foundation + AI Integration

> **Sprint Duration:** 2 weeks  
> **Sprint Goal:** Get a working Spring Boot app that can talk to Claude and connect to Qdrant  
> **Total Points:** 19  
> **Team Capacity:** 2 devs × 50 hrs = 100 hrs

---

## 📅 Sprint Dates

- **Start:** [Set your start date]
- **End:** [Start + 14 days]
- **Demo:** End of sprint
- **Retro:** End of sprint

---

## 🎯 Sprint Goal

> *By the end of Sprint 1, we should be able to send a question to our API, have it forwarded to Claude, and get an answer back. Qdrant should be running and accepting documents.*

---

## 📋 Sprint Stories

### US-001: Spring Boot Project Setup [infra] — 3 pts
**Assignee:** vagrover  
**Status:** Done ✅

Create the project skeleton:
- Initialize Spring Boot 3.x + Java 21 project
- Add all dependencies to `pom.xml` (see `projectStart.md`)
- Create package structure: `config/`, `controller/`, `dto/`, `service/`, `ingestion/`, `model/`, `exception/`, `security/`
- Create `application.yml`, `application-dev.yml`, `application-prod.yml`
- Verify it compiles and starts

**Done when:** `mvn spring-boot:run` starts without errors (even if no services work yet)

---

### US-002: Docker Compose Infrastructure [infra] — 3 pts
**Assignee:** prateekarora7  
**Status:** Done

Set up local infrastructure:
- Create `docker-compose.yml` with Qdrant, PostgreSQL, Redis
- Create `.env.example` with placeholder values
- Verify all 3 services start: `docker-compose up -d`
- Verify Qdrant dashboard at `localhost:6333/dashboard`
- Verify PostgreSQL connection via `psql` or DBeaver

**Done when:** All 3 containers running, Qdrant dashboard accessible

---

### US-003: LLM Integration (Gemini Dev / Claude Prod) [ai] — 5 pts
**Assignee:** vagrover  
**Status:** In Progress 🔨

Wire up LLM (profile-based):
- Create `CatChatService` with Spring AI `ChatClient`
- Set system prompt for CAT-only behavior
- Configure Gemini in `application-dev.yml` (`gemini-2.0-flash`, free tier)
- Configure Claude in `application-prod.yml` (paid, for production)
- Add both `spring-ai-vertex-ai-gemini-spring-boot-starter` and `spring-ai-anthropic-spring-boot-starter` to `pom.xml`
- Create `ChatController` with `POST /api/v1/cat/ask`
- Create `ChatRequest` and `ChatResponse` DTOs
- Test with curl using dev profile (Gemini, free)

**Done when:** `curl -X POST localhost:8080/api/v1/cat/ask -d '{"question":"What is CAT exam?"}' -H 'Content-Type: application/json'` returns an LLM-generated answer using Gemini (dev profile)

**Technical Notes:**
- See ADR-001 for decision rationale
- Get free API key from aistudio.google.com

---

### US-004: Qdrant Vector Store Setup [ai] — 5 pts
**Assignee:** prateekarora7  
**Status:** To Do

Connect to Qdrant:
- Configure `spring-ai-qdrant-store-spring-boot-starter` in `application.yml`
- Configure OpenAI embedding model (`text-embedding-3-small`) for prod profile
- Configure Google `text-embedding-004` for dev profile (free)
- Write a simple test: add a document with text + metadata, search for it
- Verify the collection appears in Qdrant dashboard

**Done when:** Can programmatically add a document and find it via similarity search

---

### US-005: Local LLM (Ollama) Profile + Endpoint [ai] — 3 pts
**Assignee:** prateekarora7  
**Status:** To Do

Connect a locally hosted Ollama model for local development and expose an endpoint for it:
- Add `application-local.yml/.yaml` for the `local` profile
  - Configure `spring.ai.ollama.base-url`
    - Host/WSL: `http://localhost:11434`
    - Docker Compose: `http://ollama:11434`
  - Configure chat model via env var (e.g. `OLLAMA_CHAT_MODEL=llama3.2`)
- Update `docker-compose.yml` to support local profile execution
  - Ensure API can run with `SPRING_PROFILES_ACTIVE=local`
  - Pass `OLLAMA_BASE_URL` and `OLLAMA_CHAT_MODEL` to the container (when using Docker)
  - (Optional) Add an `ollama` service in Compose for teams not running Ollama in WSL
- Add a basic API endpoint to query the local LLM:
  - `POST /api/v1/local/ask`
  - Request: `{ "question": "..." }`
  - Response: `{ "answer": "..." }`
  - Uses Spring AI `ChatClient` so provider selection stays profile-based
- Add a curl example in the story notes showing a working call with `local` profile enabled

**Done when:** With `SPRING_PROFILES_ACTIVE=local`, calling `POST /api/v1/local/ask` returns an answer from Ollama.

---

## 🤝 Dependencies Between Stories

```
US-001 (project setup) ──→ US-003 (Claude integration)
                       ──→ US-004 (Qdrant setup)
US-002 (Docker)        ──→ US-004 (Qdrant needs Docker running)
US-001 (project setup) ──→ US-005 (Local Ollama profile/endpoint)
US-002 (Docker)        ──→ US-005 (Optional: run Ollama via Docker/Compose)
```

**Recommended order:**
1. vagrover starts US-001 (Day 1–2)
2. prateekarora7 starts US-002 in parallel (Day 1–2)
3. vagrover moves to US-003 (Day 3–7)
4. prateekarora7 moves to US-004 (Day 3–7)
5. Days 8–10: Integration testing, bug fixes, demo prep

---

## ✅ Sprint 1 Definition of Done

- [ ] Spring Boot app starts and serves `/api/v1/cat/ask`
- [ ] Claude answers CAT-related questions via the API
- [ ] Qdrant stores and retrieves documents via Spring AI
- [ ] Docker Compose runs all infrastructure with one command
- [ ] All code pushed to Git repository
- [ ] `SCRUM_BOARD.md` updated with final status

