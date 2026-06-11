# Project Hooks & Context

## Overview
This file maintains project context and hooks for the Campus One-Stop Platform.

## Project Context

### Basic Information
- **Project Name**: 高校校园一站式平台 (Campus One-Stop Platform)
- **Backend**: Spring Boot 3.x (Java 17) + MyBatis + MySQL
- **Frontend**: Vue 3 + Element Plus + Leaflet + TypeScript

### Running Services
| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Frontend | http://localhost:5173/5174 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

### Test Account
- Username: `testuser123`
- Password: `123456`

### Database
- Host: `localhost:3306`
- Password: `12345678`
- Database: `campus_platform`

---

## Key Technical Decisions

### 1. Navigation Module (校园导航)
- **Map Provider**: Leaflet (OpenStreetMap)
- **Coordinates**: 中国科学技术大学 (USTC)
  - Center: 31.8384°N, 117.2167°E
  - Zoom Level: 14-18
- **Routing**: Dijkstra Algorithm with campus path nodes
- **POI Categories**: TEACHING, DINING, LIBRARY, DORMITORY, SPORTS, ADMIN, SCENIC, ENTRANCE

### 2. Security
- JWT Token Authentication
- Spring Security with custom filters
- Public endpoints: login, register, list views

### 3. Known Fixes
1. CampusMap.vue route params: `startLat/startLng` → `fromLat/fromLng/toLat/toLng`
2. SecurityConfig: Added `/api/dormitory/list` to public endpoints
3. MyBatis XML: Used `<![CDATA[<=]]>` for Haversine formula

---

## API Endpoints Summary

### User Module
- POST `/api/user/login`
- POST `/api/user/register`
- GET `/api/user/info`

### Secondhand Trading
- GET `/api/secondhand/list`
- POST `/api/secondhand/publish`

### Lost & Found
- GET `/api/lostfound/list`
- POST `/api/lostfound/publish`

### Campus Navigation
- GET `/api/navigation/pois`
- GET `/api/navigation/nearby?lat=&lng=&radius=`
- POST `/api/navigation/route`
- GET `/api/navigation/regions`

### Dormitory
- GET `/api/dormitory/list`
- POST `/api/dormitory/repair`

---

## Commands

### Start Backend
```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU
./mvnw spring-boot:run
```

### Start Frontend
```bash
cd /Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend
npm run dev
```

### Restart Services
```bash
lsof -ti:8080 | xargs kill -9
lsof -ti:5173 | xargs kill -9
```
