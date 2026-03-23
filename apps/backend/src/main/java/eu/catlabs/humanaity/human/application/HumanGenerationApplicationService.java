package eu.catlabs.humanaity.human.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.human.api.dto.HumanInput;
import eu.catlabs.humanaity.human.domain.HumanTribeRole;
import eu.catlabs.humanaity.simulation.domain.TribeHouse;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
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
    private static final double[][] TRIBE_A_OFFSETS = {
            {-0.02, 0.00},
            {0.06, 0.08},
            {0.10, -0.02}
    };
    private static final double[][] TRIBE_B_OFFSETS = {
            {-0.02, 0.00},
            {0.06, -0.08},
            {0.10, 0.02}
    };

    private final HumanApplicationService humanApplicationService;
    private final TribeHouseRepository tribeHouseRepository;

    public HumanGenerationApplicationService(
            HumanApplicationService humanApplicationService,
            TribeHouseRepository tribeHouseRepository
    ) {
        this.humanApplicationService = humanApplicationService;
        this.tribeHouseRepository = tribeHouseRepository;
    }

    @Transactional
    public void generateHumansForCity(City city) {
        if (city == null || city.getId() == null) {
            throw new IllegalArgumentException("City must be saved before generating humans");
        }

        Random random = new Random(seedFor(city));
        List<TribeHouse> tribeHouses = tribeHouseRepository.findByCityIdOrderByTribeIdAsc(city.getId());
        for (int index = 0; index < HUMANS_PER_CITY; index++) {
            boolean tribeA = index < (HUMANS_PER_CITY / 2);
            int tribeIndex = index % (HUMANS_PER_CITY / 2);
            double[] basePosition = tribeA
                    ? determinePosition(tribeHouses, "tribe-a", index, TRIBE_A_OFFSETS, 0.20, 0.24)
                    : determinePosition(tribeHouses, "tribe-b", index - (HUMANS_PER_CITY / 2), TRIBE_B_OFFSETS, 0.80, 0.76);

            double creativity = trait(random);
            double intellect = trait(random);
            double sociability = trait(random);
            double practicality = trait(random);

            HumanInput humanInput = new HumanInput();
            humanInput.setCityId(city.getId());
            humanInput.setBusy(false);
            humanInput.setName(determineName(index));
            humanInput.setTribeId(tribeA ? TRIBE_A : TRIBE_B);
            humanInput.setTribeRole(switch (tribeIndex) {
                case 0 -> HumanTribeRole.CHIEF;
                case 1 -> HumanTribeRole.SCOUT;
                default -> HumanTribeRole.MEMBER;
            });
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

    private double[] determinePosition(
            List<TribeHouse> tribeHouses,
            String tribeId,
            int tribeIndex,
            double[][] offsets,
            double fallbackX,
            double fallbackY
    ) {
        TribeHouse house = tribeHouses.stream()
                .filter(candidate -> tribeId.equals(candidate.getTribeId()))
                .findFirst()
                .orElse(null);
        double baseX = house == null || house.getX() == null ? fallbackX : house.getX();
        double baseY = house == null || house.getY() == null ? fallbackY : house.getY();
        double[] offset = offsets[tribeIndex % offsets.length];
        return new double[]{baseX + offset[0], baseY + offset[1]};
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
