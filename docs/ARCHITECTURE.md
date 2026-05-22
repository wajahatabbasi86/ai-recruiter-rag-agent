# Architecture

This document describes the system architecture of the AI Recruiter RAG Agent.

---

## High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Client (HTTP)                         │
│             cURL / Postman / React (Phase 3)             │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot Backend  :8080                  │
│                                                          │
│  ┌─────────────────┐    ┌──────────────────────────┐    │
│  │ ResumeController│    │     RagController         │    │
│  │ JobController   │    │  POST /api/rag/query      │    │
│  └────────┬────────┘    └──────────┬───────────────┘    │
│           │                        │                     │
│  ┌────────▼────────┐    ┌──────────▼───────────────┐    │
│  │ CandidateService│    │       RagService          │    │
│  │ JobService      │    │  1. Bias check            │    │
│  │                 │    │  2. Embed query           │    │
│  │  ingest flow:   │    │  3. Search Qdrant         │    │
│  │  parse→chunk    │    │  4. Build context         │    │
│  │  →embed→store   │    │  5. Call Ollama LLM       │    │
│  └──┬──────────────┘    │  6. Sanitize output       │    │
│     │                   │  7. Audit log             │    │
│  ┌──▼──────────────┐    └──────────┬───────────────┘    │
│  │ ResumeParser    │               │                     │
│  │ JobParser       │    ┌──────────▼───────────────┐    │
│  │ (Apache Tika)   │    │   BiasCheckerService      │    │
│  └──┬──────────────┘    │   AuditLogRepository      │    │
│     │                   └──────────────────────────-┘    │
│  ┌──▼──────────────┐                                     │
│  │ EmbeddingService│                                     │
│  │ (Spring AI)     │                                     │
│  └──┬──────────────┘                                     │
└─────┼───────────────────────────────────────────────────┘
      │
┌─────▼────────────────────────────────────────────────────┐
│              Infrastructure (Docker Compose)              │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │    Ollama    │  │    Qdrant    │  │   PostgreSQL   │  │
│  │   :11434     │  │    :6333     │  │     :5432      │  │
│  │              │  │              │  │                │  │
│  │ llama3.1:8b  │  │resume-chunks │  │ candidates     │  │
│  │ nomic-embed  │  │job-descriptions│ │ jobs           │  │
│  │              │  │              │  │ audit_logs     │  │
│  └──────────────┘  └──────────────┘  └────────────────┘  │
└───────────────────────────────────────────────────────────┘
```

---

## RAG Pipeline — Step by Step

### Resume Ingestion Flow

```
POST /api/resumes/upload
         │
         ▼
  ResumeController
         │
         ▼
  CandidateService.ingestResume()
    │
    ├─1─ Apache Tika extracts raw text from PDF/DOCX/TXT
    │
    ├─2─ PostgreSQL: save Candidate profile
    │
    ├─3─ ResumeParserService.chunk()
    │       Split by section headers (SKILLS, EXPERIENCE…)
    │       Fall back to paragraph splitting if no headers
    │
    ├─4─ EmbeddingService.embedAndStoreResume()  [async]
    │       For each chunk:
    │         → POST http://ollama:11434/api/embeddings
    │         → 768-dimensional float[] (nomic-embed-text)
    │         → Upsert to Qdrant with payload {candidateId, section, text}
    │
    └─5─ PostgreSQL: persist comma-separated Qdrant point IDs
              (needed for vector cleanup on candidate delete)
```

### RAG Query Flow

```
POST /api/rag/query  {"query": "...", "topK": 5}
         │
         ▼
  RagController
         │
         ▼
  RagService.query()
    │
    ├─1─ BiasCheckerService.detectBiasInQuery()
    │       Regex-based check for protected characteristics
    │       Flags query if gender/age/race/religion found
    │
    ├─2─ EmbeddingService.embedQuery()
    │       POST http://ollama:11434/api/embeddings
    │       Returns float[768] for the query text
    │
    ├─3─ Qdrant.searchAsync()
    │       Cosine similarity search in resume-chunks collection
    │       Returns top-K ScoredPoints with payloads
    │
    ├─4─ buildMatches()
    │       Map ScoredPoints → CandidateMatch DTOs
    │       Look up candidate metadata from PostgreSQL
    │
    ├─5─ generateAnswer()  ← the LLM step
    │       Build structured prompt:
    │         - System: role + rules (no hallucination, no bias)
    │         - Context: retrieved resume chunks
    │         - Query: recruiter's question
    │       POST http://ollama:11434/api/chat (llama3.1:8b)
    │       temperature=0.3 for consistent factual output
    │
    ├─6─ BiasCheckerService.sanitizeOutput()
    │       Strip protected characteristic mentions from LLM output
    │       Prepend bias warning if query was flagged in step 1
    │
    ├─7─ AuditLogRepository.save()
    │       Log: query text, matched IDs, bias flag, model used
    │       Immutable record for compliance
    │
    └─8─ Return RagQueryResponse
              {aiAnswer, matches, biasDetected, auditLogId}
```

---

## Data Model

### PostgreSQL

| Table | Purpose |
|---|---|
| `tenants` | Multi-tenancy root — all data scoped by tenant |
| `candidates` | Profile + raw resume text + Qdrant point IDs |
| `jobs` | Job title + raw JD text + Qdrant point IDs |
| `audit_logs` | Immutable record of every RAG query |
| `match_feedback` | Recruiter ratings on AI matches (Phase 3) |

### Qdrant Collections

| Collection | Contents | Vector size |
|---|---|---|
| `resume-chunks` | One vector per resume section | 768 (nomic-embed-text) |
| `job-descriptions` | One vector per JD section | 768 (nomic-embed-text) |

Each vector point stores a payload with `candidateId`/`jobId`, `section`, `text`, and `model` — so the full context can be reconstructed from Qdrant alone.

---

## Key Design Decisions

**Why Ollama instead of OpenAI?**
Zero API cost. `nomic-embed-text` (768-dim) is well-suited for semantic resume search. `llama3.1:8b` is small enough to run on a laptop with 8GB RAM and produces coherent structured output at temperature=0.3.

**Why Qdrant over pgvector?**
Qdrant has a native Java gRPC client, better filtering, and separates the vector concern cleanly from relational data. pgvector would work too but mixing vector and relational schemas in one DB adds complexity.

**Why rule-based bias detection?**
EU AI Act requires explainable AI in high-risk domains. Regex rules are transparent, auditable, and easy to extend. An ML-based bias classifier would be a black box and harder to justify to regulators.

**Why chunk resumes by section?**
A query for "Python skills" should match the Skills section specifically, not get diluted by the candidate's Education section. Section-level chunking improves retrieval precision significantly over whole-document embedding.

---

## Security Considerations

- **Phase 1 (current):** All endpoints are open — for local development only
- **Phase 3 (planned):** JWT via Spring Security OAuth2 Resource Server
- Candidate raw text stored in PostgreSQL — consider object storage (MinIO) at scale
- Audit logs are append-only — no update/delete endpoints
- Sensitive fields (email, phone) should be encrypted at rest in production

---

## Performance Notes

- Embedding is async (`@Async`) — resume upload returns immediately while vectorization happens in the background
- Qdrant batch upsert — all chunks for one resume are sent in a single API call
- `topK` defaults to 5 — retrieving more chunks improves answer quality but increases prompt size and LLM latency
- `llama3.1:8b` response time: ~5–15 seconds on CPU, ~1–3 seconds on GPU
