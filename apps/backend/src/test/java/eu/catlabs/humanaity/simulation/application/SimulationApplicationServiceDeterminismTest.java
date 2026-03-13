package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.application.EventApplicationService;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.invention.application.InventionApplicationService;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.human.application.HumanApplicationService;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulationApplicationServiceDeterminismTest {

    @Mock
    private HumanRepository humanRepository;
    @Mock
    private HumanApplicationService humanApplicationService;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private SimulationRunRepository simulationRunRepository;
    @Mock
    private EventApplicationService eventApplicationService;
    @Mock
    private InventionApplicationService inventionApplicationService;

    @Test
    void sameSeedAndSameInitialStateYieldSameFinalStateAfterSameStepCount() {
        ScenarioContext left = createScenario(42L, 4L, 1L);
        ScenarioContext right = createScenario(42L, 5L, 2L);

        left.service.step(left.cityId, 6);
        right.service.step(right.cityId, 6);

        assertRunsAndHumansEquivalent(left.run, left.humans, right.run, right.humans);
    }

    @Test
    void pauseResumePreservesContinuityComparedToUninterruptedRun() {
        ScenarioContext uninterrupted = createScenario(77L, 10L, 1L);
        ScenarioContext paused = createScenario(77L, 11L, 2L);

        uninterrupted.service.step(uninterrupted.cityId, 5);

        paused.service.step(paused.cityId, 2);
        paused.service.pauseRun(paused.cityId);
        paused.service.resumeRun(paused.cityId);
        paused.service.step(paused.cityId, 3);

        assertRunsAndHumansEquivalent(uninterrupted.run, uninterrupted.humans, paused.run, paused.humans);
        assertThat(paused.run.getStatus()).isEqualTo(SimulationRunStatus.RUNNING);
    }

    @Test
    void schedulerWrapperAndManualSteppingConvergeToSameState() {
        ScenarioContext schedulerScenario = createScenario(999L, 12L, 1L);
        ScenarioContext manualScenario = createScenario(999L, 13L, 2L);

        manualScenario.service.step(manualScenario.cityId, 5);

        schedulerScenario.service.stepViaSchedulerWrapper(schedulerScenario.cityId, 5);

        assertThat(schedulerScenario.run.getTick()).isEqualTo(5L);
        assertRunsAndHumansEquivalent(
                manualScenario.run,
                manualScenario.humans,
                schedulerScenario.run,
                schedulerScenario.humans,
                5L
        );
    }


    private void assertRunsAndHumansEquivalent(
            SimulationRun expectedRun,
            List<Human> expectedHumans,
            SimulationRun actualRun,
            List<Human> actualHumans
    ) {
        assertRunsAndHumansEquivalent(expectedRun, expectedHumans, actualRun, actualHumans, expectedRun.getTick());
    }

    private void assertRunsAndHumansEquivalent(
            SimulationRun expectedRun,
            List<Human> expectedHumans,
            SimulationRun actualRun,
            List<Human> actualHumans,
            long expectedTick
    ) {
        assertThat(actualRun.getTick()).isEqualTo(expectedTick);

        List<Human> orderedExpected = expectedHumans.stream().sorted(Comparator.comparing(Human::getId)).toList();
        List<Human> orderedActual = actualHumans.stream().sorted(Comparator.comparing(Human::getId)).toList();

        assertThat(orderedActual).hasSameSizeAs(orderedExpected);

        for (int i = 0; i < orderedExpected.size(); i++) {
            Human left = orderedExpected.get(i);
            Human right = orderedActual.get(i);
            assertThat(right.getId()).isEqualTo(left.getId());
            assertThat(right.getX()).isEqualTo(left.getX());
            assertThat(right.getY()).isEqualTo(left.getY());
            assertThat(right.isBusy()).isEqualTo(left.isBusy());
        }
    }

    private ScenarioContext createScenario(long seed, long cityId, long runId) {
        SimulationApplicationService service = new SimulationApplicationService(
                humanRepository,
                humanApplicationService,
                cityRepository,
                simulationRunRepository,
                eventApplicationService,
                inventionApplicationService
        );

        City city = new City();
        city.setId(cityId);

        SimulationRun run = new SimulationRun();
        run.setId(runId);
        run.setCity(city);
        run.setSeed(seed);
        run.setTick(0L);
        run.setStatus(SimulationRunStatus.RUNNING);

        List<Human> humans = createOrderedHumans(city);

        when(simulationRunRepository.findByCityId(cityId)).thenReturn(Optional.of(run));
        when(simulationRunRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(humanRepository.findByCityIdOrderByIdAsc(cityId)).thenAnswer(invocation -> humans);
        when(humanRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(humanApplicationService).publishHumanUpdates(any());
        when(eventApplicationService.emitEventsAtTick(anyLong(), anyLong(), any())).thenReturn(List.of());
        when(eventApplicationService.emitLifecycleEvent(anyLong(), anyLong(), any(), any(), any(Map.class), anyInt()))
                .thenReturn(new Event());
        when(inventionApplicationService.deriveFromPersistedEvents(anyLong())).thenReturn(List.of());
        when(inventionApplicationService.listCityInventions(anyLong(), any(), any(), any())).thenReturn(List.of());
        when(eventApplicationService.listCityEvents(anyLong(), any(), any(), any())).thenReturn(List.of());

        return new ScenarioContext(service, run, humans, cityId);
    }

    private List<Human> createOrderedHumans(City city) {
        List<Human> humans = new ArrayList<>();
        humans.add(createHuman(3L, 0.1, 0.1, city));
        humans.add(createHuman(1L, 0.2, 0.3, city));
        humans.add(createHuman(2L, 0.8, 0.7, city));
        return humans;
    }

    private Human createHuman(Long id, double x, double y, City city) {
        Human human = new Human();
        human.setId(id);
        human.setX(x);
        human.setY(y);
        human.setCity(city);
        human.setBusy(false);
        return human;
    }

    private record ScenarioContext(SimulationApplicationService service, SimulationRun run, List<Human> humans, Long cityId) {
    }
}
