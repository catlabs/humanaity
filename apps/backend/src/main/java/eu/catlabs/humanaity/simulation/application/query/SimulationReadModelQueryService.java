package eu.catlabs.humanaity.simulation.application.query;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.history.domain.HistoryTimelineMapper;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

    public SimulationReadModelQueryService(
            CityRepository cityRepository,
            SimulationRunRepository simulationRunRepository,
            HumanRepository humanRepository,
            EventRepository eventRepository,
            InventionRepository inventionRepository
    ) {
        this.cityRepository = cityRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.humanRepository = humanRepository;
        this.eventRepository = eventRepository;
        this.inventionRepository = inventionRepository;
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

        int busyCount = (int) humans.stream().filter(Human::isBusy).count();
        int population = humans.size();
        double busyRatio = population == 0 ? 0.0 : ((double) busyCount / population);
        MetricsProjection metrics = buildMetricsProjection(humans, busyCount, population, events.size(), inventions.size());

        List<Event> recentEvents = takeRecent(events, RECENT_HISTORY_WINDOW);
        List<Invention> recentInventions = takeRecent(inventions, RECENT_HISTORY_WINDOW);

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
                humans.stream()
                        .map(human -> new HumanProjection(
                                human.getId(),
                                human.getName(),
                                human.getX(),
                                human.getY(),
                                human.isBusy()
                        ))
                        .toList(),
                metrics,
                new TimelineSummaryProjection(
                        events.isEmpty() ? null : events.get(events.size() - 1).getTick(),
                        inventions.isEmpty() ? null : inventions.get(inventions.size() - 1).getTickCreated(),
                        recentEvents.size(),
                        recentInventions.size()
                ),
                recentEvents,
                recentInventions
        );
    }

    private CityOverviewProjection buildCityOverviewProjection(City city, Predicate<Long> runningLookup) {
        Long cityId = city.getId();
        Optional<SimulationRun> maybeRun = simulationRunRepository.findByCityId(cityId);
        long tick = maybeRun.map(SimulationRun::getTick).orElse(0L);

        int population = humanRepository.findByCityId(cityId).size();
        int eventCount = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId).size();
        int inventionCount = inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId).size();

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
                maybeRun.map(SimulationRun::getUpdatedAt).orElse(null)
        );
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
            Instant updatedAt
    ) {
    }

    public record SimulationSnapshotProjection(
            CityProjection city,
            RunProjection run,
            List<HumanProjection> humans,
            MetricsProjection metrics,
            TimelineSummaryProjection timelineSummary,
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
            Integer recentEventCount,
            Integer recentInventionCount
    ) {
    }
}
