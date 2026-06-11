# 校园导航模块升级说明

## 升级概述

本次升级将导航模块从"简单路线规划工具"升级为"Agent驱动的智能导航系统"，实现了：

> **Agent 负责理解校园出行任务，高德地图负责执行空间规划，页面负责承接任务状态与连续决策。**

---

## 核心升级内容

### 1. 槽位系统扩展（3类 → 6类）

#### A. 目的地槽位
- **显式地点名**：东区学生食堂、图书馆、教三402
- **语义目标类型**：
  - `nearest_canteen` - 最近食堂
  - `nearest_print_shop` - 最近打印店
  - `nearest_study` - 最近自习室

#### B. 起点槽位
优先级：用户明确输入 > Agent会话上下文 > 用户当前位置 > 用户默认地点

#### C. 出行方式槽位
- `walking` - 步行
- `cycling` - 骑行
- `accessible` - 无障碍（少台阶）

#### D. 偏好槽位（核心创新）
- `fastest` - 最快路线
- `shortest` - 最短路线
- `safeNight` - 夜间安全（主干道、照明好）
- `avoidCrowd` - 避开下课高峰
- `luggageFriendly` - 适合搬行李
- `viaCanteen` - 途经食堂
- `viaPrintShop` - 途经打印店

#### E. 任务场景槽位
- `class_commute` - 上课赶路
- `dining` - 去食堂
- `express` - 取快递
- `study` - 去图书馆/自习室
- `medical` - 去校医院
- `return_dorm` - 回宿舍
- `campus_tour` - 新生导览
- `daily_convenience` - 日常便利

#### F. 时间/上下文槽位
- `now` / `night` / `after_class` / `before_exam`
- `urgencyMinutes` - 紧急程度（还有几分钟）

---

### 2. 校园地点词典（语义识别增强）

**实现类**：`CampusPoiDictionary.java`

#### 地点别名映射
```
东食堂 → 东区学生食堂
教三 → 第三教学楼
菜鸟 → 校园快递中心
```

#### 语义类型识别
```
"去吃饭" → canteen
"打印论文" → print_shop
"自习" → study
```

#### 最近需求识别
```
"最近打印店" → nearest_print_shop
"附近食堂" → nearest_canteen
```

---

### 3. 场景智能分类

**实现类**：`NavigationSceneClassifier.java`

#### 场景识别规则

**上课赶路模式**
- 触发词：上课、赶课、去上课
- 自动偏好：`fastest`
- 特殊处理：显示迟到风险提示

**夜间安全模式**
- 触发条件：晚上8点后 或 用户提到"晚上"
- 自动偏好：`safeNight`
- 路线策略：优先主干道、照明好的路径

**下课高峰模式**
- 触发时间：整点前后10分钟（8:00-18:00）
- 自动偏好：`avoidCrowd`
- 路线策略：避开教学楼主干道

**搬行李模式**
- 触发词：搬行李、拿东西、不要台阶
- 自动偏好：`luggageFriendly`
- 路线策略：少台阶、少窄路、优先大道

---

### 4. 结构化导航响应（四段式）

**实现类**：`NavigationTaskResponse.java`

```json
{
  "intent": "NAVIGATION",
  "taskType": "class_commute",

  "understanding": {
    "origin": { "name": "当前位置", "lat": 30.1, "lng": 120.1, "source": "device_location" },
    "destination": { "name": "东区学生食堂", "lat": 30.2, "lng": 120.2, "source": "campus_poi_dict" },
    "mode": "walking",
    "preferences": ["fastest", "safeNight"],
    "constraints": ["avoid_construction"],
    "timeContext": "night",
    "urgencyMinutes": 8
  },

  "decision": {
    "summary": "已为您规划从当前位置前往东区学生食堂的步行路线，优先选择校内主干道，夜间照明更好。",
    "reasoning": [
      "检测到夜间场景，优先推荐主干道",
      "目的地识别为东区学生食堂",
      "当前位置作为默认起点"
    ],
    "campusHint": "东校区"
  },

  "route": {
    "ready": true,
    "distance": 820,
    "duration": 540,
    "steps": [...],
    "path": [...]
  },

  "actions": [
    { "type": "replan", "label": "改成骑行路线", "payload": { "mode": "cycling" } },
    { "type": "nearby", "label": "查附近打印店", "payload": { "category": "print_shop" } },
    { "type": "return_route", "label": "返回宿舍路线" }
  ]
}
```

---

## 创新场景示例

### 1. 上课赶路模式
**输入**：
```
"我还有8分钟上课，去教三402怎么走"
"从宿舍去明德楼，快点"
```

**Agent处理**：
- 识别 `class_commute` 场景
- 默认偏好 `fastest`
- 如果迟到风险高，突出显示：
  - ⚠️ 预计9分钟，存在迟到风险
  - 建议：是否切换骑行/更近入口

---

### 2. 最近服务点模式
**输入**：
```
"最近打印店"
"附近快递点"
"哪家食堂最近"
```

**Agent处理**：
- 不是先让用户选地点
- 而是先根据类别找最优候选，再规划路线
- 自动按距离排序

---

### 3. 夜间安全路线
**输入**：
```
"晚上回宿舍怎么走安全一点"
```

**Agent处理**：
- `safeNight=true`
- 结合校园主路、路灯覆盖、保安亭周边路径权重
- 返回"更安全但略远"的路线
- 显示：🌙 已启用夜间安全模式，优先选择主干道

---

### 4. 搬行李模式
**输入**：
```
"搬行李去宿舍怎么走方便"
"不要走台阶"
```

**Agent处理**：
- `luggageFriendly=true`
- 通过POI/路段标签模拟：
  - 少台阶
  - 少窄路
  - 优先大道

---

### 5. 语义化导航
**输入**：
```
"去最近吃饭的地方"
"去一个人少点的自习室"
"去打印论文方便的地方"
```

**Agent处理**：
- 从POI搜索升级为语义导航
- 理解"吃饭" → 食堂
- 理解"打印论文" → 打印店
- 自动匹配最近候选

---

## 技术架构

### 后端核心类

```
agent/
├── model/
│   ├── NavigationSlots.java              # 6类槽位模型
│   └── NavigationTaskResponse.java       # 四段式响应
├── service/
│   ├── CampusPoiDictionary.java          # 校园地点词典
│   ├── NavigationSceneClassifier.java    # 场景分类器
│   ├── NavigationSlotExtractor.java      # 槽位提取器
│   └── NavigationActionGenerator.java    # 推荐动作生成器
└── tool/
    └── NavigationToolV2.java             # 增强版导航工具
```

### 前端核心组件

```
components/agent/cards/
└── RouteCard.vue                         # 导航卡片（支持场景/偏好标签）

views/
└── Navigation.vue                        # 导航页（支持结构化任务显示）

stores/
└── navigation.js                         # 导航状态管理
```

---

## 使用示例

### 基础导航
```
用户：去图书馆
Agent：已为您识别目的地【中国科学技术大学图书馆】，进入导航页后可继续使用当前位置完成路线规划
```

### 场景识别
```
用户：还有10分钟上课，去教三怎么走
Agent：检测到上课赶路场景，时间紧迫。已为您规划从当前位置前往第三教学楼的最快路线，全程约0.8公里，预计9分钟。
⚠️ 时间较紧，建议切换骑行路线（预计5分钟）
```

### 最近服务点
```
用户：最近打印店
Agent：已为您找到最近的打印店：校园打印社（距您约200米），已规划步行路线，预计3分钟到达。
```

### 夜间安全
```
用户：晚上回宿舍怎么走
Agent：检测到夜间场景。已为您规划从当前位置返回宿舍的安全路线，优先选择校内主干道，照明较好，全程约1.2公里，预计15分钟。
🌙 已启用夜间安全模式
```

---

## 前端UI增强

### RouteCard卡片
- ✅ 显示场景标签（上课赶路、就餐、取快递等）
- ✅ 显示偏好标签（最快路线、夜间安全等）
- ✅ 显示决策依据

### Navigation页面
- ✅ 顶部显示当前任务场景
- ✅ 显示激活的偏好标签
- ✅ 右侧面板显示Agent理解和决策
- ✅ 快捷场景标签（去图书馆、去食堂、回宿舍、取快递、最近打印店、夜间回宿舍）
- ✅ 推荐后续动作（改成骑行、查附近设施、返回路线）

---

## 配置说明

### 启用新版导航工具

在 `PlannerService.java` 中已自动切换：
```java
"NAVIGATION" -> List.of("navigation_v2")  // 使用增强版
```

### 扩展地点词典

编辑 `CampusPoiDictionary.java`：
```java
addPoi("东区学生食堂", "东区学生食堂", "canteen", "东食堂", "东区食堂", "东边食堂");
```

### 添加新场景

编辑 `NavigationSceneClassifier.java`：
```java
if (input.contains("新场景关键词")) {
    return "new_scene_type";
}
```

---

## 测试用例

### 基础功能
- [x] 去图书馆
- [x] 从宿舍去东区学生食堂
- [x] 最近打印店

### 场景识别
- [x] 还有8分钟上课，去教三怎么走
- [x] 晚上回宿舍安全路线
- [x] 搬行李去宿舍怎么走方便

### 语义识别
- [x] 去最近吃饭的地方
- [x] 去打印论文
- [x] 附近哪里可以自习

### 偏好处理
- [x] 去食堂，快点
- [x] 回宿舍，不要走台阶
- [x] 去图书馆，避开人多的路

---

## 后续优化方向

### 短期（1-2周）
1. ✅ 完善校园地点词典（补充更多别名）
2. ⬜ 添加实时人流数据（可选）
3. ⬜ 支持多目标点导航（先去A再去B）

### 中期（1个月）
1. ⬜ 与失物招领、二手交易联动
2. ⬜ 常用路线学习和推荐
3. ⬜ 新生导览模式完善

### 长期（毕业设计展示）
1. ⬜ 访客参观模式
2. ⬜ 路线偏好个性化学习
3. ⬜ 校园热力图可视化

---

## 论文写作要点

### 创新点
1. **六类槽位系统** - 从简单导航升级为任务理解
2. **校园语义词典** - 地点别名识别和语义类型映射
3. **场景智能分类** - 8种校园出行场景自动识别
4. **结构化响应** - Understanding/Decision/Route/Actions四段式
5. **偏好推断** - 基于场景和时间的智能偏好推荐

### 技术亮点
- Agent + 高德API工具调用
- 校园域增强的NLP处理
- 结构化任务状态传递
- 前后端协同的连续决策

### 实用价值
- 比普通地图更懂校园场景
- 从"搜地点"升级为"解决需求"
- 夜间安全、上课赶路等真实痛点
- 与其他校园服务模块联动

---

## 联系方式

如有问题，请查看代码注释或联系开发者。

**核心文件**：
- 后端：`NavigationToolV2.java`
- 前端：`Navigation.vue`
- 文档：本文件
