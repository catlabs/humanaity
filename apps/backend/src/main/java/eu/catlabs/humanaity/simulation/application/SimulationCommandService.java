package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.agent.api.dto.AgentUiEffectOutput;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.application.EventApplicationService;
import eu.catlabs.humanaity.event.application.EventDraft;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.human.application.HumanDisplayNameFormatter;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandReferencedEntitiesOutput;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SimulationCommandService {

    private static final Pattern ADVANCE_PATTERN = Pattern.compile("^advance\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOCUS_PATTERN = Pattern.compile("^focus\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_PATTERN = Pattern.compile("^move\\s+(.+)\\s+(forest|river|church)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEET_PATTERN = Pattern.compile("^meet\\s+(.+)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_ADVANCE_COUNT = 20;

    private final CityRepository cityRepository;
    private final HumanRepository humanRepository;
    private final SimulationApplicationService simulationApplicationService;
    private final HumanGoalApplicationService humanGoalApplicationService;
    private final EventApplicationService eventApplicationService;

    public SimulationCommandService(
            CityRepository cityRepository,
            HumanRepository humanRepository,
            SimulationApplicationService simulationApplicationService,
            HumanGoalApplicationService humanGoalApplicationService,
            EventApplicationService eventApplicationService
    ) {
        this.cityRepository = cityRepository;
        this.humanRepository = humanRepository;
        this.simulationApplicationService = simulationApplicationService;
        this.humanGoalApplicationService = humanGoalApplicationService;
        this.eventApplicationService = eventApplicationService;
    }

    public SimulationCommandOutput execute(Long cityId, User currentUser, SimulationCommandInput input) {
        cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        String commandText = input == null || input.getCommandText() == null
                ? ""
                : input.getCommandText().trim();
        if (commandText.isBlank()) {
            return reject("Command is blank. Use `advance <count>`, `focus <human>`, `move <human> <place>`, or `meet <human> <human>`.");
        }

        Matcher advanceMatcher = ADVANCE_PATTERN.matcher(commandText);
        if (advanceMatcher.matches()) {
            return executeAdvance(cityId, Integer.parseInt(advanceMatcher.group(1)));
        }

        Matcher focusMatcher = FOCUS_PATTERN.matcher(commandText);
        if (focusMatcher.matches()) {
            return executeFocus(cityId, focusMatcher.group(1).trim());
        }

        Matcher moveMatcher = MOVE_PATTERN.matcher(commandText);
        if (moveMatcher.matches()) {
            return executeMove(cityId, moveMatcher.group(1).trim(), moveMatcher.group(2).trim().toLowerCase(Locale.ROOT));
        }

        Matcher meetMatcher = MEET_PATTERN.matcher(commandText);
        if (meetMatcher.matches()) {
            return executeMeet(cityId, meetMatcher.group(1).trim(), meetMatcher.group(2).trim());
        }

            return reject("Unsupported command. Use `advance <count>`, `focus <human>`, `move <human> <place>`, or `meet <human> <human>`.");
    }

    private SimulationCommandOutput executeAdvance(Long cityId, int count) {
        if (count < 1 || count > MAX_ADVANCE_COUNT) {
            return reject("`advance` count must be between 1 and 20.");
        }

        SimulationRun run = simulationApplicationService.step(cityId, count);
        long fromTick = Math.max(0L, run.getTick() - count + 1);

        SimulationCommandOutput output = success("ADVANCE", "Advanced city by " + count + " steps.", true);
        AgentUiEffectOutput refreshSnapshot = new AgentUiEffectOutput("REFRESH_SNAPSHOT");
        AgentUiEffectOutput refreshTimeline = new AgentUiEffectOutput("REFRESH_TIMELINE");
        refreshTimeline.setFromTick(fromTick);
        output.getUiEffects().add(refreshSnapshot);
        output.getUiEffects().add(refreshTimeline);
        return output;
    }

    private SimulationCommandOutput executeFocus(Long cityId, String humanToken) {
        Human human = resolveExactHuman(cityId, humanToken);
        if (human == null) {
            return reject("Could not resolve a single human for `focus`. Use an exact human id or exact name.");
        }

        SimulationCommandOutput output = success(
                "FOCUS_HUMAN",
                "Focused " + HumanDisplayNameFormatter.displayName(human) + ".",
                false
        );
        output.getReferencedEntities().setHumanId(human.getId());
        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(human.getId());
        output.getUiEffects().add(focus);
        return output;
    }

    private SimulationCommandOutput executeMove(Long cityId, String humanToken, String placeId) {
        Human human = resolveExactHuman(cityId, humanToken);
        if (human == null) {
            return reject("Could not resolve a single human for `move`. Use an exact human id or exact name.");
        }

        SimulationPlaceRegistry.SimulationPlace place = SimulationPlaceRegistry.byId(placeId).orElse(null);
        if (place == null) {
            return reject("Unsupported place. Use one of: forest, river, church.");
        }

        long assignedTick = currentTickForGoalAssignment(cityId);
        HumanGoal goal = humanGoalApplicationService.assignGoal(
                cityId,
                human.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.CHAT_COMMAND,
                assignedTick,
                new HumanGoalApplicationService.GoalTarget(place.id(), null, place.x(), place.y(), "command:move")
        );
        long fromTick = advanceOneStep(cityId);

        SimulationCommandOutput output = success(
                "MOVE_HUMAN_TO_PLACE",
                "Assigned " + HumanDisplayNameFormatter.displayName(human) + " to move toward " + place.id() + " and advanced city by 1 step.",
                true
        );
        output.getReferencedEntities().setHumanId(human.getId());
        output.getReferencedEntities().setPlaceId(place.id());

        output.getUiEffects().add(new AgentUiEffectOutput("REFRESH_SNAPSHOT"));
        AgentUiEffectOutput refreshTimeline = new AgentUiEffectOutput("REFRESH_TIMELINE");
        refreshTimeline.setFromTick(fromTick);
        output.getUiEffects().add(refreshTimeline);

        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(human.getId());
        output.getUiEffects().add(focus);

        AgentUiEffectOutput highlightPlace = new AgentUiEffectOutput("HIGHLIGHT_PLACE");
        highlightPlace.setPlaceId(place.id());
        output.getUiEffects().add(highlightPlace);
        return output;
    }

    private SimulationCommandOutput executeMeet(Long cityId, String actorToken, String targetToken) {
        Human actor = resolveExactHuman(cityId, actorToken);
        if (actor == null) {
            return reject("Could not resolve a single actor for `meet`. Use an exact human id or exact name.");
        }

        Human target = resolveExactHuman(cityId, targetToken);
        if (target == null) {
            return reject("Could not resolve a single target for `meet`. Use an exact human id or exact name.");
        }
        if (actor.getId().equals(target.getId())) {
            return reject("`meet` requires two distinct humans.");
        }

        long assignedTick = currentTickForGoalAssignment(cityId);
        HumanGoal goal = humanGoalApplicationService.assignGoal(
                cityId,
                actor.getId(),
                HumanGoalType.MEET_HUMAN,
                HumanGoalSource.CHAT_COMMAND,
                assignedTick,
                new HumanGoalApplicationService.GoalTarget(
                        null,
                        target.getId(),
                        target.getX(),
                        target.getY(),
                        "command:meet"
                )
        );
        long fromTick = advanceOneStep(cityId);

        SimulationCommandOutput output = success(
                "MEET_HUMAN",
                "Assigned " + HumanDisplayNameFormatter.displayName(actor) + " to meet " + HumanDisplayNameFormatter.displayName(target) + " and advanced city by 1 step.",
                true
        );
        output.getReferencedEntities().setHumanId(actor.getId());
        output.getReferencedEntities().setTargetHumanId(target.getId());

        output.getUiEffects().add(new AgentUiEffectOutput("REFRESH_SNAPSHOT"));
        AgentUiEffectOutput refreshTimeline = new AgentUiEffectOutput("REFRESH_TIMELINE");
        refreshTimeline.setFromTick(fromTick);
        output.getUiEffects().add(refreshTimeline);

        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(actor.getId());
        output.getUiEffects().add(focus);
        return output;
    }

    private long advanceOneStep(Long cityId) {
        SimulationRun run = simulationApplicationService.step(cityId, 1);
        return Math.max(0L, run.getTick());
    }

    private Human resolveExactHuman(Long cityId, String token) {
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        if (token.chars().allMatch(Character::isDigit)) {
            Long id = Long.parseLong(token);
            return humans.stream().filter(human -> human.getId().equals(id)).findFirst().orElse(null);
        }

        List<Human> matches = humans.stream()
                .filter(human -> HumanDisplayNameFormatter.displayName(human).equalsIgnoreCase(token))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private long currentTickForGoalAssignment(Long cityId) {
        try {
            return simulationApplicationService.loadRun(cityId).getTick();
        } catch (EntityNotFoundException notFound) {
            return simulationApplicationService.createRun(cityId).getTick();
        }
    }

    private SimulationCommandOutput success(String commandType, String message, boolean mutated) {
        SimulationCommandOutput output = new SimulationCommandOutput();
        output.setOk(true);
        output.setCommandType(commandType);
        output.setMessage(message);
        output.setMutated(mutated);
        output.setReferencedEntities(new SimulationCommandReferencedEntitiesOutput());
        return output;
    }

    private SimulationCommandOutput reject(String message) {
        SimulationCommandOutput output = new SimulationCommandOutput();
        output.setOk(false);
        output.setCommandType("UNSUPPORTED");
        output.setMessage(message);
        output.setMutated(false);
        output.setReferencedEntities(new SimulationCommandReferencedEntitiesOutput());
        return output;
    }
}
