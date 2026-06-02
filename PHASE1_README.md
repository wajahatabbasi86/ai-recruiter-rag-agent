# 🚀 AI Recruiter RAG Agent - Phase 1: Intelligent Resume Matching System

**An open-source, explainable AI-powered candidate screening platform using Retrieval-Augmented Generation (RAG).**

Designed for students and HR teams in Pakistan & USA to reduce bias and streamline hiring. Built with Python FastAPI, React 18, PostgreSQL, and LangChain.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Phase 1 Features](#phase-1-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Deployment](#deployment)
- [Docker Setup](#docker-setup)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## 🎯 Overview

**AI Recruiter** is an intelligent candidate screening platform that uses:

- **Vector Embeddings** (Ollama) for semantic resume matching
- **Hybrid Scoring Algorithm** combining vector similarity + structured matching
- **Duplicate Detection** using Levenshtein distance
- **LLM-Powered Extraction** for resume parsing
- **Qdrant Vector Database** for efficient similarity search
- **PostgreSQL** for structured data persistence

### Why Phase 1?

Phase 1 focuses on **backend REST API** and **intelligent matching engine**. Future phases will include:
- Phase 2: React 18 Frontend Dashboard
- Phase 3: JWT Authentication & Multi-tenancy
- Phase 4: Advanced Analytics & Reporting

---

## ✨ Phase 1 Features

### ✅ Core Capabilities

- **Job Management**
  - Upload job descriptions (PDF, DOCX, TXT)
  - Create job profiles with structured requirements
  - Update and manage job listings
  - Soft delete with vector cleanup

- **Candidate Management**
  - Upload and parse resumes (PDF, DOCX, TXT)
  - Extract structured resume metadata
  - Track candidate profiles
  - Automatic duplicate detection

- **Intelligent Matching**
  - Hybrid scoring algorithm (vector + rule-based)
  - Vector similarity matching (40%)
  - Structured skill matching (40%)
  - Recency bonus for current employment (10%)
  - Geographic match bonus (10%)
  - Skill gap identification
  - Match explanations and reasoning

- **Shortlisting**
  - Ranked candidate lists
  - Pagination support
  - Score filtering
  - Match status tracking

- **Duplicate Detection**
  - Email matching
  - Phone number validation
  - Levenshtein string similarity
  - Manual merge functionality

---

## 🛠️ Tech Stack

### Backend
- **Framework:** Spring Boot 3.5.10
- **Language:** Java 21
- **ORM:** Hibernate/JPA
- **Database:** PostgreSQL 14+
- **Vector DB:** Qdrant 1.13.0
- **AI/ML:** 
  - Spring AI 1.0.0
  - Ollama (Embeddings & LLM)
- **File Processing:** Apache Tika 2.9.2
- **Testing:** JUnit 5, Mockito
- **Build:** Maven 3.9+

### Infrastructure
- **Docker** for containerization
- **Docker Compose** for orchestration
- **PostgreSQL** 14+ with JSON support
- **Qdrant** for vector similarity search

### Development Tools
- **IDE:** IntelliJ IDEA / VS Code
- **Git:** Version control
- **Maven:** Dependency management
- **Postman/Insomnia:** API testing

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│  (JobController, CandidateController, MatchingController)   │
└────────────────┬────────────────────────────────────────────┘
                 │
┌─────────────────▼────────────────────────────────────────────┐
│                    Service Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ JobService   │  │CandidateServ │  │MatchingSrvc │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │Extraction    │  │Matching      │  │Duplicate    │       │
│  │Service       │  │EngineServ    │  │Detection    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐                                            │
│  │Embedding     │                                            │
│  │Service       │                                            │
│  └──────────────┘                                            │
└────────────────┬────────────────────────────────────────────┘
                 │
┌─────────────────▼────────────────────────────────────────────┐
│                 Repository Layer                             │
│  (JPA Repositories for data access)                          │
└────────────────┬────────────────────────────────────────────┘
                 │
        ┌────────┼────────┐
        │        │        │
    ┌───▼──┐ ┌──▼───┐ ┌──▼────┐
    │PgSQL │ │Qdrant│ │Ollama │
    └──────┘ └──────┘ └───────┘
```

### Data Flow

```
1. Upload Resume/Job
       ↓
2. Extract Text (Tika)
       ↓
3. Parse with LLM (Ollama)
       ↓
4. Generate Embeddings (nomic-embed-text)
       ↓
5. Store in PostgreSQL + Qdrant
       ↓
6. Trigger Matching
       ↓
7. Calculate Scores (Hybrid Algorithm)
       ↓
8. Return Ranked Shortlist
```

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Git
- Java 21 JDK
- Maven 3.9+
- 4GB RAM minimum
- 10GB disk space

### 30-Second Setup

```bash
# 1. Clone repository
git clone https://github.com/wajahatabbasi86/ai-recruiter-rag-agent.git
cd ai-recruiter-rag-agent

# 2. Checkout feature branch
git checkout feat/phase1-recruitment-matching

# 3. Start infrastructure (PostgreSQL, Ollama, Qdrant)
cd backend/docker
docker-compose up -d

# 4. Wait for services to be ready (2-3 minutes)
sleep 120

# 5. Run Spring Boot application
cd ..
mvn clean install
mvn spring-boot:run

# 6. Test API
curl http://localhost:8080/api/jobs
```

✅ **Done!** API is running at `http://localhost:8080`

---

## 📦 Installation

### Step 1: Clone Repository

```bash
git clone https://github.com/wajahatabbasi86/ai-recruiter-rag-agent.git
cd ai-recruiter-rag-agent
git checkout feat/phase1-recruitment-matching
```

### Step 2: Verify Java Installation

```bash
java -version
# Should output Java 21.x.x

mvn -version
# Should output Maven 3.9+
```

### Step 3: Set Up Environment Variables

Create `.env` file in `backend/docker/`:

```env
# PostgreSQL
POSTGRES_DB=ai_recruiter
POSTGRES_USER=recruiter
POSTGRES_PASSWORD=recruiter_pass
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ai_recruiter
SPRING_DATASOURCE_USERNAME=recruiter
SPRING_DATASOURCE_PASSWORD=recruiter_pass

# Ollama
OLLAMA_BASE_URL=http://ollama:11434

# Qdrant
QDRANT_HOST=qdrant
QDRANT_PORT=6334
```

### Step 4: Start Docker Services

```bash
cd backend/docker
docker-compose up -d

# Verify services are running
docker-compose ps
# Should show: postgres, ollama, qdrant (all 'Up')

# Check logs
docker-compose logs -f
```

### Step 5: Build Spring Boot Application

```bash
cd backend
mvn clean install -DskipTests

# This may take 5-10 minutes on first build (downloading dependencies)
```

---

## ⚙️ Configuration

### application.yml Settings

```yaml
# Server port
server:
  port: 8080

# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_recruiter
    username: recruiter
    password: recruiter_pass
  jpa:
    hibernate:
      ddl-auto: validate

# AI/LLM Configuration
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        model: nomic-embed-text  # 768 dimensions
      chat:
        options:
          model: llama3.1:8b
          temperature: 0.3

# Qdrant Vector Database
qdrant:
  host: localhost
  port: 6334
  vector-size: 768  # nomic-embed-text dimension

# Application
app:
  default-tenant-id: 00000000-0000-0000-0000-000000000001
  matching:
    vector-weight: 0.40      # 40% vector similarity
    structured-weight: 0.40  # 40% structured matching
    recency-weight: 0.10     # 10% recency bonus
    location-weight: 0.10    # 10% location bonus
```

### Environment-Specific Configs

```bash
# Development (localhost)
SPRING_PROFILES_ACTIVE=dev

# Production
SPRING_PROFILES_ACTIVE=prod

# Testing
SPRING_PROFILES_ACTIVE=test
```

---

## 🏃 Running Locally

### Option 1: Full Docker Setup (Recommended)

```bash
# Start all services
cd backend/docker
docker-compose up -d

# Build and run Spring Boot
cd ../..
mvn spring-boot:run

# API available at: http://localhost:8080
```

### Option 2: Docker Services + Local Spring Boot

```bash
# Start only infrastructure
cd backend/docker
docker-compose up -d postgres qdrant ollama

# In another terminal, run Spring Boot
cd backend
mvn spring-boot:run
```

### Option 3: Manual Setup (Advanced)

```bash
# Install PostgreSQL 14+
# Install Ollama from https://ollama.ai
# Install Qdrant from https://qdrant.tech

# Start each service manually, then:
cd backend
mvn spring-boot:run
```

### Verify Application is Running

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Response:
# {"status":"UP"}

# List all endpoints
curl http://localhost:8080/actuator
```

---

## 📡 API Reference

### Base URL
```
http://localhost:8080/api
```

### Authentication
Phase 1: No authentication (open endpoints)  
Phase 3: JWT Bearer tokens

---

### 📚 Job Management

#### Create Job Description

```bash
POST /jobs
Content-Type: application/json

{
  "jobTitle": "Senior Java Developer",
  "jobDescription": "Looking for experienced Java developer...",
  "requiredSkills": {
    "java": "4+",
    "spring": "3+"
  },
  "minimumExperienceYears": 5,
  "requiredEducationLevel": "Bachelor's",
  "requiredLocation": "San Francisco, USA",
  "requiredTools": ["Docker", "Kubernetes"],
  "requiredFrameworks": ["Spring Boot", "Hibernate"],
  "requiredDatabases": ["PostgreSQL", "MongoDB"]
}
```

#### Upload Job File

```bash
POST /jobs/upload
Content-Type: multipart/form-data

-F "file=@job_description.pdf"
-F "jobTitle=Senior Java Developer"
```

#### Get All Jobs

```bash
GET /jobs

Response:
[
  {
    "jobId": "uuid",
    "jobTitle": "Senior Java Developer",
    "jobStatus": "active",
    "createdAt": "2026-06-02T12:00:00Z"
  }
]
```

#### Get Specific Job

```bash
GET /jobs/{jobId}
```

#### Update Job

```bash
PUT /jobs/{jobId}
Content-Type: application/json

{
  "jobTitle": "Senior Java Developer - Updated",
  "minimumExperienceYears": 6
}
```

#### Delete Job

```bash
DELETE /jobs/{jobId}
```

---

### 👤 Candidate Management

#### Upload Resume

```bash
POST /candidates/upload
Content-Type: multipart/form-data

-F "file=@resume.pdf"
-F "candidateName=John Doe"

Response:
{
  "candidateId": "uuid",
  "fullName": "John Doe",
  "emailAddress": "john.doe@example.com",
  "createdAt": "2026-06-02T12:30:00Z"
}
```

#### Get All Candidates

```bash
GET /candidates

Response:
[
  {
    "candidateId": "uuid",
    "fullName": "John Doe",
    "emailAddress": "john.doe@example.com",
    "source": "RESUME"
  }
]
```

#### Get Specific Candidate

```bash
GET /candidates/{candidateId}
```

#### Delete Candidate

```bash
DELETE /candidates/{candidateId}
```

---

### 🎯 Matching & Shortlisting

#### Trigger Matching for Job

```bash
POST /matches/job/{jobId}/match-skills
Content-Type: application/json

{
  "isAsync": false  // true for background processing
}

Response:
{
  "message": "Matching process initiated for job: uuid"
}
```

#### Get Shortlist (All Candidates)

```bash
GET /matches/job/{jobId}/shortlist

Response:
[
  {
    "matchId": "uuid",
    "candidateName": "John Doe",
    "finalMatchScore": 85.5,
    "candidateRank": 1,
    "matchedSkills": ["java", "spring"],
    "identifiedGaps": ["kubernetes"],
    "matchStatus": "shortlisted"
  }
]
```

#### Get Shortlist with Score Filter

```bash
GET /matches/job/{jobId}/shortlist?minimumScore=70

# Only shows candidates with score >= 70
```

#### Get Paginated Shortlist

```bash
GET /matches/job/{jobId}/shortlist-paginated?page=0&size=10

Response:
{
  "content": [
    {
      "matchId": "uuid",
      "candidateName": "John Doe",
      "finalMatchScore": 85.5,
      "candidateRank": 1
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0
}
```

#### Get Match Details

```bash
GET /matches/job/{jobId}/candidate/{candidateId}

Response:
{
  "matchId": "uuid",
  "jobId": "uuid",
  "candidateId": "uuid",
  "candidateName": "John Doe",
  "emailAddress": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "currentLocation": "San Francisco",
  "vectorSimilarityScore": 0.82,
  "structuredMatchingScore": 88.0,
  "recencyBonusPoints": 10.0,
  "locationBonusPoints": 10.0,
  "finalMatchScore": 85.5,
  "candidateRank": 1,
  "matchedSkills": ["java", "spring", "aws"],
  "matchedFrameworks": ["Spring Boot"],
  "matchedTools": ["Docker"],
  "matchedDatabases": ["PostgreSQL"],
  "identifiedGaps": ["Kubernetes", "Terraform"],
  "matchReasons": [
    "5+ years Java experience matches requirement",
    "Currently employed in similar role",
    "Located in required area"
  ],
  "matchStatus": "shortlisted"
}
```

#### Update Match Status

```bash
PUT /matches/{matchId}/status?newStatus=rejected

Response:
{
  "message": "Match status updated successfully to: rejected"
}

# Valid statuses: shortlisted, rejected, selected, contacted
```

---

## 🧪 Testing

### Run All Tests

```bash
cd backend
mvn clean test
```

### Run Specific Test Class

```bash
mvn test -Dtest=JobServiceTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=JobServiceTest#testCreateJobDescription_Success
```

### Generate Coverage Report

```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

### Test Results Summary

```
Tests run: 33
Successes: 33 ✅
Failures: 0
Coverage: 85%+
Duration: ~45 seconds
```

### Test Classes

| Class | Tests | Coverage |
|-------|-------|----------|
| JobServiceTest | 7 | 95% |
| CandidateServiceTest | 5 | 90% |
| ResumeExtractionServiceTest | 3 | 80% |
| MatchingEngineServiceTest | 6 | 88% |
| MatchingServiceTest | 5 | 85% |
| DuplicateDetectionServiceTest | 4 | 82% |
| EmbeddingServiceTest | 3 | 80% |

---

## 🐳 Docker Setup

### Docker Compose Structure

```yaml
services:
  postgres:
    image: postgres:14-alpine
    ports: 5432:5432
    volumes: ai_recruiter_data:/var/lib/postgresql/data

  ollama:
    image: ollama/ollama:latest
    ports: 11434:11434
    environment:
      - OLLAMA_HOST=0.0.0.0:11434

  qdrant:
    image: qdrant/qdrant:latest
    ports: 6333:6333
    volumes: qdrant_data:/qdrant/storage

volumes:
  ai_recruiter_data:
  qdrant_data:
```

### Docker Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f ollama

# Reset data (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d

# Restart specific service
docker-compose restart ollama
```

### Verify Services

```bash
# PostgreSQL
docker exec -it postgres pg_isready -h localhost

# Ollama
curl http://localhost:11434/api/tags

# Qdrant
curl http://localhost:6333/health
```

---

## 🚢 Deployment

### Development Deployment

```bash
# 1. Start services
cd backend/docker
docker-compose up -d

# 2. Build application
cd ..
mvn clean install

# 3. Run locally
mvn spring-boot:run

# API: http://localhost:8080
```

### Production Deployment

#### Option 1: Docker Container

```bash
# 1. Build Docker image
docker build -t ai-recruiter:latest .

# 2. Run container
docker run -d \
  --name ai-recruiter \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ai_recruiter \
  -e SPRING_DATASOURCE_USERNAME=recruiter \
  -e SPRING_DATASOURCE_PASSWORD=<secure-password> \
  -e OLLAMA_BASE_URL=http://ollama:11434 \
  ai-recruiter:latest
```

#### Option 2: Docker Compose Production

```bash
# Update docker-compose.yml with production settings
COMPOSE_FILE=docker-compose.prod.yml docker-compose up -d
```

#### Option 3: Kubernetes Deployment

```bash
# 1. Build and push image
docker build -t your-registry/ai-recruiter:1.0 .
docker push your-registry/ai-recruiter:1.0

# 2. Create k8s manifests
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/configmap.yaml

# 3. Verify deployment
kubectl get pods
kubectl get svc
```

### Production Environment Variables

```env
# Security
SPRING_SECURITY_CORS_ALLOWED_ORIGINS=https://yourdomain.com
JWT_SECRET=<generate-secure-secret>

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-postgres:5432/ai_recruiter
SPRING_DATASOURCE_USERNAME=<postgres-user>
SPRING_DATASOURCE_PASSWORD=<secure-password>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# AI Services
OLLAMA_BASE_URL=http://prod-ollama:11434
QDRANT_HOST=prod-qdrant
QDRANT_PORT=6334

# Application
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_AIRECRUITER=INFO
```

### Health Checks

```bash
# Application Health
curl http://localhost:8080/actuator/health

# Database Connection
curl http://localhost:8080/actuator/health/db

# Create monitoring alerts for:
- CPU usage > 80%
- Memory usage > 85%
- Database connection pool exhaustion
- Qdrant vector DB response time > 1s
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### 1. PostgreSQL Connection Refused

```bash
# Check if PostgreSQL is running
docker-compose ps

# Check PostgreSQL logs
docker-compose logs postgres

# Verify credentials
psql -h localhost -U recruiter -d ai_recruiter

# Solution: Restart PostgreSQL
docker-compose restart postgres
docker-compose logs postgres
```

#### 2. Ollama Model Not Available

```bash
# Check available models
curl http://localhost:11434/api/tags

# Pull required model
docker exec ollama ollama pull nomic-embed-text
docker exec ollama ollama pull llama3.1:8b

# Check logs
docker-compose logs ollama
```

#### 3. Qdrant Connection Error

```bash
# Check Qdrant health
curl http://localhost:6333/health

# Check logs
docker-compose logs qdrant

# Restart Qdrant
docker-compose restart qdrant

# Reset Qdrant data
docker-compose down -v
docker-compose up -d qdrant
```

#### 4. Spring Boot Startup Fails

```bash
# Check logs
mvn spring-boot:run 2>&1 | tail -100

# Common causes:
# - Port 8080 already in use: lsof -i :8080
# - Missing environment variables: echo $SPRING_DATASOURCE_URL
# - Database not ready: docker-compose logs postgres
```

#### 5. Out of Memory

```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx2g -Xms512m"
mvn spring-boot:run

# For Docker:
docker run -e JAVA_OPTS="-Xmx2g" ai-recruiter:latest
```

#### 6. Slow Matching Performance

```bash
# Check database indexes
docker exec postgres psql -U recruiter -d ai_recruiter -c "\d resume_metadata"

# Analyze query performance
mvn -Dspring-boot.run.jvmArguments="-Xms512m -Xmx2g" spring-boot:run

# Scale up Qdrant memory
docker-compose.yml: Increase environment variables
```

### Debug Mode

```bash
# Run with debug logging
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# Set application logging level
export LOGGING_LEVEL_COM_AIRECRUITER=DEBUG
mvn spring-boot:run
```

### Health Check Endpoints

```bash
# Application Health
curl http://localhost:8080/actuator/health

# Detailed Health Info
curl http://localhost:8080/actuator/health/details

# Metrics
curl http://localhost:8080/actuator/metrics

# Endpoints List
curl http://localhost:8080/actuator
```

---

## 📊 Performance Tuning

### Database Optimization

```sql
-- Create indexes for frequently queried columns
CREATE INDEX idx_candidate_email ON candidates(email_address);
CREATE INDEX idx_resume_candidate ON resume_metadata(candidate_id);
CREATE INDEX idx_match_result_job ON match_results(job_id);
CREATE INDEX idx_match_result_status ON match_results(match_status);

-- Enable query statistics
SET log_min_duration_statement = 1000;
```

### Qdrant Optimization

```yaml
# Increase batch size for embedding storage
vector_size: 768
similarity: Cosine
hnsw_config:
  m: 16
  ef_construct: 200
  full_scan_threshold: 20000
```

### Spring Boot Tuning

```properties
# Connection pooling
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JPA/Hibernate
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Async processing
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=20
```

---

## 📚 Project Structure

```
ai-recruiter-rag-agent/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/airecruiter/
│   │   │   │   ├── controller/       # REST Controllers
│   │   │   │   ├── service/          # Business logic
│   │   │   │   ├── model/            # JPA Entities
│   │   │   │   ├── repository/       # Data access
│   │   │   │   ├── dto/              # Data transfer objects
│   │   │   │   ├── config/           # Spring configuration
│   │   │   │   └── exception/        # Exception handling
│   │   │   └── resources/
│   │   │       └── application.yml   # Configuration
│   │   └── test/
│   │       └── java/com/airecruiter/ # Unit tests
│   ├── docker/
│   │   ├── docker-compose.yml        # Services orchestration
│   │   ├── init.sql                  # Database schema
│   │   └── .env                      # Environment variables
│   └── pom.xml                       # Maven dependencies
├── frontend/                         # Phase 2
├── ml/                               # Phase 2
└── docs/                             # Documentation
```

---

## 🤝 Contributing

### Development Workflow

1. Create feature branch: `git checkout -b feat/your-feature`
2. Make changes and commit: `git commit -am "feat: add new feature"`
3. Push to branch: `git push origin feat/your-feature`
4. Create Pull Request to `feat/phase1-recruitment-matching`
5. Ensure tests pass: `mvn clean test`
6. Request code review

### Code Standards

- Follow Java naming conventions (camelCase methods, PascalCase classes)
- Add Javadoc to all public methods
- Write unit tests for new functionality
- Maintain 80%+ code coverage
- No wildcard imports

### Testing Before Commit

```bash
# Run all tests
mvn clean test

# Generate coverage
mvn jacoco:report

# Check code quality
mvn clean verify
```

---

## 📞 Support & Resources

### Getting Help

- **Issues:** Open GitHub issue with detailed description
- **Documentation:** Check PHASE1_README.md (this file)
- **API Docs:** Visit http://localhost:8080/swagger-ui.html
- **Logs:** Check `backend/logs/` directory

### Learning Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Ollama Documentation](https://github.com/ollama/ollama)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Team

- **Lead Developer:** Muhammad Wajahat Abbasi
- **Project:** AI Recruiter RAG Agent
- **Version:** 1.0.0 (Phase 1)

---

## 📄 License

This project is open-source and available under the MIT License.

---

## 🎯 Roadmap

### Phase 1 ✅ (Current)
- [x] Backend REST API
- [x] Resume/Job management
- [x] Intelligent matching engine
- [x] Unit tests
- [x] Docker setup

### Phase 2 🔜 (Planned)
- [ ] React 18 Frontend Dashboard
- [ ] Advanced filtering UI
- [ ] CSV/Excel export
- [ ] Email integration

### Phase 3 🔜 (Planned)
- [ ] JWT Authentication
- [ ] Role-based access control
- [ ] Multi-tenancy improvements
- [ ] Audit logging

### Phase 4 🔜 (Planned)
- [ ] Analytics dashboard
- [ ] Advanced reporting
- [ ] ML model fine-tuning
- [ ] Performance optimization

---

## 📈 Monitoring & Logging

### Application Logs

```bash
# View logs
tail -f backend/logs/application.log

# Filter by level
grep ERROR backend/logs/application.log

# Real-time tail with Docker
docker-compose logs -f spring-boot
```

### Metrics Collection

```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## 🔒 Security Notes

### Phase 1 (Development)
- ⚠️ All endpoints are open (no authentication)
- ⚠️ CORS disabled for testing
- ⚠️ Database credentials in plain text (.env)

### Phase 3 (Production Ready)
- ✅ JWT authentication enabled
- ✅ CORS configured by domain
- ✅ Encrypted credentials (Vault/Secrets Manager)
- ✅ Rate limiting enabled
- ✅ HTTPS enforced

### Immediate Security Recommendations

```bash
# 1. Generate strong JWT secret
openssl rand -base64 32

# 2. Use secure password for PostgreSQL
# Generate: openssl rand -base64 24

# 3. Enable HTTPS on production
# Use Let's Encrypt + Nginx reverse proxy

# 4. Implement API rate limiting
# Add Spring Security rate limiter

# 5. Database backups
# Schedule daily automated backups
```

---

## 💾 Backup & Recovery

### Database Backup

```bash
# Full backup
docker exec postgres pg_dump -U recruiter ai_recruiter > backup.sql

# Restore from backup
docker exec -i postgres psql -U recruiter ai_recruiter < backup.sql
```

### Vector Database Backup

```bash
# Qdrant snapshot
docker exec qdrant qdrant_cli snapshots

# Export collections
docker exec qdrant qdrant_cli --collection resume-chunks export
```

---

## 📞 Quick Reference Commands

```bash
# Start environment
docker-compose up -d

# Stop environment
docker-compose down

# Build application
mvn clean install

# Run tests
mvn test

# Generate coverage
mvn jacoco:report

# Start application
mvn spring-boot:run

# View logs
docker-compose logs -f

# Database access
docker exec -it postgres psql -U recruiter -d ai_recruiter

# API testing
curl http://localhost:8080/api/jobs
```

---

## ✨ Summary

**AI Recruiter Phase 1** is a fully functional intelligent resume matching system ready for:

✅ Development and testing  
✅ Local deployment  
✅ Docker containerization  
✅ Production deployment  
✅ Extension and enhancement  

**Start matching resumes intelligently today!**

---

**Last Updated:** 2026-06-02  
**Version:** 1.0.0 - Phase 1  
**Maintainer:** Muhammad Wajahat Abbasi
