package eu.catlabs.humanaity.simulation.application.query;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.history.domain.HistoryTimelineMapper;
import eu.catlabs.humanaity.human.application.HumanDisplayNameFormatter;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.domain.HumanTribeRole;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import eu.catlabs.humanaity.simulation.domain.TribeHouse;
import eu.catlabs.humanaity.simulation.domain.TribeKnownPlace;
import eu.catlabs.humanaity.simulation.domain.TribePlan;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribePlanRepository;
import eu.catlabs.humanaity.simulation.domain.TechTreeNodeType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class SimulationReadModelQueryService {

    private static final int RECENT_HISTORY_WINDOW = 20;

    private final CityRepository cityRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final HumanRepository humanRepository;
    private final EventRepository eventRepository;
    private final InventionRepository inventionRepository;
    private final KnowledgeUnlockRepository knowledgeUnlockRepository;
    private final TribeHouseRepository tribeHouseRepository;
    private final TribeKnownPlaceRepository tribeKnownPlaceRepository;
    private final TribePlanRepository tribePlanRepository;

    public SimulationReadModelQueryService(
            CityRepository cityRepository,
            SimulationRunRepository simulationRunRepository,
            HumanRepository humanRepository,
            EventRepository eventRepository,
            InventionRepository inventionRepository,
            KnowledgeUnlockRepository knowledgeUnlockRepository,
            TribeHouseRepository tribeHouseRepository,
            TribeKnownPlaceRepository tribeKnownPlaceRepository,
            TribePlanRepository tribePlanRepository
    ) {
        this.cityRepository = cityRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.humanRepository = humanRepository;
        this.eventRepository = eventRepository;
        this.inventionRepository = inventionRepository;
        this.knowledgeUnlockRepository = knowledgeUnlockRepository;
        this.tribeHouseRepository = tribeHouseRepository;
        this.tribeKnownPlaceRepository = tribeKnownPlaceRepository;
        this.tribePlanRepository = tribePlanRepository;
    }

    @Transactional(readOnly = true)
    public List<CityOverviewProjection> listCityOverviews(Predicate<Long> runningLookup) {
        return cityRepository.findAll().stream()
                .sorted(Comparator.comparing(City::getId))
                .map(city -> buildCityOverviewProjection(city, runningLookup))
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationSnapshotProjection getCitySnapshot(Long cityId, Predicate<Long> runningLookup) {
        Objects.requireNonNull(cityId, "cityId must not be null");
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        Optional<SimulationRun> maybeRun = simulationRunRepository.findByCityId(cityId);
        long tick = maybeRun.map(SimulationRun::getTick).orElse(0L);

        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        List<Event> events = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId);
        List<Invention> inventions = inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId);
        List<eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock> knowledgeUnlocks =
                knowledgeUnlockRepository.findByCityIdOrderByUnlockedTickAscNodeIdAsc(cityId);
        List<TribeHouse> tribeHouses = tribeHouseRepository.findByCityIdOrderByTribeIdAsc(cityId);
        List<TribeKnownPlace> tribeKnownPlaces = tribeKnownPlaceRepository.findByCityIdOrderByTribeIdAscDiscoveredTickAscIdAsc(cityId);

        int busyCount = (int) humans.stream().filter(Human::isBusy).count();
        int population = humans.size();
        double busyRatio = population == 0 ? 0.0 : ((double) busyCount / population);
        MetricsProjection metrics = buildMetricsProjection(humans, busyCount, population, events.size(), inventions.size());

        List<Event> recentEvents = takeRecent(events, RECENT_HISTORY_WINDOW);
        List<Invention> recentInventions = takeRecent(inventions, RECENT_HISTORY_WINDOW);
        List<eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock> recentKnowledgeUnlocks =
                takeRecent(knowledgeUnlocks, RECENT_HISTORY_WINDOW);

        return new SimulationSnapshotProjection(
                new CityProjection(city.getId(), city.getName()),
                new RunProjection(
                        maybeRun.isPresent(),
                        maybeRun.map(SimulationRun::getId).orElse(null),
                        maybeRun.map(SimulationRun::getSeed).orElse(null),
                        maybeRun.map(SimulationRun::getStatus).orElse(null),
                        runningLookup.test(cityId),
                        tick,
                        HistoryTimelineMapper.yearForTick(tick),
                        HistoryTimelineMapper.eraForTick(tick),
                        maybeRun.map(SimulationRun::getCreatedAt).orElse(null),
                        maybeRun.map(SimulationRun::getUpdatedAt).orElse(null)
                ),
                buildTribeProjection(tribeHouses, tribeKnownPlaces, humans),
                humans.stream()
                        .map(human -> new HumanProjection(
                                human.getId(),
                                human.getName(),
                                human.getTribeId(),
                                human.getTribeRole() == null ? null : human.getTribeRole().name(),
                                human.getX(),
                                human.getY(),
                                human.isBusy()
                        ))
                        .toList(),
                metrics,
                new TimelineSummaryProjection(
                        events.isEmpty() ? null : events.get(events.size() - 1).getTick(),
                        inventions.isEmpty() ? null : inventions.get(inventions.size() - 1).getTickCreated(),
                        knowledgeUnlocks.isEmpty() ? null : knowledgeUnlocks.get(knowledgeUnlocks.size() - 1).getUnlockedTick(),
                        recentEvents.size(),
                        recentInventions.size(),
                        recentKnowledgeUnlocks.size()
                ),
                buildKnowledgeProjection(knowledgeUnlocks),
                recentEvents,
                recentInventions
        );
    }

    @Transactional(readOnly = true)
    public List<CurrentTribePlanProjection> listCurrentTribePlans(Long cityId) {
        Objects.requireNonNull(cityId, "cityId must not be null");
        cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        Map<Long, String> humanNames = humans.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Human::getId,
                        HumanDisplayNameFormatter::displayName,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        Map<String, TribePlan> plansByTribe = tribePlanRepository.findByCityIdOrderByTribeIdAsc(cityId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        TribePlan::getTribeId,
                        plan -> plan,
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
        java.util.LinkedHashSet<String> tribeIds = new java.util.LinkedHashSet<>();
        tribeHouseRepository.findByCityIdOrderByTribeIdAsc(cityId).stream()
                .map(TribeHouse::getTribeId)
                .forEach(tribeIds::add);
        humans.stream()
                .map(Human::getTribeId)
                .filter(tribeId -> tribeId != null && !tribeId.isBlank())
                .sorted()
                .forEach(tribeIds::add);
        plansByTribe.keySet().forEach(tribeIds::add);

        return tribeIds.stream()
                .sorted()
                .map(tribeId -> {
                    TribePlan plan = plansByTribe.get(tribeId);
                    List<Long> assignedHumanIds = plan == null || plan.getAssignedHumanIds() == null
                            ? List.of()
                            : List.copyOf(plan.getAssignedHumanIds());
                    return new CurrentTribePlanProjection(
                            tribeId,
                            plan == null ? null : plan.getChiefHumanId(),
                            plan == null || plan.getChiefHumanId() == null
                                    ? null
                                    : humanNames.getOrDefault(plan.getChiefHumanId(), "Human " + plan.getChiefHumanId()),
                            plan == null ? null : plan.getPlanType(),
                            plan == null ? null : plan.getPlanStatus(),
                            plan == null ? null : plan.getTargetPlaceId(),
                            assignedHumanIds,
                            assignedHumanIds.stream()
                                    .map(humanId -> humanNames.getOrDefault(humanId, "Human " + humanId))
                                    .toList(),
                            plan == null ? null : plan.getDecisionSource(),
                            plan == null ? null : plan.getReasonSummary(),
                            plan != null,
                            plan == null ? null : plan.getLastAssignedTick(),
                            plan == null ? null : plan.getCompletedTick()
                    );
                })
                .toList();
    }

    private CityOverviewProjection buildCityOverviewProjection(City city, Predicate<Long> runningLookup) {
        Long cityId = city.getId();
        Optional<SimulationRun> maybeRun = simulationRunRepository.findByCityId(cityId);
        long tick = maybeRun.map(SimulationRun::getTick).orElse(0L);

        int population = humanRepository.findByCityId(cityId).size();
        int eventCount = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId).size();
        int inventionCount = inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId).size();
        List<eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock> knowledgeUnlocks =
                knowledgeUnlockRepository.findByCityIdOrderByUnlockedTickAscNodeIdAsc(cityId);
        int discoveryCount = (int) knowledgeUnlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.DISCOVERY)
                .count();
        int unlockedInventionCount = (int) knowledgeUnlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.INVENTION)
                .count();
        int applicationCount = (int) knowledgeUnlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.APPLICATION)
                .count();

        return new CityOverviewProjection(
                cityId,
                city.getName(),
                maybeRun.isPresent(),
                maybeRun.map(SimulationRun::getStatus).orElse(null),
                runningLookup.test(cityId),
                tick,
                HistoryTimelineMapper.yearForTick(tick),
                HistoryTimelineMapper.eraForTick(tick),
                population,
                inventionCount,
                eventCount,
                discoveryCount,
                unlockedInventionCount,
                applicationCount,
                maybeRun.map(SimulationRun::getUpdatedAt).orElse(null)
        );
    }

    private KnowledgeProjection buildKnowledgeProjection(
            List<eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock> unlocks
    ) {
        List<String> discoveries = unlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.DISCOVERY)
                .map(eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock::getNodeId)
                .toList();
        List<String> inventions = unlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.INVENTION)
                .map(eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock::getNodeId)
                .toList();
        List<String> applications = unlocks.stream()
                .filter(unlock -> unlock.getNodeType() == TechTreeNodeType.APPLICATION)
                .map(eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock::getNodeId)
                .toList();
        return new KnowledgeProjection(discoveries, inventions, applications);
    }

    private MetricsProjection buildMetricsProjection(
            List<Human> humans,
            int busyCount,
            int population,
            int eventCount,
            int inventionCount
    ) {
        List<Human> humansWithFiniteCoordinates = humans.stream()
                .filter(human -> human.getX() != null && Double.isFinite(human.getX()))
                .filter(human -> human.getY() != null && Double.isFinite(human.getY()))
                .toList();

        CentroidProjection centroid = null;
        BoundsProjection bounds = null;
        if (!humansWithFiniteCoordinates.isEmpty()) {
            double centroidX = humansWithFiniteCoordinates.stream().mapToDouble(Human::getX).average().orElse(0.0);
            double centroidY = humansWithFiniteCoordinates.stream().mapToDouble(Human::getY).average().orElse(0.0);
            double minX = humansWithFiniteCoordinates.stream().mapToDouble(Human::getX).min().orElse(0.0);
            double maxX = humansWithFiniteCoordinates.stream().mapToDouble(Human::getX).max().orElse(0.0);
            double minY = humansWithFiniteCoordinates.stream().mapToDouble(Human::getY).min().orElse(0.0);
            double maxY = humansWithFiniteCoordinates.stream().mapToDouble(Human::getY).max().orElse(0.0);
            centroid = new CentroidProjection(centroidX, centroidY);
            bounds = new BoundsProjection(minX, maxX, minY, maxY);
        }

        return new MetricsProjection(
                population,
                busyCount,
                population == 0 ? 0.0 : ((double) busyCount / population),
                centroid,
                bounds,
                eventCount,
                inventionCount
        );
    }

    private List<TribeProjection> buildTribeProjection(
            List<TribeHouse> tribeHouses,
            List<TribeKnownPlace> tribeKnownPlaces,
            List<Human> humans
    ) {
        Map<String, List<Human>> humansByTribe = humans.stream()
                .filter(human -> human.getTribeId() != null && !human.getTribeId().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(Human::getTribeId, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

        return tribeHouses.stream()
                .map(house -> {
                    String tribeId = house.getTribeId();
                    Long scoutHumanId = humansByTribe.getOrDefault(tribeId, List.of()).stream()
                            .filter(human -> human.getTribeRole() == HumanTribeRole.SCOUT)
                            .map(Human::getId)
                            .findFirst()
                            .orElse(null);
                    List<KnownPlaceProjection> knownPlaces = tribeKnownPlaces.stream()
                            .filter(place -> tribeId.equals(place.getTribeId()))
                            .map(place -> new KnownPlaceProjection(
                                    place.getPlaceId(),
                                    place.getDiscoveredByHumanId(),
                                    place.getDiscoveredTick(),
                                    place.getReportedTick(),
                                    place.isReported()
                            ))
                            .toList();
                    return new TribeProjection(
                            tribeId,
                            new TribeHouseProjection(house.getX(), house.getY()),
                            scoutHumanId,
                            knownPlaces
                    );
                })
                .toList();
    }

    private <T> List<T> takeRecent(List<T> all, int recentLimit) {
        if (all.size() <= recentLimit) {
            return all;
        }
        return all.subList(all.size() - recentLimit, all.size());
    }

    public record CityOverviewProjection(
            Long cityId,
            String cityName,
            boolean hasRun,
            SimulationRunStatus runStatus,
            boolean running,
            Long tick,
            Integer year,
            HistoryEra era,
            Integer population,
            Integer inventionCount,
            Integer eventCount,
            Integer discoveryUnlockCount,
            Integer unlockedInventionCount,
            Integer applicationUnlockCount,
            Instant updatedAt
    ) {
    }

    public record SimulationSnapshotProjection(
            CityProjection city,
            RunProjection run,
            List<TribeProjection> tribes,
            List<HumanProjection> humans,
            MetricsProjection metrics,
            TimelineSummaryProjection timelineSummary,
            KnowledgeProjection knowledge,
            List<Event> recentEvents,
            List<Invention> recentInventions
    ) {
    }

    public record CityProjection(
            Long id,
            String name
    ) {
    }

    public record RunProjection(
            boolean hasRun,
            Long runId,
            Long seed,
            SimulationRunStatus status,
            boolean running,
            Long tick,
            Integer year,
            HistoryEra era,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record HumanProjection(
            Long id,
            String name,
            String tribeId,
            String tribeRole,
            Double x,
            Double y,
            boolean busy
    ) {
    }

    public record MetricsProjection(
            Integer population,
            Integer busyCount,
            Double busyRatio,
            CentroidProjection centroid,
            BoundsProjection bounds,
            Integer eventCount,
            Integer inventionCount
    ) {
    }

    public record CentroidProjection(
            Double x,
            Double y
    ) {
    }

    public record BoundsProjection(
            Double minX,
            Double maxX,
            Double minY,
            Double maxY
    ) {
    }

    public record TimelineSummaryProjection(
            Long latestEventTick,
            Long latestInventionTick,
            Long latestKnowledgeUnlockTick,
            Integer recentEventCount,
            Integer recentInventionCount,
            Integer recentKnowledgeUnlockCount
    ) {
    }

    public record KnowledgeProjection(
            List<String> unlockedDiscoveries,
            List<String> unlockedInventions,
            List<String> unlockedApplications
    ) {
    }

    public record TribeProjection(
            String tribeId,
            TribeHouseProjection house,
            Long scoutHumanId,
            List<KnownPlaceProjection> knownPlaces
    ) {
    }

    public record TribeHouseProjection(
            Double x,
            Double y
    ) {
    }

    public record KnownPlaceProjection(
            String placeId,
            Long discoveredByHumanId,
            Long discoveredTick,
            Long reportedTick,
            boolean reported
    ) {
    }

    public record CurrentTribePlanProjection(
            String tribeId,
            Long chiefHumanId,
            String chiefHumanName,
            eu.catlabs.humanaity.simulation.application.tribe.TribeDecisionType planType,
            eu.catlabs.humanaity.simulation.domain.TribePlanStatus planStatus,
            String targetPlaceId,
            List<Long> assignedHumanIds,
            List<String> assignedHumanNames,
            eu.catlabs.humanaity.simulation.domain.TribeDecisionSource decisionSource,
            String reasonSummary,
            boolean hasPlan,
            Long lastAssignedTick,
            Long completedTick
    ) {
    }
}
