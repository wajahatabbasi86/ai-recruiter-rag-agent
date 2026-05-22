# Roadmap

---

## Phase 1 — MVP ✅ In Progress (Weeks 1–4)

**Goal:** Working RAG pipeline from resume upload to AI-generated candidate ranking.

### Completed
- [x] Spring Boot project structure
- [x] Docker Compose with Ollama, Qdrant, PostgreSQL
- [x] Resume parser (Apache Tika — PDF, DOCX, TXT)
- [x] Section-based chunking strategy
- [x] Embedding service (nomic-embed-text via Ollama)
- [x] Qdrant vector storage (resume-chunks collection)
- [x] RAG query pipeline (embed → search → generate)
- [x] Bias detection and output sanitization (rule-based)
- [x] Audit log (every query persisted)
- [x] Candidate CRUD (create, read, update, delete)
- [x] Qdrant vector cleanup on candidate delete
- [x] Job description parser (JobParserService)
- [x] Job ingestion endpoints (file + plain text)
- [x] Multi-tenant data model

### Remaining Phase 1 Items
- [ ] `GET /api/embeddings/search` — expose semantic search as its own endpoint
- [ ] Basic unit tests (ResumeParserService, BiasCheckerService)
- [ ] Integration test: full resume upload → RAG query flow

---

## Phase 2 — Enhanced AI (Weeks 5–7)

**Goal:** Job-to-candidate matching scores and smarter bias handling.

- [ ] Job-candidate matching: cosine similarity between job JD vectors and candidate resume vectors
- [ ] Match score endpoint: `POST /api/matches/job/{jobId}` — returns ranked candidates for a specific job
- [ ] Improve BiasChecker: replace regex with a small local classifier (optional, if explainability permits)
- [ ] Skill gap analysis: identify missing skills between a candidate and a job requirement
- [ ] Candidate ranking improvements: weight recent experience more heavily
- [ ] MinIO integration for raw file storage (instead of storing text in Postgres)

---

## Phase 3 — UI & Reporting (Weeks 8–9)

**Goal:** Recruiter-facing dashboard and feedback loop.

- [ ] React 18 frontend scaffold
- [ ] Candidate list with search and filters
- [ ] Job list with one-click "find matching candidates"
- [ ] RAG query UI — natural language search box
- [ ] Match score display with similarity bars
- [ ] Diversity & bias metrics dashboard
- [ ] Recruiter feedback form (1–5 star rating per match)
- [ ] JWT authentication (Spring Security OAuth2 Resource Server)
- [ ] CORS configuration for React frontend

---

## Phase 4 — Deployment & Monitoring (Weeks 10–11)

**Goal:** Production-ready, observable, and CI/CD-automated.

- [ ] GitHub Actions CI pipeline (build + test on every PR)
- [ ] Prometheus metrics endpoint
- [ ] Grafana dashboard (query latency, bias detection rate, embedding throughput)
- [ ] Alert rules (failed queries, Qdrant unavailable, Ollama timeout)
- [ ] Helm chart or Docker Compose production config
- [ ] Environment-specific configs (dev / staging / prod)
- [ ] Retraining workflow: re-embed all resumes when embedding model is upgraded

---

## Future Ideas (Backlog)

- LinkedIn profile ingestion via browser extension
- Email integration — auto-ingest resumes from recruiter inbox
- Multi-language support (Urdu for Pakistan market)
- Candidate self-service portal — upload your own resume and see your match scores
- GDPR / PDPA right-to-erasure endpoint (already partially supported via delete + Qdrant cleanup)
- A/B testing different LLM prompts to improve answer quality
