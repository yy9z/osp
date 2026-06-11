package com.caspar.mapper;

import com.caspar.agent.model.AgentSessionSummary;
import com.caspar.entity.AgentLogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentHistoryMapper {

    Long findSessionOwner(@Param("sessionId") String sessionId);

    int upsertSession(@Param("id") String id,
                      @Param("userId") Long userId,
                      @Param("title") String title,
                      @Param("lastIntent") String lastIntent);

    int incrementSessionStats(@Param("id") String id,
                              @Param("delta") int delta,
                              @Param("lastIntent") String lastIntent);

    int insertLog(AgentLogRecord logRecord);

    int insertLogLegacy(AgentLogRecord logRecord);

    List<AgentSessionSummary> selectSessionSummaries(@Param("userId") Long userId,
                                                     @Param("limit") Integer limit);

    List<AgentLogRecord> selectSessionLogs(@Param("userId") Long userId,
                                           @Param("sessionId") String sessionId,
                                           @Param("limit") Integer limit);

    List<AgentLogRecord> selectSessionLogsLegacy(@Param("userId") Long userId,
                                                 @Param("sessionId") String sessionId,
                                                 @Param("limit") Integer limit);

    int deleteSessionLogs(@Param("userId") Long userId,
                          @Param("sessionId") String sessionId);

    int deleteSession(@Param("userId") Long userId,
                      @Param("sessionId") String sessionId);
}
