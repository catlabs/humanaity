package eu.catlabs.humanaity.simulation.domain;

import java.util.List;
import java.util.Map;

public record TechTreeNode(
        String id,
        TechTreeNodeType nodeType,
        List<String> prerequisites,
        String triggerEventType,
        Map<String, String> metadata
) {
}
