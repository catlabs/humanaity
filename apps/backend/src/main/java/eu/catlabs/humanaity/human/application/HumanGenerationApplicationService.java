package eu.catlabs.humanaity.human.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.human.api.dto.HumanInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class HumanGenerationApplicationService {

    private static final String TRIBE_A = "tribe-a";
    private static final String TRIBE_B = "tribe-b";
    private static final int HUMANS_PER_CITY = 6;
    private static final List<String> LOCAL_NAMES = List.of("Ari", "Bo", "Cy", "Dee", "Ena", "Fio");
    private static final double[][] TRIBE_A_POSITIONS = {
            {0.18, 0.20},
            {0.26, 0.30},
            {0.30, 0.18}
    };
    private static final double[][] TRIBE_B_POSITIONS = {
            {0.72, 0.72},
            {0.80, 0.64},
            {0.86, 0.78}
    };

    private final HumanApplicationService humanApplicationService;

    public HumanGenerationApplicationService(HumanApplicationService humanApplicationService) {
        this.humanApplicationService = humanApplicationService;
    }

    @Transactional
    public void generateHumansForCity(City city) {
        if (city == null || city.getId() == null) {
            throw new IllegalArgumentException("City must be saved before generating humans");
        }

        Random random = new Random(seedFor(city));
        for (int index = 0; index < HUMANS_PER_CITY; index++) {
            boolean tribeA = index < (HUMANS_PER_CITY / 2);
            double[] basePosition = tribeA
                    ? TRIBE_A_POSITIONS[index % TRIBE_A_POSITIONS.length]
                    : TRIBE_B_POSITIONS[index % TRIBE_B_POSITIONS.length];

            double creativity = trait(random);
            double intellect = trait(random);
            double sociability = trait(random);
            double practicality = trait(random);

            HumanInput humanInput = new HumanInput();
            humanInput.setCityId(city.getId());
            humanInput.setBusy(false);
            humanInput.setName(determineName(index));
            humanInput.setTribeId(tribeA ? TRIBE_A : TRIBE_B);
            humanInput.setX(clamp(basePosition[0] + jitter(random, 0.018)));
            humanInput.setY(clamp(basePosition[1] + jitter(random, 0.018)));
            humanInput.setCreativity(creativity);
            humanInput.setIntellect(intellect);
            humanInput.setSociability(sociability);
            humanInput.setPracticality(practicality);
            humanInput.setPersonality(humanApplicationService.derivePersonality(
                    creativity,
                    intellect,
                    sociability,
                    practicality
            ));
            humanApplicationService.createHuman(humanInput);
        }
    }

    private long seedFor(City city) {
        long seed = 17L;
        seed = (seed * 31L) + city.getId();
        seed = (seed * 31L) + (city.getName() == null ? 0L : city.getName().hashCode());
        return seed;
    }

    private String determineName(int index) {
        if (index % 2 == 0) {
            return "";
        }
        return LOCAL_NAMES.get(index % LOCAL_NAMES.size());
    }

    private double jitter(Random random, double range) {
        return ((random.nextDouble() * 2.0) - 1.0) * range;
    }

    private double clamp(double value) {
        return Math.max(0.05, Math.min(0.95, value));
    }

    private double trait(Random random) {
        return round(0.35 + (random.nextDouble() * 0.55));
    }

    private double round(double value) {
        return Math.round(Math.max(0.0, Math.min(1.0, value)) * 100.0) / 100.0;
    }
}
