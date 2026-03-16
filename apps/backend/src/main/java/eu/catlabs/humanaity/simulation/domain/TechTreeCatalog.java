package eu.catlabs.humanaity.simulation.domain;

import java.util.List;
import java.util.Map;

public record TechTreeCatalog(
        String version,
        List<TechTreeNode> discoveries,
        List<TechTreeNode> inventions,
        List<TechTreeNode> applications,
        Map<String, TechTreeNode> nodesById
) {
}
