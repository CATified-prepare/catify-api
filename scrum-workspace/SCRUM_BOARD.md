# 📊 Scrum Board — CAT AI Assistant

> **Current Sprint:** Sprint 2  
> **Sprint Goal:** Complete Qdrant integration, build RAG search, guardrail service, and exception handling  
> **Last Updated:** May 25, 2026

---

## 🏃 Sprint 2 Board

| Story  | Title                          | Assignee      | Points | Status         |
|--------|--------------------------------|---------------|--------|----------------|
| US-004 | Qdrant Vector Store Setup      | vagrover      | 5      | 🔨 In Progress |
| US-005 | Data Ingestion — CSV Loader    | prateekarora7 | 8      | 🔨 In Progress |
| US-006 | RAG Search Service             | vagrover      | 5      | 📝 To Do       |
| US-007 | Guardrail Service              | prateekarora7      | 5      | 📝 To Do       |
| US-012 | Global Exception Handling      | prateekarora7 | 3      | 📝 To Do       |

### Sprint Progress: **0 / 26 points** completed

### Notes
- US-004: Carried over from Sprint 1 — collection setup, document add/search, prod embedding config remaining.
- US-005: Data ingestion (CSV → Qdrant) — in progress this sprint.
- US-006: Depends on US-004 + US-005 (Qdrant must have data).
- US-007: Guardrail with keyword fast-path + LLM fallback + Redis caching.
- US-012: Global exception handler — can be done in parallel with other stories.

---

## ✅ Sprint 1 — Closed (May 25, 2026)

| Story  | Title                              | Assignee      | Points | Status   |
|--------|------------------------------------|---------------|--------|----------|
| US-001 | Spring Boot Project Setup              | vagrover      | 3      | ✅ Done  |
| US-002 | Docker Compose Infrastructure          | prateekarora7 | 3      | ✅ Done  |
| US-003 | LLM Integration (Gemini/Claude)        | vagrover      | 5      | ✅ Done  |
| US-017 | Local LLM (Ollama) Profile + Endpoint  | prateekarora7 | 3      | ✅ Done  |
| US-004 | Qdrant Vector Store Setup              | vagrover      | 5      | ➡️ Carried to Sprint 2 |

**Sprint 1 Completed:** 14 / 19 points (US-004 carried over)

---

## Status Legend

| Emoji | Status       |
|-------|--------------|
| 📝    | To Do        |
| 🔨    | In Progress  |
| 🔍    | In Review    |
| ✅    | Done         |
| 🚫    | Blocked      |
| ➡️    | Carried Over |

---

## Upcoming (Sprint 3)

| Story  | Title                         | Priority | Points |
|--------|-------------------------------|----------|--------|
| US-008 | Conversation Memory           | P1       | 5      |
| US-009 | Streaming Responses (SSE)     | P1       | 5      |
| US-010 | API Key Authentication        | P1       | 5      |

---

## Velocity Tracker

| Sprint   | Planned | Completed | Notes                                      |
|----------|---------|-----------|--------------------------------------------|
| Sprint 1 | 19      | 14        | US-004 carried to Sprint 2; US-017 (Ollama) done |
| Sprint 2 | 26      | 0         | In progress                                |
| Sprint 3 | —       | —         | Not started                                |
| Sprint 4 | —       | —         | Not started                                |

