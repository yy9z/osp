package com.caspar.agent.registry;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolCatalogTest {

    private final AgentToolCatalog catalog = new AgentToolCatalog();

    @Test
    void definitionsShouldHaveUniqueIntentAndToolNames() {
        assertEquals(11, catalog.getDefinitions().size());
        assertEquals(11, new HashSet<>(catalog.getDefinitions().stream()
                .map(AgentToolDefinition::intent).toList()).size());
        assertEquals(11, new HashSet<>(catalog.getDefinitions().stream()
                .map(AgentToolDefinition::name).toList()).size());
    }

    @Test
    void catalogShouldExposePlanningAndSafetyMetadata() {
        AgentToolDefinition publish = catalog.findByIntent("SECONDHAND_PUBLISH").orElseThrow();
        assertEquals("secondhand_publish", publish.name());
        assertEquals(java.util.List.of("title", "category", "price"), publish.requiredSlots());
        assertFalse(publish.readOnly());

        AgentToolDefinition search = catalog.findByName("secondhand_search").orElseThrow();
        assertTrue(search.readOnly());
        assertEquals("请输入价格（元）：", catalog.getSlotQuestion("price"));

        AgentToolDefinition knowledge = catalog.findByIntent("CAMPUS_KNOWLEDGE").orElseThrow();
        assertEquals("campus_knowledge_query", knowledge.name());
        assertEquals(java.util.List.of("question"), knowledge.requiredSlots());
        assertTrue(knowledge.readOnly());
    }
}
