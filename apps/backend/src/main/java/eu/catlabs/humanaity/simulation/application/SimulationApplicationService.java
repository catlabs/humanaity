package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.human.application.HumanApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class SimulationApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationApplicationService.class);
    private static final double COLLISION_DISTANCE = 0.02;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private final HumanRepository humanRepository;
    private final HumanApplicationService humanApplicationService;
    private final Random random = new Random();

    public SimulationApplicationService(HumanRepository humanRepository, HumanApplicationService humanApplicationService) {
        this.humanRepository = humanRepository;
        this.humanApplicationService = humanApplicationService;
    }

    public synchronized String startSimulation(Long cityId) {
        if (runningTasks.containsKey(cityId)) {
            return "Simulation already running for city " + cityId;
        }

        ScheduledFuture<?> task = executor.scheduleAtFixedRate(
                () -> simulateCity(cityId),
                0, 100, TimeUnit.MILLISECONDS
        );

        runningTasks.put(cityId, task);
        return "Simulation started for city " + cityId;
    }

    public synchronized String stopSimulation(Long cityId) {
        ScheduledFuture<?> task = runningTasks.get(cityId);
        if (task == null) {
            return "No simulation running for city " + cityId;
        }

        task.cancel(true);
        runningTasks.remove(cityId);
        return "Simulation stopped for city " + cityId;
    }

    public boolean isRunning(Long cityId) {
        return runningTasks.containsKey(cityId);
    }

    private void simulateCity(Long cityId) {
        try {
            List<Human> allHumans = humanRepository.findByCityId(cityId);
            Collections.shuffle(allHumans);
            List<Human> randomHumans = allHumans.stream().limit(10).toList();

            if (randomHumans.isEmpty()) {
                logger.debug("No humans found in city {}", cityId);
                return;
            }

            Set<Human> changedHumans = new HashSet<>();

            for (Human human : randomHumans) {
                if (!human.isBusy()) {
                    boolean hadCollision = checkCollisions(human, allHumans, changedHumans);
                    if (!hadCollision) {
                        updateHumanPosition(human);
                        changedHumans.add(human);
                    }
                }
            }
            if (!changedHumans.isEmpty()) {
                humanRepository.saveAll(changedHumans);
            }
            this.humanApplicationService.publishHumanUpdates(randomHumans);

        } catch (Exception e) {
            logger.error("Error simulating city {}: {}", cityId, e.getMessage(), e);
        }
    }

    private boolean checkCollisions(Human currentHuman, List<Human> allHumans, Set<Human> changedHumans) {
        for (Human otherHuman : allHumans) {
            if (currentHuman.getId().equals(otherHuman.getId())) {
                continue;
            }

            if (areHumansColliding(currentHuman, otherHuman)) {
                currentHuman.setBusy(true);
                otherHuman.setBusy(true);

                changedHumans.add(currentHuman);
                changedHumans.add(otherHuman);
                return true;
            }
        }
        return false;
    }

    private boolean areHumansColliding(Human human1, Human human2) {
        double distance = Math.sqrt(
                Math.pow(human1.getX() - human2.getX(), 2) +
                        Math.pow(human1.getY() - human2.getY(), 2)
        );
        return distance < COLLISION_DISTANCE;
    }

    private void updateHumanPosition(Human human) {
        double deltaX = (random.nextDouble() - 0.5) * 0.1;
        double deltaY = (random.nextDouble() - 0.5) * 0.1;

        double newX = Math.max(0, Math.min(1, human.getX() + deltaX));
        double newY = Math.max(0, Math.min(1, human.getY() + deltaY));

        human.setX(newX);
        human.setY(newY);
    }
}
