package com.caspar.agent.service;

import com.caspar.agent.model.AgentSession;
import com.caspar.agent.model.AgentSessionHistory;
import com.caspar.entity.AgentLogRecord;
import com.caspar.mapper.AgentHistoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentHistoryServiceTest {

    @Test
    void restoreSessionHistory_shouldRestorePendingSlotState() {
        AgentHistoryMapper mapper = mock(AgentHistoryMapper.class);
        AgentHistoryService service = new AgentHistoryService(mapper, new ObjectMapper());
        AgentSession session = session("session-1");
        AgentLogRecord log = log("NAVIGATION", "{\"destination\":\"图书馆\"}", "[]", null, 1);

        when(mapper.findSessionOwner("session-1")).thenReturn(2L);
        when(mapper.selectLatestSessionLogs(2L, "session-1", 20)).thenReturn(List.of(log));

        service.restoreSessionHistory(session, 2L, "session-1", 20);

        assertEquals("NAVIGATION", session.getIntent());
        assertEquals("图书馆", session.getSlots().get("destination"));
        assertEquals(1, session.getTurnCount());
    }

    @Test
    void restoreSessionHistory_shouldRestoreToolClarificationState() {
        AgentHistoryMapper mapper = mock(AgentHistoryMapper.class);
        AgentHistoryService service = new AgentHistoryService(mapper, new ObjectMapper());
        AgentSession session = session("session-2");
        AgentLogRecord log = log(
                "NAVIGATION",
                "{\"destination\":\"图书馆\"}",
                "[\"navigation_v2\"]",
                "{\"followUpType\":\"campus_clarification\"}",
                2
        );

        when(mapper.findSessionOwner("session-2")).thenReturn(2L);
        when(mapper.selectLatestSessionLogs(2L, "session-2", 20)).thenReturn(List.of(log));

        service.restoreSessionHistory(session, 2L, "session-2", 20);

        assertEquals("NAVIGATION", session.getIntent());
        assertEquals("图书馆", session.getSlots().get("destination"));
        assertEquals(2, session.getTurnCount());
    }

    @Test
    void restoreSessionHistory_shouldNotResumeCompletedBusinessTask() {
        AgentHistoryMapper mapper = mock(AgentHistoryMapper.class);
        AgentHistoryService service = new AgentHistoryService(mapper, new ObjectMapper());
        AgentSession session = session("session-3");
        AgentLogRecord log = log(
                "NAVIGATION",
                "{\"destination\":\"图书馆\"}",
                "[\"navigation_v2\"]",
                "{\"cards\":[]}",
                1
        );

        when(mapper.findSessionOwner("session-3")).thenReturn(2L);
        when(mapper.selectLatestSessionLogs(2L, "session-3", 20)).thenReturn(List.of(log));

        service.restoreSessionHistory(session, 2L, "session-3", 20);

        assertNull(session.getIntent());
        assertEquals(0, session.getTurnCount());
    }

    @Test
    void restoreSessionHistory_shouldNotResumeRejectedWriteConfirmation() {
        AgentHistoryMapper mapper = mock(AgentHistoryMapper.class);
        AgentHistoryService service = new AgentHistoryService(mapper, new ObjectMapper());
        AgentSession session = session("session-rejected");
        AgentLogRecord log = log(
                "SECONDHAND_PUBLISH",
                "{\"title\":\"台灯\",\"category\":\"生活\",\"price\":50}",
                "[]",
                "{\"followUpType\":null,\"confirmationStatus\":\"REJECTED\"}",
                2
        );

        when(mapper.findSessionOwner("session-rejected")).thenReturn(2L);
        when(mapper.selectLatestSessionLogs(2L, "session-rejected", 20)).thenReturn(List.of(log));

        service.restoreSessionHistory(session, 2L, "session-rejected", 20);

        assertNull(session.getIntent());
        assertEquals(0, session.getTurnCount());
    }

    @Test
    void sessionHistory_shouldRestoreOnlyLatestPendingConfirmationMetadata() {
        AgentHistoryMapper mapper = mock(AgentHistoryMapper.class);
        AgentHistoryService service = new AgentHistoryService(mapper, new ObjectMapper());
        AgentLogRecord log = log(
                "SECONDHAND_PUBLISH",
                "{\"title\":\"台灯\",\"category\":\"生活\",\"price\":50}",
                "[]",
                "{\"followUpType\":\"write_confirmation\",\"confirmationRequired\":true,"
                        + "\"confirmationId\":\"confirm-1\",\"confirmationStatus\":\"PENDING\","
                        + "\"confirmationPreview\":{\"action\":\"发布校园二手商品\"}}",
                1
        );

        when(mapper.findSessionOwner("session-history")).thenReturn(2L);
        when(mapper.selectSessionLogs(2L, "session-history", 200)).thenReturn(List.of(log));

        AgentSessionHistory history = service.getSessionHistory(2L, "session-history", 200);

        assertEquals(2, history.getMessages().size());
        assertTrue(history.getMessages().get(1).isConfirmationRequired());
        assertEquals("confirm-1", history.getMessages().get(1).getConfirmationId());
        assertEquals("发布校园二手商品",
                history.getMessages().get(1).getConfirmationPreview().get("action"));
        assertFalse(history.getMessages().get(0).isConfirmationRequired());
    }

    private AgentSession session(String sessionId) {
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setUserId(2L);
        return session;
    }

    private AgentLogRecord log(String intent, String slots, String toolsUsed, String responsePayload, int turn) {
        AgentLogRecord log = new AgentLogRecord();
        log.setIntent(intent);
        log.setSlots(slots);
        log.setToolsUsed(toolsUsed);
        log.setResponsePayload(responsePayload);
        log.setTurn(turn);
        log.setSuccess(true);
        log.setUserInput("测试输入");
        log.setReply("请继续补充信息");
        return log;
    }
}
