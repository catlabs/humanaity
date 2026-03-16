package eu.catlabs.humanaity.simulation.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.simulation.domain.TechTreeCatalog;
import eu.catlabs.humanaity.simulation.domain.TechTreeNode;
import eu.catlabs.humanaity.simulation.domain.TechTreeNodeType;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TechTreeCatalogLoader {

    private final ObjectMapper objectMapper;
    private TechTreeCatalog catalog;

    public TechTreeCatalogLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initialize() {
        this.catalog = load(new ClassPathResource("tech-tree.json"));
    }

    public TechTreeCatalog getCatalog() {
        if (catalog == null) {
            throw new IllegalStateException("Tech tree catalog has not been initialized");
        }
        return catalog;
    }

    TechTreeCatalog load(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            RawTechTree raw = objectMapper.readValue(inputStream, RawTechTree.class);
            return validateAndConvert(raw);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to load tech tree config from " + resource.getDescription(), ioException);
        }
    }

    private TechTreeCatalog validateAndConvert(RawTechTree raw) {
        if (raw == null) {
            throw new IllegalStateException("Tech tree config is empty");
        }
        if (isBlank(raw.version)) {
            throw new IllegalStateException("Tech tree config must include a non-empty version");
        }

        List<RawNode> discoveries = nullSafe(raw.discoveries);
        List<RawNode> inventions = nullSafe(raw.inventions);
        List<RawNode> applications = nullSafe(raw.applications);
        if (discoveries.isEmpty() || inventions.isEmpty() || applications.isEmpty()) {
            throw new IllegalStateException("Tech tree config must include at least one discovery, invention, and application");
        }

        Map<String, TechTreeNode> nodesById = new LinkedHashMap<>();
        List<TechTreeNode> discoveryNodes = convertNodes(discoveries, TechTreeNodeType.DISCOVERY, nodesById);
        List<TechTreeNode> inventionNodes = convertNodes(inventions, TechTreeNodeType.INVENTION, nodesById);
        List<TechTreeNode> applicationNodes = convertNodes(applications, TechTreeNodeType.APPLICATION, nodesById);

        for (TechTreeNode node : nodesById.values()) {
            for (String prerequisiteId : node.prerequisites()) {
                if (!nodesById.containsKey(prerequisiteId)) {
                    throw new IllegalStateException("Tech tree node " + node.id()
                            + " references missing prerequisite " + prerequisiteId);
                }
            }
        }

        return new TechTreeCatalog(
                raw.version,
                discoveryNodes,
                inventionNodes,
                applicationNodes,
                nodesById
        );
    }

    private List<TechTreeNode> convertNodes(
            List<RawNode> rawNodes,
            TechTreeNodeType nodeType,
            Map<String, TechTreeNode> nodesById
    ) {
        List<TechTreeNode> converted = new ArrayList<>();
        for (RawNode rawNode : rawNodes) {
            if (rawNode == null || isBlank(rawNode.id)) {
                throw new IllegalStateException("Tech tree " + nodeType.name().toLowerCase(Locale.ROOT)
                        + " node is missing id");
            }
            String normalizedId = rawNode.id.trim();
            if (nodesById.containsKey(normalizedId)) {
                throw new IllegalStateException("Tech tree node id " + normalizedId + " is duplicated");
            }
            Set<String> prerequisites = new LinkedHashSet<>();
            for (String prerequisite : nullSafe(rawNode.prerequisites)) {
                if (isBlank(prerequisite)) {
                    throw new IllegalStateException("Tech tree node " + normalizedId + " has blank prerequisite");
                }
                String normalizedPrerequisite = prerequisite.trim();
                if (normalizedPrerequisite.equals(normalizedId)) {
                    throw new IllegalStateException("Tech tree node " + normalizedId + " cannot depend on itself");
                }
                prerequisites.add(normalizedPrerequisite);
            }

            Map<String, String> metadata = rawNode.metadata == null ? Map.of() : new LinkedHashMap<>(rawNode.metadata);
            TechTreeNode node = new TechTreeNode(
                    normalizedId,
                    nodeType,
                    List.copyOf(prerequisites),
                    isBlank(rawNode.triggerEventType) ? null : rawNode.triggerEventType.trim(),
                    Map.copyOf(metadata)
            );
            nodesById.put(normalizedId, node);
            converted.add(node);
        }
        return List.copyOf(converted);
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawTechTree {
        public String version;
        public List<RawNode> discoveries;
        public List<RawNode> inventions;
        public List<RawNode> applications;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RawNode {
        public String id;
        public List<String> prerequisites;
        public String triggerEventType;
        public Map<String, String> metadata;
    }
}
