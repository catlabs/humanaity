package eu.catlabs.humanaity.simulation.application.tribe;

import eu.catlabs.humanaity.event.application.EventApplicationService;
import eu.catlabs.humanaity.event.application.EventDraft;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.domain.HumanTribeRole;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.application.HumanGoalApplicationService;
import eu.catlabs.humanaity.simulation.application.SimulationPlaceRegistry;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.domain.TribeDecisionSource;
import eu.catlabs.humanaity.simulation.domain.TribeHouse;
import eu.catlabs.humanaity.simulation.domain.TribeKnownPlace;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribePlanRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TribeExplorationService {

    private final TribeHouseRepository tribeHouseRepository;
    private final TribeKnownPlaceRepository tribeKnownPlaceRepository;
    private final HumanGoalApplicationService humanGoalApplicationService;
    private final HumanRepository humanRepository;
    private final CityRepository cityRepository;
    private final EventApplicationService eventApplicationService;
    private final TribeDecisionSelector tribeDecisionSelector;
    private final TribeChiefDecisionResolver tribeChiefDecisionResolver;
    private final TribePlanApplicationService tribePlanApplicationService;
    private final TribePlanRepository tribePlanRepository;

    public TribeExplorationService(
            TribeHouseRepository tribeHouseRepository,
            TribeKnownPlaceRepository tribeKnownPlaceRepository,
            HumanGoalApplicationService humanGoalApplicationService,
            HumanRepository humanRepository,
            CityRepository cityRepository,
            EventApplicationService eventApplicationService,
            TribeDecisionSelector tribeDecisionSelector,
            TribeChiefDecisionResolver tribeChiefDecisionResolver,
            TribePlanApplicationService tribePlanApplicationService
            ,
            TribePlanRepository tribePlanRepository
    ) {
        this.tribeHouseRepository = tribeHouseRepository;
        this.tribeKnownPlaceRepository = tribeKnownPlaceRepository;
        this.humanGoalApplicationService = humanGoalApplicationService;
        this.humanRepository = humanRepository;
        this.cityRepository = cityRepository;
        this.eventApplicationService = eventApplicationService;
        this.tribeDecisionSelector = tribeDecisionSelector;
        this.tribeChiefDecisionResolver = tribeChiefDecisionResolver;
        this.tribePlanApplicationService = tribePlanApplicationService;
        this.tribePlanRepository = tribePlanRepository;
    }

    @Transactional
    public List<EventDraft> processCompletedGoal(
            Long cityId,
            long tick,
            HumanGoal goal,
            List<Human> humans,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        if (goal == null || goal.getHuman() == null || goal.getHuman().getTribeId() == null) {
            return List.of();
        }

        String metadataKey = safeMetadataKey(goal.getMetadataKey());
        if (metadataKey.startsWith("tribe-explore:")) {
            return handleScoutDiscovery(cityId, tick, goal, humans, activeGoalsByHuman, metadataKey);
        }
        if (metadataKey.startsWith("tribe-report:")) {
            return handleScoutReport(cityId, tick, goal, humans, activeGoalsByHuman, metadataKey);
        }
        return List.of();
    }

    @Transactional
    public List<EventDraft> planTribeActions(
            Long cityId,
            long runSeed,
            long tick,
            List<Human> humans,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        List<EventDraft> drafts = new ArrayList<>();
        normalizeTribeRoles(humans);
        Map<String, List<Human>> humansByTribe = humans.stream()
                .filter(human -> human.getTribeId() != null && !human.getTribeId().isBlank())
                .collect(Collectors.groupingBy(Human::getTribeId, LinkedHashMap::new, Collectors.toList()));

        for (TribeHouse house : tribeHouseRepository.findByCityIdOrderByTribeIdAsc(cityId)) {
            String tribeId = house.getTribeId();
            List<Human> tribeHumans = humansByTribe.getOrDefault(tribeId, List.of());
            if (tribeHumans.isEmpty()) {
                continue;
            }

            Human scout = selectScout(tribeHumans);
            if (scout != null && !activeGoalsByHuman.containsKey(scout.getId())) {
                Optional<TribeDecisionCandidate> scoutCandidate = selectScoutCandidate(cityId, runSeed, tick, house, scout);
                scoutCandidate.ifPresent(candidate -> drafts.addAll(applyScoutCandidate(cityId, tick, candidate, activeGoalsByHuman)));
            }

            Human chief = selectChief(tribeHumans);
            if (chief != null) {
                List<TribeDecisionCandidate> chiefCandidates = buildChiefDecisionCandidates(
                        cityId,
                        tick,
                        tribeHumans,
                        activeGoalsByHuman,
                        chief
                );
                TribeChiefDecisionResolver.TribeDecisionResolution resolution = tribeChiefDecisionResolver.resolve(cityId, chiefCandidates);
                if (resolution.candidate() != null) {
                    drafts.addAll(applyChiefCandidate(
                            cityId,
                            tick,
                            chief,
                            tribeHumans,
                            activeGoalsByHuman,
                            resolution
                    ));
                }
            }
        }

        return drafts;
    }

    private List<EventDraft> handleScoutDiscovery(
            Long cityId,
            long tick,
            HumanGoal goal,
            List<Human> humans,
            Map<Long, HumanGoal> activeGoalsByHuman,
            String metadataKey
    ) {
        String tribeId = goal.getHuman().getTribeId();
        String placeId = parsePlaceId(metadataKey).orElse(goal.getTargetPlaceId());
        if (placeId == null || tribeId == null) {
            return List.of();
        }

        Optional<TribeKnownPlace> existing = tribeKnownPlaceRepository
                .findFirstByCityIdAndTribeIdAndPlaceId(cityId, tribeId, placeId);
        if (existing.isPresent()) {
            return List.of();
        }

        TribeKnownPlace knownPlace = new TribeKnownPlace();
        knownPlace.setCity(cityRepository.findById(cityId).orElseThrow());
        knownPlace.setTribeId(tribeId);
        knownPlace.setPlaceId(placeId);
        knownPlace.setDiscoveredByHumanId(goal.getHuman().getId());
        knownPlace.setDiscoveredTick(tick);
        knownPlace.setReported(false);
        tribeKnownPlaceRepository.save(knownPlace);

        List<EventDraft> drafts = new ArrayList<>();
        drafts.add(new EventDraft(
                EventType.TRIBE_PLACE_DISCOVERED,
                List.of(goal.getHuman().getId()),
                discoveryPayload(tribeId, placeId, goal.getHuman().getId(), tick),
                42,
                tribeEventKey("DISCOVERY", tribeId, placeId, tick)
        ));

        TribeHouse house = tribeHouseRepository.findFirstByCityIdAndTribeId(cityId, tribeId).orElse(null);
        if (house != null) {
            HumanGoal returnHomeGoal = humanGoalApplicationService.assignGoal(
                    cityId,
                    goal.getHuman().getId(),
                    HumanGoalType.MOVE_TO_PLACE,
                    HumanGoalSource.AUTONOMOUS,
                    tick,
                    new HumanGoalApplicationService.GoalTarget(
                            null,
                            null,
                            house.getX(),
                            house.getY(),
                            tribeReportMetadata(tribeId, placeId)
                    )
            );
            activeGoalsByHuman.put(goal.getHuman().getId(), returnHomeGoal);
            tribePlanApplicationService.activatePlan(
                    cityId,
                    tribeId,
                    goal.getHuman().getId(),
                    TribeDecisionType.SCOUT_RETURN_HOME,
                    placeId,
                    List.of(goal.getHuman().getId()),
                    TribeDecisionSource.DETERMINISTIC,
                    "Scout returns home to report " + placeId,
                    returnHomeGoal.getMetadataKey(),
                    tick
            );
        }

        return drafts;
    }

    private List<EventDraft> handleScoutReport(
            Long cityId,
            long tick,
            HumanGoal goal,
            List<Human> humans,
            Map<Long, HumanGoal> activeGoalsByHuman,
            String metadataKey
    ) {
        String tribeId = goal.getHuman().getTribeId();
        String placeId = parsePlaceId(metadataKey).orElse(goal.getTargetPlaceId());
        if (placeId == null || tribeId == null) {
            return List.of();
        }

        TribeKnownPlace knownPlace = tribeKnownPlaceRepository
                .findFirstByCityIdAndTribeIdAndPlaceId(cityId, tribeId, placeId)
                .orElseGet(() -> {
                    TribeKnownPlace created = new TribeKnownPlace();
                    created.setCity(cityRepository.findById(cityId).orElseThrow());
                    created.setTribeId(tribeId);
                    created.setPlaceId(placeId);
                    created.setDiscoveredByHumanId(goal.getHuman().getId());
                    created.setDiscoveredTick(tick);
                    created.setReported(false);
                    return tribeKnownPlaceRepository.save(created);
                });

        if (knownPlace.isReported()) {
            return List.of();
        }

        knownPlace.setReported(true);
        knownPlace.setReportedTick(tick);
        tribeKnownPlaceRepository.save(knownPlace);

        List<EventDraft> drafts = new ArrayList<>();
        String sourceDiscoveryEventKey = reachedPlaceDiscoveryKey(goal.getHuman().getId(), placeId, tick);
        drafts.add(new EventDraft(
                EventType.TRIBE_SCOUT_REPORT,
                List.of(goal.getHuman().getId()),
                scoutReportPayload(tribeId, placeId, goal.getHuman().getId(), chiefHumanId(tribeId, humans), sourceDiscoveryEventKey, tick),
                39,
                tribeEventKey("SCOUT_REPORT", tribeId, placeId, tick)
        ));
        drafts.add(new EventDraft(
                EventType.TRIBE_DISCOVERY_REPORTED,
                List.of(goal.getHuman().getId()),
                reportPayload(tribeId, placeId, goal.getHuman().getId(), tick),
                38,
                tribeEventKey("REPORT", tribeId, placeId, tick)
        ));

        return drafts;
    }

    private void normalizeTribeRoles(List<Human> humans) {
        if (humans == null || humans.isEmpty()) {
            return;
        }

        Map<String, List<Human>> humansByTribe = humans.stream()
                .filter(human -> human.getTribeId() != null && !human.getTribeId().isBlank())
                .collect(Collectors.groupingBy(Human::getTribeId, LinkedHashMap::new, Collectors.toList()));

        List<Human> mutated = new ArrayList<>();
        for (List<Human> tribeHumans : humansByTribe.values()) {
            List<Human> ordered = tribeHumans.stream()
                    .sorted(Comparator.comparing(Human::getId))
                    .toList();
            for (int index = 0; index < ordered.size(); index++) {
                HumanTribeRole desiredRole = switch (index) {
                    case 0 -> HumanTribeRole.CHIEF;
                    case 1 -> HumanTribeRole.SCOUT;
                    default -> HumanTribeRole.MEMBER;
                };
                Human human = ordered.get(index);
                if (human.getTribeRole() != desiredRole) {
                    human.setTribeRole(desiredRole);
                    mutated.add(human);
                }
            }
        }

        if (!mutated.isEmpty()) {
            humanRepository.saveAll(mutated);
        }
    }

    private Human selectChief(List<Human> tribeHumans) {
        return tribeHumans.stream()
                .filter(human -> human.getTribeRole() == HumanTribeRole.CHIEF)
                .sorted(Comparator.comparing(Human::getId))
                .findFirst()
                .orElseGet(() -> tribeHumans.stream()
                        .sorted(Comparator.comparing(Human::getId))
                        .findFirst()
                        .orElse(null));
    }

    private int rolePriority(Human human) {
        return switch (human.getTribeRole() == null ? HumanTribeRole.MEMBER : human.getTribeRole()) {
            case SCOUT -> 0;
            case MEMBER -> 1;
            case CHIEF -> 2;
        };
    }

    private List<TribeDecisionCandidate> buildChiefDecisionCandidates(
            Long cityId,
            long tick,
            List<Human> tribeHumans,
            Map<Long, HumanGoal> activeGoalsByHuman,
            Human chief
    ) {
        if (chief == null || chief.getTribeId() == null || chief.getTribeId().isBlank()) {
            return List.of();
        }

        TribeDecisionCandidate waitCandidate = new TribeDecisionCandidate(
                tribeChiefOptionId(chief.getTribeId(), "wait"),
                TribeDecisionType.WAIT,
                chief.getTribeId(),
                chief.getId(),
                List.of(),
                null,
                null,
                null,
                0,
                "Chief waits for a better opportunity"
        );

        if (tribePlanRepository.findFirstByCityIdAndTribeId(cityId, chief.getTribeId())
                .filter(plan -> plan.getPlanStatus() == eu.catlabs.humanaity.simulation.domain.TribePlanStatus.ACTIVE)
                .isPresent()) {
            return List.of(waitCandidate);
        }

        List<TribeDecisionCandidate> candidates = new ArrayList<>();
        for (TribeKnownPlace knownPlace : tribeKnownPlaceRepository.findByCityIdAndTribeIdOrderByDiscoveredTickAscIdAsc(cityId, chief.getTribeId())) {
            if (!knownPlace.isReported() || knownPlace.getReportedTick() == null || knownPlace.getReportedTick() >= tick) {
                continue;
            }
            Optional<SimulationPlaceRegistry.SimulationPlace> place = SimulationPlaceRegistry.byId(knownPlace.getPlaceId());
            if (place.isEmpty()) {
                continue;
            }

            List<Human> eligibleMembers = tribeHumans.stream()
                    .filter(human -> !human.getId().equals(chief.getId()))
                    .filter(human -> human.getTribeRole() != HumanTribeRole.CHIEF)
                    .filter(human -> !activeGoalsByHuman.containsKey(human.getId()))
                    .sorted(Comparator
                            .comparingInt(this::rolePriority)
                            .thenComparing(Human::getId))
                    .toList();
            if (eligibleMembers.size() < 2) {
                continue;
            }

            List<Long> assigneeIds = eligibleMembers.stream()
                    .limit(2)
                    .map(Human::getId)
                    .sorted()
                    .toList();
            candidates.add(new TribeDecisionCandidate(
                    tribeChiefOptionId(chief.getTribeId(), knownPlace.getPlaceId(), assigneeIds),
                    TribeDecisionType.SEND_TWO_MEMBERS_TO_KNOWN_PLACE,
                    chief.getTribeId(),
                    chief.getId(),
                    assigneeIds,
                    knownPlace.getPlaceId(),
                    place.get().x(),
                    place.get().y(),
                    100,
                    "Chief sends two members to " + knownPlace.getPlaceId()
            ));
        }

        candidates.add(waitCandidate);
        return candidates;
    }

    private List<EventDraft> applyChiefCandidate(
            Long cityId,
            long tick,
            Human chief,
            List<Human> tribeHumans,
            Map<Long, HumanGoal> activeGoalsByHuman,
            TribeChiefDecisionResolver.TribeDecisionResolution resolution
    ) {
        TribeDecisionCandidate candidate = resolution.candidate();
        if (candidate == null) {
            return List.of();
        }

        List<EventDraft> drafts = new ArrayList<>();
        if (candidate.type() == TribeDecisionType.WAIT) {
            drafts.add(new EventDraft(
                    EventType.TRIBE_PLAN_CHOSEN,
                    List.of(chief.getId()),
                    buildTribePlanChosenPayload(
                            chief,
                            candidate,
                            null,
                            resolution.decisionSource(),
                            resolution.fallbackReason(),
                            null,
                            tick
                    ),
                    35,
                    chiefPlanEventKey(chief.getTribeId(), candidate.candidateId(), tick)
            ));
            return drafts;
        }

        Optional<SimulationPlaceRegistry.SimulationPlace> place = SimulationPlaceRegistry.byId(candidate.placeId());
        if (place.isEmpty() || candidate.memberIds() == null || candidate.memberIds().size() < 2) {
            return List.of(new EventDraft(
                    EventType.TRIBE_PLAN_CHOSEN,
                    List.of(chief.getId()),
                    buildTribePlanChosenPayload(
                            chief,
                            new TribeDecisionCandidate(
                                    tribeChiefOptionId(chief.getTribeId(), "wait"),
                                    TribeDecisionType.WAIT,
                                    chief.getTribeId(),
                                    chief.getId(),
                                    List.of(),
                                    null,
                                    null,
                                    null,
                                    0,
                                    "Chief waits for a better opportunity"
                            ),
                            null,
                            TribeDecisionSource.DETERMINISTIC_FALLBACK,
                            "invalid-place-or-members",
                            null,
                            tick
                    ),
                    35,
                    chiefPlanEventKey(chief.getTribeId(), candidate.candidateId(), tick)
            ));
        }

        TribeKnownPlace knownPlace = tribeKnownPlaceRepository
                .findFirstByCityIdAndTribeIdAndPlaceId(cityId, chief.getTribeId(), candidate.placeId())
                .orElse(null);
        String sourceDiscoveryEventKey = knownPlace == null
                ? ""
                : discoveredPlaceEventKey(knownPlace);
        String metadataKey = tribePlanMetadataKey(candidate.candidateId());
        List<Long> assigneeIds = candidate.memberIds().stream().sorted().toList();
        tribePlanApplicationService.activatePlan(
                cityId,
                chief.getTribeId(),
                chief.getId(),
                candidate.type(),
                candidate.placeId(),
                assigneeIds,
                resolution.decisionSource(),
                candidate.description(),
                metadataKey,
                tick
        );

        drafts.add(new EventDraft(
                EventType.TRIBE_PLAN_CHOSEN,
                List.of(chief.getId()),
                buildTribePlanChosenPayload(
                        chief,
                        candidate,
                        sourceDiscoveryEventKey,
                        resolution.decisionSource(),
                        resolution.fallbackReason(),
                        assigneeIds,
                        tick
                ),
                40,
                chiefPlanEventKey(chief.getTribeId(), candidate.candidateId(), tick)
        ));

        for (Long assigneeId : assigneeIds) {
            Human assignee = tribeHumans.stream()
                    .filter(human -> human.getId().equals(assigneeId))
                    .findFirst()
                    .orElse(null);
            if (assignee == null) {
                continue;
            }

            HumanGoal goal = humanGoalApplicationService.assignGoal(
                    cityId,
                    assignee.getId(),
                    HumanGoalType.MOVE_TO_PLACE,
                    HumanGoalSource.TRIBE_PLAN,
                    tick,
                    new HumanGoalApplicationService.GoalTarget(
                            candidate.placeId(),
                            null,
                            place.get().x(),
                            place.get().y(),
                            metadataKey
                    )
            );
            activeGoalsByHuman.put(assignee.getId(), goal);
            drafts.add(new EventDraft(
                    EventType.GOAL_ASSIGNED,
                    List.of(assignee.getId()),
                    buildTribePlanGoalAssignedPayload(
                            chief,
                            candidate,
                            assignee,
                            goal,
                            sourceDiscoveryEventKey,
                            tick
                    ),
                    22,
                    tribeGoalAssignedEventKey(candidate.candidateId(), assignee.getId(), tick)
            ));
        }

        return drafts;
    }

    private Map<String, String> buildTribePlanChosenPayload(
            Human chief,
            TribeDecisionCandidate candidate,
            String sourceDiscoveryEventKey,
            TribeDecisionSource decisionSource,
            String fallbackReason,
            List<Long> assigneeIds,
            long tick
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tribeId", chief.getTribeId());
        payload.put("chiefId", String.valueOf(chief.getId()));
        payload.put("planType", candidate.type().name());
        payload.put("optionId", candidate.candidateId());
        if (candidate.placeId() != null) {
            payload.put("placeId", candidate.placeId());
        }
        payload.put("assigneeIds", assigneeIds == null || assigneeIds.isEmpty()
                ? ""
                : assigneeIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        if (sourceDiscoveryEventKey != null && !sourceDiscoveryEventKey.isBlank()) {
            payload.put("sourceDiscoveryEventKey", sourceDiscoveryEventKey);
        }
        payload.put("decisionSource", decisionSource.name());
        if (fallbackReason != null && !fallbackReason.isBlank()) {
            payload.put("fallbackReason", fallbackReason);
        }
        payload.put("tick", String.valueOf(tick));
        payload.put("title", "Chief plan chosen");
        payload.put("summary", buildChiefPlanSummary(candidate, assigneeIds));
        return payload;
    }

    private Map<String, String> buildTribePlanGoalAssignedPayload(
            Human chief,
            TribeDecisionCandidate candidate,
            Human assignee,
            HumanGoal goal,
            String sourceDiscoveryEventKey,
            long tick
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tribeId", chief.getTribeId());
        payload.put("chiefId", String.valueOf(chief.getId()));
        payload.put("optionId", candidate.candidateId());
        payload.put("placeId", candidate.placeId());
        payload.put("goalId", String.valueOf(goal.getId()));
        payload.put("goalType", goal.getGoalType().name());
        payload.put("source", goal.getSource().name());
        payload.put("metadataKey", goal.getMetadataKey());
        payload.put("assigneeId", String.valueOf(assignee.getId()));
        payload.put("assigneeName", assignee.getName() == null ? "" : assignee.getName());
        if (sourceDiscoveryEventKey != null && !sourceDiscoveryEventKey.isBlank()) {
            payload.put("sourceDiscoveryEventKey", sourceDiscoveryEventKey);
        }
        payload.put("tick", String.valueOf(tick));
        payload.put("title", "Tribe member assigned");
        payload.put("summary", "The chief assigned " + assignee.getId() + " to " + candidate.placeId() + ".");
        return payload;
    }

    private String buildChiefPlanSummary(TribeDecisionCandidate candidate, List<Long> assigneeIds) {
        if (candidate.type() == TribeDecisionType.WAIT) {
            return "The chief chose to wait.";
        }
        return "The chief sent " + assigneeIds + " to " + candidate.placeId() + ".";
    }

    private String tribePlanMetadataKey(String optionId) {
        return "tribe-plan:" + optionId;
    }

    private String discoveredPlaceEventKey(TribeKnownPlace knownPlace) {
        return reachedPlaceDiscoveryKey(
                knownPlace.getDiscoveredByHumanId(),
                knownPlace.getPlaceId(),
                knownPlace.getDiscoveredTick()
        );
    }

    private String reachedPlaceDiscoveryKey(Long humanId, String placeId, long tick) {
        return "REACHED:" + humanId + ":" + placeId + ":" + tick;
    }

    private String chiefHumanId(String tribeId, List<Human> humans) {
        return humans.stream()
                .filter(human -> tribeId.equals(human.getTribeId()))
                .filter(human -> human.getTribeRole() == HumanTribeRole.CHIEF)
                .map(Human::getId)
                .findFirst()
                .map(String::valueOf)
                .orElse("");
    }

    private String chiefPlanEventKey(String tribeId, String optionId, long tick) {
        return "TRIBE_PLAN_CHOSEN:" + tribeId + ":" + optionId + ":" + tick;
    }

    private String tribeGoalAssignedEventKey(String optionId, Long assigneeId, long tick) {
        return "TRIBE_GOAL_ASSIGNED:" + optionId + ":" + assigneeId + ":" + tick;
    }

    private String tribeChiefOptionId(String tribeId, String placeId, List<Long> assigneeIds) {
        List<Long> ordered = assigneeIds == null ? List.of() : assigneeIds.stream().sorted().toList();
        return tribeId + ":send-two:" + placeId + ":" + ordered.stream().map(String::valueOf).collect(Collectors.joining("-"));
    }

    private String tribeChiefOptionId(String tribeId, String action) {
        return tribeId + ":" + action;
    }

    private Optional<TribeDecisionCandidate> selectScoutCandidate(
            Long cityId,
            long runSeed,
            long tick,
            TribeHouse house,
            Human scout
    ) {
        List<TribeKnownPlace> knownPlaces = tribeKnownPlaceRepository
                .findByCityIdAndTribeIdOrderByDiscoveredTickAscIdAsc(cityId, scout.getTribeId());
        Optional<TribeKnownPlace> pendingReport = knownPlaces.stream()
                .filter(place -> !place.isReported())
                .findFirst();
        if (pendingReport.isPresent()) {
            TribeKnownPlace knownPlace = pendingReport.get();
            return Optional.of(new TribeDecisionCandidate(
                    tribeActionId(scout.getTribeId(), "return", knownPlace.getPlaceId()),
                    TribeDecisionType.SCOUT_RETURN_HOME,
                    scout.getTribeId(),
                    scout.getId(),
                    List.of(scout.getId()),
                    knownPlace.getPlaceId(),
                    house.getX(),
                    house.getY(),
                    200,
                    "Scout returns home to report " + knownPlace.getPlaceId()
            ));
        }

        List<SimulationPlaceRegistry.SimulationPlace> unknownPlaces = SimulationPlaceRegistry.all().stream()
                .filter(place -> knownPlaces.stream().noneMatch(knownPlace -> knownPlace.getPlaceId().equals(place.id())))
                .toList();
        if (unknownPlaces.isEmpty()) {
            return Optional.empty();
        }

        int placeIndex = Math.floorMod((int) ((runSeed ^ (tick * 31L) ^ scout.getTribeId().hashCode()) & Integer.MAX_VALUE), unknownPlaces.size());
        SimulationPlaceRegistry.SimulationPlace place = unknownPlaces.get(placeIndex);
        return Optional.of(new TribeDecisionCandidate(
                tribeActionId(scout.getTribeId(), "explore", place.id()),
                TribeDecisionType.SCOUT_EXPLORE,
                scout.getTribeId(),
                scout.getId(),
                List.of(scout.getId()),
                place.id(),
                place.x(),
                place.y(),
                100,
                "Scout explores " + place.id()
        ));
    }

    private Optional<TribeDecisionCandidate> selectTravelCandidate(
            List<Human> tribeHumans,
            Human scout,
            TribeKnownPlace knownPlace,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        Optional<SimulationPlaceRegistry.SimulationPlace> place = SimulationPlaceRegistry.byId(knownPlace.getPlaceId());
        if (place.isEmpty()) {
            return Optional.empty();
        }

        List<Human> eligibleMembers = tribeHumans.stream()
                .filter(human -> !human.getId().equals(scout.getId()))
                .filter(human -> human.getTribeRole() != HumanTribeRole.CHIEF)
                .filter(human -> !activeGoalsByHuman.containsKey(human.getId()))
                .filter(human -> distance(human.getX(), human.getY(), place.get().x(), place.get().y()) > place.get().radius())
                .sorted(Comparator.comparing(Human::getId))
                .toList();
        if (eligibleMembers.size() < 2) {
            return Optional.empty();
        }

        List<TribeDecisionCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < eligibleMembers.size() - 1; i++) {
            for (int j = i + 1; j < eligibleMembers.size(); j++) {
                Human first = eligibleMembers.get(i);
                Human second = eligibleMembers.get(j);
                candidates.add(new TribeDecisionCandidate(
                        tribeActionId(scout.getTribeId(), "travel", knownPlace.getPlaceId(), first.getId(), second.getId()),
                TribeDecisionType.GROUP_TRAVEL,
                scout.getTribeId(),
                scout.getId(),
                List.of(first.getId(), second.getId()),
                knownPlace.getPlaceId(),
                place.get().x(),
                        place.get().y(),
                        50 - i - j,
                        "Two tribe members travel together to " + knownPlace.getPlaceId()
                ));
            }
        }

        return tribeDecisionSelector.select(candidates);
    }

    private List<EventDraft> applyScoutCandidate(
            Long cityId,
            long tick,
            TribeDecisionCandidate candidate,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        Human scout = humanRepository.findById(candidate.humanId()).orElse(null);
        if (scout == null) {
            return List.of();
        }

        HumanGoal goal = humanGoalApplicationService.assignGoal(
                cityId,
                scout.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.AUTONOMOUS,
                tick,
                new HumanGoalApplicationService.GoalTarget(
                        candidate.placeId(),
                        null,
                        candidate.targetX(),
                        candidate.targetY(),
                        candidate.type() == TribeDecisionType.SCOUT_RETURN_HOME
                                ? tribeReportMetadata(scout.getTribeId(), candidate.placeId())
                                : tribeExploreMetadata(scout.getTribeId(), candidate.placeId())
                )
        );
        activeGoalsByHuman.put(scout.getId(), goal);
        tribePlanApplicationService.activatePlan(
                cityId,
                candidate.tribeId(),
                scout.getId(),
                candidate.type(),
                candidate.placeId(),
                List.of(scout.getId()),
                TribeDecisionSource.DETERMINISTIC,
                candidate.description(),
                goal.getMetadataKey(),
                tick
        );
        return List.of();
    }

    private List<EventDraft> applyTravelCandidate(
            Long cityId,
            long tick,
            TribeDecisionCandidate candidate,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        List<Long> memberIds = candidate.memberIds();
        if (memberIds == null || memberIds.size() < 2) {
            return List.of();
        }

        Human first = humanRepository.findById(memberIds.get(0)).orElse(null);
        Human second = humanRepository.findById(memberIds.get(1)).orElse(null);
        if (first == null || second == null) {
            return List.of();
        }

        String coordinationKey = coordinationKey(candidate.tribeId(), candidate.placeId(), memberIds, tick);
        HumanGoal firstGoal = humanGoalApplicationService.assignGoal(
                cityId,
                first.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.AUTONOMOUS,
                tick,
                new HumanGoalApplicationService.GoalTarget(candidate.placeId(), null, candidate.targetX(), candidate.targetY(), coordinationKey)
        );
        HumanGoal secondGoal = humanGoalApplicationService.assignGoal(
                cityId,
                second.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.AUTONOMOUS,
                tick,
                new HumanGoalApplicationService.GoalTarget(candidate.placeId(), null, candidate.targetX(), candidate.targetY(), coordinationKey)
        );
        activeGoalsByHuman.put(first.getId(), firstGoal);
        activeGoalsByHuman.put(second.getId(), secondGoal);
        tribePlanApplicationService.activatePlan(
                cityId,
                candidate.tribeId(),
                candidate.humanId(),
                candidate.type(),
                candidate.placeId(),
                memberIds,
                TribeDecisionSource.DETERMINISTIC,
                candidate.description(),
                coordinationKey,
                tick
        );

        return List.of(new EventDraft(
                EventType.TRIBE_GROUP_TRAVEL_COORDINATED,
                memberIds,
                travelPayload(candidate.tribeId(), candidate.placeId(), coordinationKey, memberIds),
                34,
                tribeEventKey("TRAVEL", candidate.tribeId(), candidate.placeId(), tick)
        ));
    }

    private List<EventDraft> tryAssignGroupTravel(
            Long cityId,
            long tick,
            String tribeId,
            String placeId,
            List<Human> humans,
            Map<Long, HumanGoal> activeGoalsByHuman
    ) {
        TribeHouse house = tribeHouseRepository.findFirstByCityIdAndTribeId(cityId, tribeId).orElse(null);
        if (house == null) {
            return List.of();
        }

        if (hasAlreadyCoordinatedTravel(cityId, tribeId, placeId)) {
            return List.of();
        }

        Optional<SimulationPlaceRegistry.SimulationPlace> place = SimulationPlaceRegistry.byId(placeId);
        if (place.isEmpty()) {
            return List.of();
        }

        List<Human> eligibleMembers = humans.stream()
                .filter(human -> tribeId.equals(human.getTribeId()))
                .filter(human -> human.getTribeRole() != HumanTribeRole.CHIEF)
                .filter(human -> !activeGoalsByHuman.containsKey(human.getId()))
                .filter(human -> distance(human.getX(), human.getY(), place.get().x(), place.get().y()) > place.get().radius())
                .sorted(Comparator.comparing(Human::getId))
                .toList();
        if (eligibleMembers.size() < 2) {
            return List.of();
        }

        List<TribeDecisionCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < eligibleMembers.size() - 1; i++) {
            for (int j = i + 1; j < eligibleMembers.size(); j++) {
                Human first = eligibleMembers.get(i);
                Human second = eligibleMembers.get(j);
                candidates.add(new TribeDecisionCandidate(
                        tribeActionId(tribeId, "travel", placeId, first.getId(), second.getId()),
                        TribeDecisionType.GROUP_TRAVEL,
                        tribeId,
                        null,
                        List.of(first.getId(), second.getId()),
                        placeId,
                        place.get().x(),
                        place.get().y(),
                        50 - i - j,
                        "Two tribe members travel together to " + placeId
                ));
            }
        }

        return tribeDecisionSelector.select(candidates)
                .map(candidate -> applyTravelCandidate(cityId, tick, candidate, activeGoalsByHuman))
                .orElse(List.of());
    }

    private boolean hasFreeEligibleTravelPair(
            List<Human> tribeHumans,
            Human scout,
            Map<Long, HumanGoal> activeGoalsByHuman,
            TribeKnownPlace knownPlace
    ) {
        Optional<SimulationPlaceRegistry.SimulationPlace> place = SimulationPlaceRegistry.byId(knownPlace.getPlaceId());
        if (place.isEmpty()) {
            return false;
        }

        long count = tribeHumans.stream()
                .filter(human -> !human.getId().equals(scout.getId()))
                .filter(human -> human.getTribeRole() != HumanTribeRole.CHIEF)
                .filter(human -> !activeGoalsByHuman.containsKey(human.getId()))
                .filter(human -> distance(human.getX(), human.getY(), place.get().x(), place.get().y()) > place.get().radius())
                .count();
        return count >= 2;
    }

    private Optional<TribeKnownPlace> selectReportedPlaceWithoutCoordinatedTravel(Long cityId, String tribeId) {
        return tribeKnownPlaceRepository.findByCityIdAndTribeIdOrderByDiscoveredTickAscIdAsc(cityId, tribeId).stream()
                .filter(TribeKnownPlace::isReported)
                .filter(place -> !hasAlreadyCoordinatedTravel(cityId, tribeId, place.getPlaceId()))
                .findFirst();
    }

    private boolean hasAlreadyCoordinatedTravel(Long cityId, String tribeId, String placeId) {
        return eventApplicationService.listCityEventsByType(cityId, EventType.TRIBE_GROUP_TRAVEL_COORDINATED).stream()
                .filter(event -> event.getPayload() != null)
                .anyMatch(event -> tribeId.equals(event.getPayload().get("tribeId"))
                        && placeId.equals(event.getPayload().get("placeId")));
    }

    private Human selectScout(List<Human> tribeHumans) {
        return tribeHumans.stream()
                .filter(human -> human.getTribeRole() == HumanTribeRole.SCOUT)
                .sorted(Comparator.comparing(Human::getId))
                .findFirst()
                .orElseGet(() -> tribeHumans.stream()
                        .sorted(Comparator.comparing(Human::getId))
                        .findFirst()
                        .orElse(null));
    }

    private Optional<String> parsePlaceId(String metadataKey) {
        String[] parts = safeMetadataKey(metadataKey).split(":");
        if (parts.length >= 3) {
            return Optional.of(parts[2]);
        }
        return Optional.empty();
    }

    private String safeMetadataKey(String metadataKey) {
        return metadataKey == null ? "" : metadataKey.trim();
    }

    private String tribeExploreMetadata(String tribeId, String placeId) {
        return "tribe-explore:" + tribeId + ":" + placeId;
    }

    private String tribeReportMetadata(String tribeId, String placeId) {
        return "tribe-report:" + tribeId + ":" + placeId;
    }

    private String tribeActionId(String tribeId, String action, String placeId, Long... memberIds) {
        String members = memberIds == null || memberIds.length == 0
                ? ""
                : ":" + java.util.Arrays.stream(memberIds).map(String::valueOf).collect(Collectors.joining("-"));
        return tribeId + ":" + action + ":" + placeId + members;
    }

    private String tribeEventKey(String action, String tribeId, String placeId, long tick) {
        return "TRIBE:" + action + ":" + tribeId + ":" + placeId + ":" + tick;
    }

    private String coordinationKey(String tribeId, String placeId, List<Long> memberIds, long tick) {
        List<Long> orderedMembers = memberIds.stream().sorted().toList();
        return tribeId + ":" + placeId + ":" + tick + ":" + orderedMembers.get(0) + "-" + orderedMembers.get(1);
    }

    private Map<String, String> discoveryPayload(String tribeId, String placeId, Long humanId, long tick) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tribeId", tribeId);
        payload.put("placeId", placeId);
        payload.put("triggerHumanId", String.valueOf(humanId));
        payload.put("tick", String.valueOf(tick));
        payload.put("title", "Tribe discovery");
        payload.put("summary", "A scout discovered " + placeId + " for " + tribeId + ".");
        return payload;
    }

    private Map<String, String> reportPayload(String tribeId, String placeId, Long humanId, long tick) {
        return reportPayload(tribeId, placeId, humanId, tick, null);
    }

    private Map<String, String> reportPayload(String tribeId, String placeId, Long humanId, long tick, String chiefId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tribeId", tribeId);
        payload.put("placeId", placeId);
        payload.put("reportingHumanId", String.valueOf(humanId));
        if (chiefId != null && !chiefId.isBlank()) {
            payload.put("chiefId", chiefId);
        }
        payload.put("tick", String.valueOf(tick));
        payload.put("title", "Tribe discovery reported");
        payload.put("summary", "A scout reported " + placeId + " to " + tribeId + ".");
        return payload;
    }

    private Map<String, String> scoutReportPayload(
            String tribeId,
            String placeId,
            Long humanId,
            String chiefId,
            String sourceDiscoveryEventKey,
            long tick
    ) {
        Map<String, String> payload = reportPayload(tribeId, placeId, humanId, tick, chiefId);
        payload.put("sourceDiscoveryEventKey", sourceDiscoveryEventKey);
        payload.put("title", "Scout report");
        payload.put("summary", "A scout reported " + placeId + " to the chief.");
        return payload;
    }

    private Map<String, String> travelPayload(String tribeId, String placeId, String coordinationKey, List<Long> memberIds) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("tribeId", tribeId);
        payload.put("placeId", placeId);
        payload.put("coordinationKey", coordinationKey);
        payload.put("memberIds", memberIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        payload.put("title", "Coordinated travel");
        payload.put("summary", "Two tribe members coordinated travel toward " + placeId + ".");
        return payload;
    }

    private double distance(Double x, Double y, double targetX, double targetY) {
        double safeX = x == null ? 0.5 : x;
        double safeY = y == null ? 0.5 : y;
        double deltaX = safeX - targetX;
        double deltaY = safeY - targetY;
        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }
}
