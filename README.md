# Kafka Control System Dashboard

A **Spring Kafka** producer-consumer demo with an industrial-style control dashboard UI.

![Dashboard](https://img.shields.io/badge/UI-Industrial%20Dashboard-00FF88?style=flat-square&labelColor=0a0a0a)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square)
![Spring Kafka](https://img.shields.io/badge/Spring%20Kafka-3.1.4-brightgreen?style=flat-square)

## Features

- **Producer** — sends `Foo1` JSON objects to `topic1` via REST
- **Consumer** — `fooGroup` listener deserializes to `Foo2` using `JsonMessageConverter`
- **Dead Letter Topic** — failed messages retried 2× then sent to `topic1-dlt`
- **Dashboard UI** — real-time control panel served from `localhost:8080`
  - 4 stat panels: Produced / Consumed / Errors / DLT
  - Producer control with `▶ SEND` and `✕ SEND FAIL`
  - Auto-polling event log table (3s interval)
  - Live terminal log + system info panel
  - Broker status indicator

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.5 + Spring Kafka |
| Messaging | Apache Kafka (topic1, topic1-dlt) |
| Frontend | HTML + Vanilla CSS + Vanilla JS |
| Font | JetBrains Mono |
| Build | Maven 3.9 |
| Java | 17+ |

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- Apache Kafka running on `localhost:9092`

### Quick Start

```bash
# 1. Start Kafka (Docker)
docker run -d -p 9092:9092 --name kafka apache/kafka:latest

# 2. Run the app
mvn spring-boot:run

# 3. Open the dashboard
open http://localhost:8080
```

### Send a message via curl

```bash
# Normal message
curl -X POST http://localhost:8080/send/foo/bar

# Trigger failure + DLT flow
curl -X POST http://localhost:8080/send/foo/fail
```

## REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/send/foo/{payload}` | Produce message to topic1 |
| `GET` | `/api/stats` | Produced / consumed / error / DLT counts |
| `GET` | `/api/events` | Last 200 events (newest first) |
| `GET` | `/api/health` | Broker health check |

## Architecture

```
Browser → POST /send/foo/{msg}
              │
              ▼
         Controller → KafkaTemplate → topic1
                                          │
                              fooGroup Listener
                                  │        │
                               OK ▼    FAIL ▼
                           MessageStore  Retry × 2
                                            │
                                        topic1-dlt
                                            │
                                     dltGroup Listener
                                            │
                                       MessageStore
```

## Deployment

### Docker

```bash
docker build -t kafka-dashboard .
docker run -p 8080:8080 -e SPRING_KAFKA_BOOTSTRAP_SERVERS=your-broker:9092 kafka-dashboard
```

### Render.com

1. Fork this repo
2. Create a new **Web Service** on [render.com](https://render.com)
3. Connect your GitHub repo
4. Render auto-detects `render.yaml` and deploys

## License

Apache License 2.0
