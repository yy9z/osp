# Project Context (项目上下文记忆)

## Role
Maintains project context and history for better development continuity.

## When to Invoke
- When starting new development tasks
- When you need to recall project architecture
- When onboarding to an existing project
- When you need to check project status

## Project Information

### Project Name
高校校园一站式平台 (Campus One-Stop Platform) - 中科大USTC版

### Project Location
- Backend: `/Users/caspar/Documents/IdeaProjects/NewOSPFU`
- Frontend: `/Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend`

### Technology Stack
**Backend:**
- Spring Boot 3.x (Java 17)
- MyBatis (not MyBatis-Plus)
- MySQL
- JWT Authentication
- Spring Security
- Aliyun OSS SDK

**Frontend:**
- Vue 3
- Element Plus
- Leaflet Maps (校园导航)
- TypeScript
- Vite

### Database
- Host: localhost:3306
- Password: 12345678
- Database: campus_platform

### Running Services
- Backend: http://localhost:8080
- Frontend: http://localhost:5173
- Swagger: http://localhost:8080/swagger-ui.html

### Test Account
- Username: testuser123
- Password: 123456

### Core Modules
1. **User Module** - Login, Register, JWT Auth
2. **Secondhand Trading** - Publish, List, Buy/Sell
3. **Lost & Found** - Publish, Claim
4. **Messaging** - Conversations, History
5. **Dormitory** - Rooms, Repairs
6. **Campus Navigation** - POI, Maps, Dijkstra Routing + Amap Walking API

### Image Resources
已下载到 `/campus-frontend/public/images/`:
- `ustc-logo.png` - 中科大官方校徽
- `campus-bg.jpg` - 大学校园背景
- `library-bg.jpg` - 图书馆背景
- `building-bg.jpg` - 科技楼背景

### Key Files
- Backend API: `/Users/caspar/Documents/IdeaProjects/NewOSPFU/docs/API.md`
- Frontend API: `/Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend/docs/API.md`
- CSS Theme: `/Users/caspar/Documents/IdeaProjects/NewOSPFU/campus-frontend/src/style.css`

### Important Fixes Applied
1. **Navigation Route Fix** - CampusMap.vue parameters: startLat/startLng → fromLat/fromLng/toLat/toLng
2. **SecurityConfig** - Added /api/dormitory/list public access
3. **Navigation POI Data** - China University of Science and Technology (USTC)
   - Center: 31.8384, 117.2167
   - 16 POIs: Teaching, Dining, Library, Dormitory, Sports, Admin, Scenic, Entrance
   - Path nodes and edges for Dijkstra routing
4. **Amap Walking Integration** - Backend proxy to Amap Web Services API
   - API Key: a2efeb47180324d3b18c304ae7aba417
   - Full path coordinate parsing from step.polyline
   - Digital signature support
5. **Aliyun OSS** - Image storage integration
   - Endpoint: https://oss-cn-beijing.aliyuncs.com
   - Bucket: caspar-java-tlias-pratice
   - Upload API: POST /api/file/upload
6. **UI Brand Upgrade**
   - USTC Blue (#004191) theme
   - Glassmorphism effects
   - Bento Grid dashboard layout
   - Login page with campus background
7. **Dashboard Fixes**
   - Replaced non-existent Bus icon with Van
   - Added Badge component for message notifications

### Design System
- **Core Color**: #004191 (科大蓝)
- **Card Spec**: 16px border-radius, 1px solid rgba(0,0,0,0.05) border
- **Glassmorphism**: backdrop-filter: blur(8px)
- **Responsive**: 1200px/768px/480px breakpoints
