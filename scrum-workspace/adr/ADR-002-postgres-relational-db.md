# ADR-002: PostgreSQL as Relational Database

**Status:** Accepted  
**Date:** 2026-05-16  
**Decider:** Architect  

## Context

The project needs a relational database for structured data: chat sessions, chat messages, API usage tracking, and user management. The vector search is handled by Qdrant (separate concern).

## Decision

Use **PostgreSQL 16** as the relational database for all structured/transactional data.

## Reasoning

- **Clear separation of concerns** — Qdrant handles vector search, PostgreSQL handles relational data. Each tool does what it's best at.
- **Spring Boot first-class support** — Spring Data JPA + PostgreSQL driver is the most battle-tested combination. HikariCP connection pooling, Flyway migrations, and Testcontainers all work out of the box.
- **Simple relational schema** — The data model (chat_sessions, chat_messages, api_usage) is inherently relational with foreign keys and transactions.
- **Production path is smooth** — Docker locally → AWS RDS / Supabase / Cloud SQL in production with zero code changes.
- **Team familiarity** — SQL is a known skill; no learning curve.

## Schema

```sql
CREATE TABLE chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  UUID REFERENCES chat_sessions(id),
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE api_usage (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(100),
    tokens_in   INTEGER,
    tokens_out  INTEGER,
    cost_usd    DECIMAL(10,6),
    created_at  TIMESTAMP DEFAULT NOW()
);
```

## Consequences

- Must use **Flyway or Liquibase** for schema migrations in production (not `ddl-auto: update`)
- Connection pooling via HikariCP (Spring Boot default) — no additional setup needed

## Alternatives Considered

| Option | Rejected Because |
|--------|-----------------|
| MongoDB | Data is relational; loses JPA/transaction support for no benefit |
| pgvector (for vectors) | Qdrant is purpose-built and scales better; pgvector only relevant if consolidating DBs |
| SQLite | Not suitable for production or concurrent access |

## Related

- Affects: US-008 (Conversation Memory), US-002 (Docker Compose)
- See also: ADR-001 (Dev LLM choice)

