package com.caspar.agent.framework;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mirrors checkpoints in memory and temporarily bypasses Redis after failures.
 */
@Slf4j
public class ResilientCheckpointSaver implements BaseCheckpointSaver {

    private static final long RETRY_AFTER_MS = 30_000L;

    private final BaseCheckpointSaver primary;
    private final BaseCheckpointSaver fallback;
    private final AtomicLong primaryUnavailableUntil = new AtomicLong(0L);

    public ResilientCheckpointSaver(BaseCheckpointSaver primary, BaseCheckpointSaver fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        if (canUsePrimary()) {
            try {
                Collection<Checkpoint> checkpoints = primary.list(config);
                primaryUnavailableUntil.set(0L);
                if (!checkpoints.isEmpty()) {
                    return checkpoints;
                }
            } catch (Exception e) {
                markPrimaryUnavailable(e);
            }
        }
        return fallback.list(config);
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        if (canUsePrimary()) {
            try {
                Optional<Checkpoint> checkpoint = primary.get(config);
                primaryUnavailableUntil.set(0L);
                if (checkpoint.isPresent()) {
                    return checkpoint;
                }
            } catch (Exception e) {
                markPrimaryUnavailable(e);
            }
        }
        return fallback.get(config);
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        RunnableConfig fallbackConfig = fallback.put(config, checkpoint);
        if (canUsePrimary()) {
            try {
                RunnableConfig primaryConfig = primary.put(config, checkpoint);
                primaryUnavailableUntil.set(0L);
                return primaryConfig;
            } catch (Exception e) {
                markPrimaryUnavailable(e);
            }
        }
        return fallbackConfig;
    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        Tag fallbackTag = fallback.release(config);
        if (canUsePrimary()) {
            try {
                Tag primaryTag = primary.release(config);
                primaryUnavailableUntil.set(0L);
                return primaryTag;
            } catch (Exception e) {
                markPrimaryUnavailable(e);
            }
        }
        return fallbackTag;
    }

    private boolean canUsePrimary() {
        return System.currentTimeMillis() >= primaryUnavailableUntil.get();
    }

    private void markPrimaryUnavailable(Exception e) {
        primaryUnavailableUntil.set(System.currentTimeMillis() + RETRY_AFTER_MS);
        log.warn("Graph Redis checkpoint unavailable, using memory fallback: {}", e.getMessage());
    }
}
