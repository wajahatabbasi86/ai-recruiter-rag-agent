# Contributing

Thanks for your interest in contributing to the AI Recruiter RAG Agent!

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker + Docker Compose
- Git

### Local Setup

```bash
git clone https://github.com/wajahatabbasi86/ai-recruiter-rag-agent.git
cd ai-recruiter-rag-agent/backend

# Start infrastructure (Postgres, Qdrant, Ollama) without the app
docker-compose up -d postgres qdrant ollama

# Pull models (first time)
docker exec -it ai-recruiter-ollama ollama pull llama3.1:8b
docker exec -it ai-recruiter-ollama ollama pull nomic-embed-text

# Run the app locally (hot reload)
mvn spring-boot:run
```

---

## Branching Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, always deployable |
| `feat/your-feature` | New features |
| `fix/your-fix` | Bug fixes |
| `docs/your-docs` | Documentation only |

Always branch from `main`. Never push directly to `main`.

---

## Making a Contribution

1. Fork the repo
2. Create your branch: `git checkout -b feat/job-matching-score`
3. Make your changes
4. Write or update tests where applicable
5. Commit with a clear message: `git commit -m "feat: add job-candidate matching score endpoint"`
6. Push: `git push origin feat/job-matching-score`
7. Open a Pull Request against `main`

### Commit Message Format

```
type: short description

types: feat | fix | docs | refactor | test | chore
```

Examples:
- `feat: add skill gap analysis endpoint`
- `fix: clean up Qdrant vectors on candidate delete`
- `docs: add architecture diagrams`

---

## Code Style

- Follow standard Java conventions
- Lombok is used — use `@Slf4j`, `@RequiredArgsConstructor`, `@Builder` where appropriate
- Add Javadoc to public service methods explaining _why_, not just _what_
- Keep controllers thin — business logic belongs in services

---

## Reporting Issues

Use the GitHub Issues tab. Choose the appropriate template:
- **Bug report** — something is broken
- **Feature request** — new capability you'd like to see
- **Question** — ask about design decisions or usage

---

## Areas Where Help is Needed

- Writing tests (unit + integration)
- React frontend (Phase 3)
- Urdu language support
- Improving the BiasChecker patterns
- Documentation improvements

---

## Code of Conduct

Be respectful. This is an open-source project aimed at making fair hiring accessible. Contributions from all backgrounds are welcome.
