# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OSP (高校校园一站式平台) — a university campus all-in-one platform. Spring Boot 3.5 backend + Vue 3 frontend SPA. The platform includes an AI agent (campus assistant) that handles natural language queries for navigation, dormitory, lost-and-found, secondhand trading, and messaging.

## Build & Run

### Backend (Java 21, Maven)
```bash
./mvnw spring-boot:run
```
Runs on http://localhost:8080. Requires MySQL on localhost:3306 with database `campus_platform`, and Redis on localhost:6379.

Key environment variables (set before running):
- `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` — MySQL password
- `JWT_SECRET` — JWT signing key
- `SPRING_AI_OPENAI_API_KEY` — SiliconFlow API key (OpenAI-compatible)
- `SPRING_AI_OPENAI_BASE_URL` — defaults to `https://api.siliconflow.cn`
- `LLM_MODEL` — defaults to `Qwen/Qwen2.5-72B-Instruct`
- `AGENT_NATIVE_TOOL_CALLING_ENABLED` — enables the read-only native Tool Calling pilot; defaults to `false`
- `AGENT_GRAPH_REDIS_CHECKPOINT_ENABLED` — persists Graph checkpoints in Redis; defaults to `true`
- `AMAP_WEB_KEY` — Amap (高德) web API key
- `ALIYUN_OSS_ACCESS_KEY_ID` / `ALIYUN_OSS_ACCESS_KEY_SECRET` — Aliyun OSS credentials

### Frontend (Node.js, Vite 7)
```bash
cd campus-frontend
npm install
npm run dev
```
Runs on http://localhost:5173. Vite proxies `/api` requests to the backend at :8080.

### Build for production
```bash
# Backend
./mvnw clean package -DskipTests

# Frontend
cd campus-frontend && npm run build
```

## Architecture

### Backend (`src/main/java/com/caspar/`)

Standard Spring Boot layered architecture with an additional `agent` module:

- `controller/` — REST controllers for each domain (User, Dormitory, LostFound, Secondhand, Message, Navigation, File, Home). Admin controllers are separate (`*AdminController`).
- `service/` + `service/impl/` — Business logic. Interfaces in `service/`, implementations in `service/impl/`.
- `mapper/` — MyBatis-Plus mappers. XML mappings in `src/main/resources/mapper/`.
- `entity/` — JPA/MyBatis entities. DTOs in `entity/dto/`. Enums in `entity/enums/`.
- `config/` — Spring Security (JWT + rate limiting), Redis, Caffeine cache, Aliyun OSS, Amap, MyBatis, OpenAPI configs.
- `util/` — JWT, Aliyun OSS, Amap route, Dijkstra pathfinding, coordinate conversion, geofencing.
- `common/` — `Result<T>` (unified API response wrapper), `PageResult<T>`, `UserRole` enum.

### Agent Module (`agent/`)

The AI campus assistant. Spring AI Alibaba Graph owns the runtime pipeline: Route → Plan → Slot Check → Write Confirmation/Tool Execution → Follow-up/Response.

- `controller/AgentController` — HTTP entry point (`POST /api/agent/chat`)
- `framework/CampusAgentGraph` — Compiled state graph with independently testable workflow nodes and conditional edges
- `framework/CampusAgentExecution` — Immutable request-scoped input carried by the graph
- `framework/AgentGraphCheckpointConfiguration` — Redis-backed Graph checkpoints with an in-memory fallback
- `service/AgentWriteConfirmationService` — Safe previews and approval/rejection handling for write tools
- `service/AgentOrchestrator` — Thin adapter for session preparation, history restoration, graph invocation, and top-level failures
- `service/IntentService` — LLM-based intent classification
- `service/SlotFillingService` — Extracts required parameters from user input
- `service/PlannerService` — Selects which tools to invoke based on intent + slots
- `service/ToolExecutorService` — Executes tools, collects results
- `service/ResponseGeneratorService` — LLM generates natural language reply from tool results
- `tool/AgentTool` — Interface all tools implement (`getName()`, `execute(ToolArgs)`)
- `tool/` — Concrete tools: NavigationToolV2, SecondHandTool, LostFoundTool, DormQueryTool, DormRepairTool, MessageTool, etc.
- `session/AgentSessionManager` — Redis-backed session management with conversation history
- `llm/LlmClient` — Spring AI OpenAI client (used with SiliconFlow's OpenAI-compatible API)
- `mcp/` — Optional MCP (Model Context Protocol) tool calling support

Navigation is the most complex agent feature, with multi-waypoint routing, segment analysis, and Amap API integration.

### Frontend (`campus-frontend/src/`)

Vue 3 + Vite + Element Plus + Pinia + Vue Router.

- `api/` — Axios-based API modules per domain. `request.js` is the shared axios instance with JWT auth interceptor.
- `views/` — Page components. `admin/` for admin pages, `dormitory/` for dorm pages, `user/` for profile.
- `components/` — Shared components. `agent/` contains chat UI (ChatWindow, AgentInputBar, ContextPanel) and result cards (RouteCard, ProductCard, etc.).
- `stores/` — Pinia stores (user, agent, message, navigation).
- `router/index.js` — Route definitions with role-based guards (`meta.role`).
- `layouts/MainLayout.vue` — Authenticated app shell.
- `utils/map.ts` — Leaflet map utilities for campus navigation.

Auth: JWT token stored in `sessionStorage`. Roles: STUDENT, TEACHER, ADMIN, DORM_MANAGER.

### API Convention

All backend endpoints return `Result<T>` with structure `{ code, message, data }`. Code 200 = success. The frontend axios interceptor in `request.js` unwraps this automatically.

All API paths are under `/api/`. Frontend proxies to backend in dev via Vite config.

## Key External Services

- **SiliconFlow** — LLM provider (OpenAI-compatible API) for the agent module
- **Amap (高德地图)** — Map tiles (frontend) and route planning API (backend)
- **Aliyun OSS** — File/image storage
- **Redis** — Agent session management and durable Graph checkpoints
- **MySQL** — Primary database (`campus_platform`)
