# Simple Chat Application Using Redis

A RESTful chat application built with **Spring Boot** and **Redis**, demonstrating how to use multiple Redis data structures together — hashes, sets, lists, and pub/sub — to power a real-time chat backend.

---

## Features

- Create and delete chat rooms
- Join rooms as a named participant
- Send and retrieve messages with timestamps
- Real-time message broadcast via Redis Pub/Sub
- Full validation and structured error responses

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21+ |
| Framework | Spring Boot 3.2.5 |
| Data store | Redis (via Spring Data Redis + Lettuce) |
| Cloud Redis | Upstash |
| Serialization | Jackson |
| Testing | JUnit 5, Mockito, MockMvc |

---

## Architecture

```
Client (curl / Postman)
        │
        ▼
┌──────────────────┐
│  ChatController  │  REST layer — validates input, routes requests
└────────┬─────────┘
         │
         ▼
┌──────────────────────┐
│  ChatRoomServiceImpl │  Business logic
└────────┬─────────────┘
         │
    ┌────┴──────────────────────────────┐
    │                                   │
    ▼                                   ▼
┌─────────────────────┐     ┌────────────────────────┐
│    RedisTemplate    │     │  RedisMessagePublisher  │
│  (CRUD operations)  │     │  (Pub/Sub broadcast)    │
└─────────────────────┘     └────────────────────────┘
         │                               │
         └──────────────┬────────────────┘
                        ▼
              ┌──────────────────┐
              │  Redis / Upstash │
              └──────────────────┘
                        │
                        ▼
              ┌──────────────────────┐
              │  MessageSubscriber   │  Receives real-time pub/sub events
              └──────────────────────┘
```

### Redis Data Structures

Each chat room uses four Redis keys:

| Key pattern | Type | Stores |
|---|---|---|
| `chatroom:{roomId}` | Hash | Room metadata (`roomName`, `createdAt`) |
| `chatroom:{roomId}:participants` | Set | Participant names (unique) |
| `chatroom:{roomId}:messages` | List | JSON-serialised `ChatMessage` entries |
| `chatroom:{roomId}` | Pub/Sub channel | Real-time message broadcast |

---

## Prerequisites

- Java 21 or higher
- Maven (or use the included `mvnw.cmd` wrapper)
- A running Redis instance — **local** or **Upstash** (see below)

---

## Setup

### Option 1 — Upstash Redis (recommended, no local install needed)

I used [Upstash](https://upstash.com) as the Redis provider. Upstash gives you a managed Redis instance with a **visual dashboard** where you can watch data appear and disappear in real time — keys, hash fields, list entries, and set members — as you call the API. It also shows detailed read/write operation metrics, latency graphs, and per-command breakdowns, which makes it easy to understand exactly what's happening inside Redis without needing a local Redis CLI.

1. Create a free account at [console.upstash.com](https://console.upstash.com)
2. Create a new Redis database
3. From the **Details** tab, copy the **Redis URL**:
   ```
   rediss://default:<PASSWORD>@<HOST>:6379
   ```
4. Set the password as an environment variable:
   ```bash
   # Windows (PowerShell)
   $env:UPSTASH_REDIS_PASSWORD="<your-password>"

   # Windows (Command Prompt)
   set UPSTASH_REDIS_PASSWORD=<your-password>
   ```

`application.properties` is already configured for Upstash:
```properties
spring.data.redis.host=upward-antelope-97867.upstash.io
spring.data.redis.port=6379
spring.data.redis.password=${UPSTASH_REDIS_PASSWORD}
spring.data.redis.ssl.enabled=true
```

### Option 2 — Local Redis

If you have Redis running locally, update `application.properties`:
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
# remove the password and ssl lines
```

### Option 3 — Docker

```bash
docker run -d -p 6379:6379 --name redis redis:latest
```

Then use the local config from Option 2.

---

## Running the Application

```bash
# Using the Maven wrapper
./mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # Mac/Linux

# Or with the password env var inline (Windows PowerShell)
$env:UPSTASH_REDIS_PASSWORD="<password>"; ./mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8080**.

---

## API Reference

Base path: `/api/chatapp/chatrooms`

### Create a room
```bash
POST /api/chatapp/chatrooms
Content-Type: application/json

{"roomName": "general"}
```

### Join a room
```bash
POST /api/chatapp/chatrooms/{roomId}/join
Content-Type: application/json

{"participant": "alice"}
```

### Send a message
```bash
POST /api/chatapp/chatrooms/{roomId}/messages
Content-Type: application/json

{"participant": "alice", "message": "Hello everyone!"}
```

### Get chat history
```bash
GET /api/chatapp/chatrooms/{roomId}/messages?limit=50
```

### Delete a room
```bash
DELETE /api/chatapp/chatrooms/{roomId}
```

---

## Running Tests

```bash
./mvnw.cmd test
```

The test suite has **32 tests** covering:
- Controller layer (MockMvc + `@WebMvcTest`)
- Service layer (Mockito unit tests)
- Pub/Sub components (`MessageSubscriber`, `RedisMessagePublisher`)
- Validation and error handling
- Edge cases (null Redis responses, JSON serialization failures)

> **Java 26 note:** The `pom.xml` includes `-Dnet.bytebuddy.experimental=true` in the Surefire config so Mockito's inline mock maker works on Java 26, which Byte Buddy doesn't officially support yet.

---

## Project Structure

```
src/
├── main/java/com/chatapp/
│   ├── ChatApplication.java
│   ├── config/
│   │   └── RedisConfig.java          # RedisTemplate + pub/sub listener setup
│   ├── controller/
│   │   └── ChatController.java
│   ├── dto/
│   │   ├── request/                  # CreateRoomRequest, JoinRoomRequest, SendMessageRequest
│   │   └── response/                 # typed response DTOs + ErrorResponse
│   ├── exception/
│   │   ├── ChatRoomNotFoundException.java
│   │   ├── DuplicateChatRoomException.java
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   └── ChatMessage.java
│   ├── pubsub/
│   │   ├── MessagePublisher.java     # interface
│   │   ├── RedisMessagePublisher.java
│   │   └── MessageSubscriber.java    # MessageListener implementation
│   └── service/
│       ├── ChatRoomService.java      # interface
│       └── ChatRoomServiceImpl.java
└── test/java/com/chatapp/
    ├── controller/ChatControllerTest.java
    ├── service/ChatRoomServiceImplTest.java
    └── pubsub/
        ├── RedisMessagePublisherTest.java
        └── MessageSubscriberTest.java
```
