package com.caspar.agent.controller;

import com.caspar.agent.model.AgentRequest;
import com.caspar.agent.model.AgentResponse;
import com.caspar.agent.model.AgentSessionHistory;
import com.caspar.agent.model.AgentSessionSummary;
import com.caspar.agent.service.AgentHistoryService;
import com.caspar.agent.service.AgentOrchestrator;
import com.caspar.agent.session.AgentSessionManager;
import com.caspar.common.Result;
import com.caspar.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 对话接口：HTTP 层入口，负责鉴权与参数校验，业务编排委托给 AgentOrchestrator。
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final AgentHistoryService agentHistoryService;
    private final AgentSessionManager sessionManager;

    /**
     * POST /api/agent/chat
     * 接收用户自然语言，返回 Agent 回复。
     */
    @PostMapping("/chat")
    public Result<AgentResponse> chat(@RequestBody AgentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        if (request.getSessionId() != null && !agentHistoryService.isSessionAccessible(userId, request.getSessionId())) {
            return Result.forbidden();
        }

        AgentResponse response = orchestrator.process(request, userId);
        return Result.success(response);
    }

    @GetMapping("/sessions")
    public Result<List<AgentSessionSummary>> sessions(@RequestParam(defaultValue = "30") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        return Result.success(agentHistoryService.listSessions(userId, limit));
    }

    @GetMapping("/session/{sessionId}/history")
    public Result<AgentSessionHistory> sessionHistory(@PathVariable String sessionId,
                                                      @RequestParam(defaultValue = "200") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            return Result.success(agentHistoryService.getSessionHistory(userId, sessionId, limit));
        } catch (SecurityException e) {
            return Result.forbidden();
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * DELETE /api/agent/session/{sessionId}
     * 删除指定会话历史，并清理活跃会话缓存。
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized();
        }
        try {
            var session = sessionManager.getSession(sessionId);
            if (session != null && !userId.equals(session.getUserId())) {
                return Result.forbidden();
            }
            agentHistoryService.deleteSession(userId, sessionId);
            orchestrator.clearCheckpoint(sessionId);
            sessionManager.delete(sessionId);
            return Result.success();
        } catch (SecurityException e) {
            return Result.forbidden();
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }
}
