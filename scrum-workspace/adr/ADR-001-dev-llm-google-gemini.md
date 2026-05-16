# ADR-001: Use Google Gemini as Dev Environment LLM

**Status:** Accepted  
**Date:** 2026-05-16  
**Decider:** Architect  

## Context

The team needs a free LLM for development to avoid API costs during the build phase. Production will use Claude API (Anthropic).

## Decision

Use **Google Gemini 2.0 Flash** (free tier) for chat and **Google `text-embedding-004`** for embeddings in the dev profile.

## Reasoning

- **1M tokens/day free tier** — more than enough for development and testing
- **Official Spring AI starter** available (`spring-ai-vertex-ai-gemini-spring-boot-starter`)
- **No local hardware requirements** — unlike Ollama, runs in the cloud
- **Spring Profiles** make swapping to Claude in prod a config-only change — no code changes needed
- **Quality is close to Claude/GPT-4** — Gemini 2.0 Flash is very capable for dev/testing purposes

## Implementation

### Dev Profile (`application-dev.yml`)

```yaml
spring:
  ai:
    vertex:
      ai:
        gemini:
          api-key: ${GEMINI_API_KEY}
          chat:
            options:
              model: gemini-2.0-flash
              temperature: 0.3
          embedding:
            options:
              model: text-embedding-004
```

### Prod Profile (`application-prod.yml`)

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-20250514
          temperature: 0.3
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small
```

### Dependencies (`pom.xml`)

```xml
<!-- Dev: Google Gemini -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vertex-ai-gemini-spring-boot-starter</artifactId>
</dependency>

<!-- Prod: Anthropic Claude -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- Prod: OpenAI Embeddings -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

## Consequences

- **Embedding dimension mismatch:** Google `text-embedding-004` = 768 dims vs OpenAI `text-embedding-3-small` = 1536 dims. Dev and prod Qdrant collections must be separate (this is expected — dev and prod data are separate anyway).
- **Quality gap:** Gemini Flash is slightly lower quality than Claude for nuanced CAT exam answers — acceptable for development and testing.
- **API key required:** Team needs a Google account + free API key from [aistudio.google.com](https://aistudio.google.com).

## Alternatives Considered

| Option | Rejected Because |
|--------|-----------------|
| Ollama (local) | Requires decent hardware (8GB+ RAM), slower on laptops |
| Groq | No official Spring AI starter, relies on OpenAI compatibility layer |
| OpenAI free tier | No meaningful free tier exists |
| Claude free tier | No free tier for API usage |

## Related

- Affects: US-003 (LLM Integration), US-004 (Qdrant Vector Store Setup)
- See also: ADR-002 (PostgreSQL as relational DB)

