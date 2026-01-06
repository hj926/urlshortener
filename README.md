URL Shortener Service
A production-style URL Shortener backend service built with Spring Boot, featuring Redis caching, MySQL persistence, access statistics, expiration (TTL), and Docker-based one-command deployment.

🚀 Features
🔗 Generate short URLs from long URLs
↪️ Redirect short URLs to original URLs (HTTP 302)
⏳ Support optional expiration time (TTL)
🔁 Prevent duplicate generation for permanent URLs
⚡ High-performance read path with Redis caching
📊 Access statistics (PV / hit count)
🐳 One-command startup using Docker Compose

🧱 Tech Stack
Language: Java 21
Framework: Spring Boot, Spring Data JPA
Database: MySQL 8
Cache: Redis
Build Tool: Maven
Deployment: Docker, Docker Compose

🏗️ Architecture Overview
Client
  |
  | HTTP
  v
Spring Boot Application
  ├── Controller Layer
  ├── Service Layer
  │     ├── URL generation (Base62)
  │     ├── Cache-aside logic
  │     ├── TTL & expiration check
  │     └── Hit count tracking
  ├── Redis
  │     ├── Short URL cache
  │     └── Hit counter (INCR)
  └── MySQL
        └── Persistent URL mappings & statistics

⚙️ Core Design Decisions
1️⃣ Short URL Generation
Uses MySQL auto-increment ID
Encoded into Base62 for compact short codes
Collision-free and deterministic
Designed for easy upgrade to Snowflake / segment-based ID generators
2️⃣ Cache Strategy (Redis)
Cache-aside pattern
Hot-path reads served directly from Redis
TTL-aware caching to avoid serving expired URLs
Cache penetration protection:
Store short-lived NULL markers for non-existent or expired keys
3️⃣ Expiration (TTL)
Optional ttlSeconds during URL creation
Expiration enforced at:
Cache layer (Redis TTL)
Database layer (expireAt check)
Expired URLs return HTTP 410 (Gone)
4️⃣ Access Statistics
Redis INCR used for fast hit counting
Periodic background job aggregates counts into MySQL
Minimizes database write amplification under high read traffic

📦 API Endpoints
🔹 Create Short URL
POST /api/v1/shorten
Content-Type: application/json
{
  "longUrl": "https://www.google.com",
  "ttlSeconds": 60
}
Response
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/abc123"
}
🔹 Redirect
GET /{shortCode}
Returns 302 Found with Location header
Invalid code → 404
Expired code → 410
🔹 Statistics
GET /api/v1/stats/{shortCode}
{
  "shortCode": "abc123",
  "longUrl": "https://www.google.com",
  "hitCount": 10,
  "pendingHits": 2,
  "totalHits": 12
}

🐳 Run with Docker (One Command)
1️⃣ Build JAR
./mvnw clean package -DskipTests
2️⃣ Start All Services
docker compose up --build -d
This will start:
Spring Boot app
MySQL
Redis
3️⃣ Verify
curl http://localhost:8080/actuator/health
Expected:
{ "status": "UP" }
🔧 Environment Profiles
local: Run app locally with Dockerized MySQL & Redis
docker: Fully containerized environment
Profiles are configured via:
spring.profiles.active=local | docker

🧠 Future Improvements
Snowflake or segment-based ID generator
Bloom filter for cache penetration protection
Rate limiting for abuse prevention
Read replica support
Kubernetes deployment

📌 Why This Project
This project demonstrates:
Backend system design fundamentals
Cache + database hybrid architecture
Performance optimization under read-heavy workloads
Production-ready deployment using Docker

👤 Author
Huangjie