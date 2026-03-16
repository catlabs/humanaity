package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock;
import eu.catlabs.humanaity.simulation.domain.TechTreeCatalog;
import eu.catlabs.humanaity.simulation.domain.TechTreeNode;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class KnowledgeProgressionService {

    private final KnowledgeUnlockRepository knowledgeUnlockRepository;
    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final TechTreeCatalogLoader techTreeCatalogLoader;

    public KnowledgeProgressionService(
            KnowledgeUnlockRepository knowledgeUnlockRepository,
            EventRepository eventRepository,
            CityRepository cityRepository,
            TechTreeCatalogLoader techTreeCatalogLoader
    ) {
        this.knowledgeUnlockRepository = knowledgeUnlockRepository;
        this.eventRepository = eventRepository;
        this.cityRepository = cityRepository;
        this.techTreeCatalogLoader = techTreeCatalogLoader;
    }

    @Transactional
    public List<KnowledgeUnlock> evaluateUnlocks(Long cityId, long currentTick) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        TechTreeCatalog catalog = techTreeCatalogLoader.getCatalog();
        List<Event> events = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId);
        Set<String> eventTypesPresent = new LinkedHashSet<>();
        for (Event event : events) {
            eventTypesPresent.add(event.getEventType().name());
        }

        List<KnowledgeUnlock> existing = knowledgeUnlockRepository.findByCityIdOrderByUnlockedTickAscNodeIdAsc(cityId);
        Set<String> unlockedNodeIds = new LinkedHashSet<>();
        for (KnowledgeUnlock unlock : existing) {
            unlockedNodeIds.add(unlock.getNodeId());
        }

        List<TechTreeNode> evaluationOrder = new ArrayList<>();
        evaluationOrder.addAll(catalog.discoveries());
        evaluationOrder.addAll(catalog.inventions());
        evaluationOrder.addAll(catalog.applications());

        List<KnowledgeUnlock> created = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TechTreeNode node : evaluationOrder) {
                if (unlockedNodeIds.contains(node.id())) {
                    continue;
                }
                if (!unlockedNodeIds.containsAll(node.prerequisites())) {
                    continue;
                }
                if (node.nodeType() == eu.catlabs.humanaity.simulation.domain.TechTreeNodeType.DISCOVERY
                        && (node.triggerEventType() == null || !eventTypesPresent.contains(node.triggerEventType()))) {
                    continue;
                }

                KnowledgeUnlock unlock = new KnowledgeUnlock();
                unlock.setCity(city);
                unlock.setNodeId(node.id());
                unlock.setNodeType(node.nodeType());
                unlock.setUnlockedTick(currentTick);
                unlock.setTriggerEventType(node.triggerEventType());
                KnowledgeUnlock saved = knowledgeUnlockRepository.save(unlock);
                created.add(saved);
                unlockedNodeIds.add(node.id());
                changed = true;
            }
        }
        return created;
    }
}
