#!/bin/bash
set -e

echo "==> Building deps layer (with local .m2 cache)..."
docker run --rm \
  -v "$(pwd)":/app \
  -v "$HOME/.m2":/root/.m2 \
  -w /app \
  maven:3.9.6-eclipse-temurin-21 \
  mvn dependency:resolve package -DskipTests -B -q

echo "==> Building Docker image..."
docker build -t ai-recruiter-app .

echo "==> Done."