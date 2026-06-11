---
name: "changelog-recorder"
description: "Records project updates to PROJECT_ARCHITECTURE.md changelog. Invoke IMMEDIATELY after completing any feature, bugfix, or code change to document the update."
---

# Changelog Recorder

This skill automatically records project updates to the changelog section of PROJECT_ARCHITECTURE.md.

## When to Invoke

**CRITICAL: Invoke this skill IMMEDIATELY after:**
- Completing any new feature implementation
- Fixing any bug
- Making any code change that affects functionality
- Updating API endpoints
- Modifying database schema
- Adding new dependencies

## What to Record

Each changelog entry should include:
1. **Date**: Current date in YYYY-MM-DD format
2. **Description**: Clear, concise description of what was changed/added/fixed

## Changelog Location

File: `/Users/caspar/Documents/IdeaProjects/NewOSPFU/docs/PROJECT_ARCHITECTURE.md`

Section: "八、更新日志" (Section 8: Changelog)

## Format

```markdown
| 日期 | 更新内容 |
|------|---------|
| YYYY-MM-DD | 描述更新的具体功能或修复内容 |
```

## Example Entries

```markdown
| 日期 | 更新内容 |
|------|---------|
| 2026-02-26 | 修复消息未读数统计问题（SQL查询优化） |
| 2026-02-26 | 添加二手交易URL状态同步功能 |
| 2026-02-26 | 优化收藏功能防重复点击 |
| 2026-02-26 | 添加我的收藏分页功能 |
| 2026-02-27 | 新增用户头像上传功能 |
| 2026-02-27 | 修复宿舍报修状态更新异常 |
```

## Instructions

1. Read the current PROJECT_ARCHITECTURE.md file
2. Locate the "八、更新日志" section
3. Add a new row with today's date and the update description
4. Keep entries sorted by date (newest first)
5. Be specific about what was changed - mention:
   - Module name (e.g., "消息中心", "二手交易")
   - Type of change (新增/修复/优化/重构)
   - Brief technical detail if relevant

## Language

Use Chinese (Simplified) for changelog entries to match the existing format.
