# AI Recruiter RAG Agent

An **AI-powered, open-source recruitment assistant** that leverages **Retrieval-Augmented Generation (RAG)** to screen candidates, analyze resumes, and reduce bias in hiring decisions.

This project is designed to help **fresh graduates, recruiters, and organizations** assess candidate readiness using real-world, context-aware evaluation rather than simple keyword matching.

---

## 🚀 Key Features

* 🤖 **AI-driven Candidate Screening** using RAG
* 📄 **Resume Parsing & Profile Creation**
* 🧠 **Context-aware Questioning & Evaluation**
* ⚖️ **Bias Detection & Mitigation** in screening results
* 🔍 **Skill Gap Analysis & Readiness Scoring**
* 🌐 **Modern Web Interface** for recruiters and candidates
* 🔓 **Fully Open-Source** and extensible

---

## 🧱 Tech Stack

### Backend (Core Platform – Java)

* **Java 17+**
* **Spring Boot** – Core backend framework
* **Spring Web / Spring MVC** – RESTful APIs
* **Spring Security** – Authentication & authorization (JWT / OAuth2)
* **Spring Data JPA** – Data access layer
* **Hibernate** – ORM
* **PostgreSQL** – Primary relational database

### Backend (AI & RAG Layer – Spring AI)

* **Spring AI** – Native AI & RAG integration for Spring Boot
* **Spring AI Vector Stores** – Semantic search & embeddings (PGVector / Redis / Elasticsearch / Pinecone)
* **Spring AI Prompt Templates** – Structured & reusable prompts
* **Embedding Models** – OpenAI / Azure OpenAI / Open-source models
* **LLM Providers** – OpenAI / Azure OpenAI / Open-source LLMs

### Backend (Optional – Python AI Services)

* **FastAPI** – Optional Python-based AI microservices
* **LangChain** – Advanced agent workflows (optional)

### Frontend

* **React** – Interactive UI
* **Modern component-based architecture**

### AI & Data

* **Retrieval-Augmented Generation (RAG)**
* **Embedding-based semantic search**
* **Bias detection heuristics & scoring models**

---

## 🏗️ High-Level Architecture

1. Candidate uploads resume or creates profile
2. Resume is parsed and converted into embeddings
3. Job requirements & knowledge base indexed in vector store
4. RAG pipeline retrieves relevant context
5. LLM evaluates candidate with bias-aware scoring
6. Recruiter dashboard shows insights and recommendations

---

## 📂 Project Structure (Proposed)

```
ai-recruiter-rag-agent/
├── backend/
│   ├── app/
│   ├── api/
│   ├── services/
│   ├── rag/
│   └── models/
├── frontend/
│   ├── src/
│   ├── components/
│   └── pages/
├── docs/
├── scripts/
├── tests/
└── README.md
```

---

## 🧪 Use Cases

* Fresh graduates assessing job readiness
* Recruiters performing unbiased screening
* Companies experimenting with AI-driven hiring
* Researchers exploring bias mitigation in AI recruitment

---

## 🔐 Ethics & Bias Mitigation

This project emphasizes:

* Transparent scoring logic
* Bias-aware prompt engineering
* Optional anonymization of sensitive attributes
* Fairness-focused evaluation metrics

> ⚠️ This tool is designed to **assist** human decision-making, not replace it.

---

## 📜 License

This project is open-source under the **MIT License**.

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---

## ⭐ Support

If you find this project useful:

* Give it a ⭐ on GitHub
* Share feedback or ideas via Issues

---

## 📬 Contact

Maintained by **Muhammad Wajahat Abbasi**

---

> Building fair, intelligent, and practical AI for hiring 🚀
