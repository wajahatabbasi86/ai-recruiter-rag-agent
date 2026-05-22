# AI Recruiter RAG Agent

An **open-source, AI-powered candidate screening platform** built with Java, Spring Boot, and fully local (free) AI tools.

Uses **Retrieval-Augmented Generation (RAG)** to match candidates to job descriptions, reduce bias in hiring, and provide explainable, auditable results — with no OpenAI costs.

> ⚠️ This tool is designed to **assist** human decision-making, not replace it. All AI outputs include a mandatory human-review disclaimer.

---

## ✨ Key Features

- 📄 Resume parsing (PDF, DOCX, TXT) via Apache Tika
- 🧩 Semantic chunking by section (SKILLS, EXPERIENCE, EDUCATION…)
- 🔍 Vector similarity search via Qdrant
- 🤖 Local LLM generation via Ollama (llama3.1:8b) — no API keys
- ⚖️ Bias detection and output sanitization
- 📋 Full audit log for every AI query (EU AI Act compliance)
- 🏢 Multi-tenant architecture from day 1
- 🐳 One-command local setup via Docker Compose

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| Backend framework | Spring Boot 3.3, Java 21 |
| AI / RAG | Spring AI + Ollama |
| LLM | llama3.1:8b (local, free) |
| Embeddings | nomic-embed-text (local, free) |
| Vector DB | Qdrant (self-hosted) |
| Relational DB | PostgreSQL 16 |
| Resume parsing | Apache Tika 2.9 |
| Frontend | React (Phase 3) |

---

## 🚀 Quick Start

### Prerequisites

- Docker + Docker Compose
- 8GB+ RAM (for local LLM)
- Java 21 + Maven (for development)

### 1. Clone the repo

```bash
git clone https://github.com/wajahatabbasi86/ai-recruiter-rag-agent.git
cd ai-recruiter-rag-agent
```

### 2. Start all services

```bash
cd backend
docker-compose up -d
```

This starts PostgreSQL, Qdrant, Ollama, and the Spring Boot app.

### 3. Pull the AI models (first time only)

```bash
docker exec -it ai-recruiter-ollama ollama pull llama3.1:8b
docker exec -it ai-recruiter-ollama ollama pull nomic-embed-text
```

This downloads ~5GB. Only needed once — models persist in a Docker volume.

### 4. Verify everything is running

```bash
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

---

## 📡 API Reference

### Resume Ingestion

```bash
# Upload a resume
curl -X POST http://localhost:8080/api/resumes/upload \
  -F "file=@resume.pdf" \
  -F "candidateName=John Doe"

# List all candidates
curl http://localhost:8080/api/candidates

# Update a candidate profile
curl -X PUT http://localhost:8080/api/candidates/{id} \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","phone":"+923001234567"}'

# Delete a candidate (also removes Qdrant vectors)
curl -X DELETE http://localhost:8080/api/candidates/{id}
```

### Job Descriptions

```bash
# Upload a JD file
curl -X POST http://localhost:8080/api/jobs/upload \
  -F "file=@job_description.pdf" \
  -F "title=Senior Java Developer"

# Ingest JD as plain text
curl -X POST http://localhost:8080/api/jobs/text \
  -H "Content-Type: application/json" \
  -d '{"title":"Backend Engineer","description":"We are looking for..."}'

# List all jobs
curl http://localhost:8080/api/jobs
```

### RAG Query

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Find Java developers with Spring Boot and microservices experience",
    "topK": 5
  }'
```

**Response includes:**
- `aiAnswer` — structured LLM-generated summary with candidate ranking
- `matches` — list of candidates with similarity scores and resume excerpts
- `biasDetected` — true if the query contained protected characteristic terms
- `auditLogId` — UUID for traceability

### Request Human Review

```bash
curl -X POST "http://localhost:8080/api/rag/request-human-review?auditLogId={id}"
```

---

## 📂 Project Structure

```
ai-recruiter-rag-agent/
├── backend/
│   ├── src/main/java/com/airecruiter/
│   │   ├── controller/         # REST endpoints
│   │   │   ├── RagController.java
│   │   │   ├── ResumeController.java
│   │   │   └── JobController.java
│   │   ├── service/            # Business logic
│   │   │   ├── RagService.java
│   │   │   ├── CandidateService.java
│   │   │   ├── JobService.java
│   │   │   ├── EmbeddingService.java
│   │   │   ├── ResumeParserService.java
│   │   │   ├── JobParserService.java
│   │   │   └── BiasCheckerService.java
│   │   ├── model/              # JPA entities
│   │   ├── repository/         # Spring Data repos
│   │   ├── dto/                # Request/response objects
│   │   └── config/             # Qdrant, Security config
│   ├── docker/
│   │   └── init.sql            # DB schema
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── pom.xml
├── docs/
│   ├── ARCHITECTURE.md         # System architecture & RAG flow
│   ├── ROADMAP.md              # Phase-by-phase development plan
│   └── modules/                # Per-module documentation
├── frontend/                   # React dashboard (Phase 3)
├── ml/                         # Experimental ML scripts (Phase 2+)
└── .github/
    ├── workflows/              # CI/CD (Phase 4)
    └── ISSUE_TEMPLATE/
```

---

## 🗺️ Roadmap

| Phase | Status | Key Deliverables |
|---|---|---|
| Phase 1 — MVP | ✅ In progress | Resume parser, embeddings, RAG query, Job parser |
| Phase 2 — Enhanced AI | 🔜 Planned | Job-candidate matching scores, improved bias detection |
| Phase 3 — UI & Reporting | 🔜 Planned | React dashboard, diversity metrics, recruiter feedback |
| Phase 4 — Deployment | 🔜 Planned | CI/CD, JWT auth, Prometheus + Grafana monitoring |

See [ROADMAP.md](docs/ROADMAP.md) for full details.

---

## ⚖️ Ethics & Bias Mitigation

- Rule-based bias detection (transparent and auditable over black-box ML)
- Recruiter queries containing protected characteristics (gender, age, race, religion, nationality) are flagged and logged
- AI-generated output is sanitized before delivery
- Every query is logged to an immutable audit table
- All responses include a mandatory human-review disclaimer
- Human review endpoint available for any AI decision

---

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for guidelines.

1. Fork the repo
2. Create a feature branch (`git checkout -b feat/job-matching-score`)
3. Commit your changes
4. Open a Pull Request

---

## 📜 License

MIT License — see [LICENSE](LICENSE) for details.

---

## 👤 Author

**Muhammad Wajahat Abbasi** — open-source developer, Pakistan 🇵🇰
