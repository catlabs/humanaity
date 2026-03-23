package eu.catlabs.humanaity.city.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.simulation.domain.TribeHouse;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class CityTribeBootstrapService {

    private static final String TRIBE_A = "tribe-a";
    private static final String TRIBE_B = "tribe-b";

    private final TribeHouseRepository tribeHouseRepository;

    public CityTribeBootstrapService(TribeHouseRepository tribeHouseRepository) {
        this.tribeHouseRepository = tribeHouseRepository;
    }

    @Transactional
    public List<TribeHouse> ensureTribeHouses(City city) {
        List<TribeHouse> existing = tribeHouseRepository.findByCityIdOrderByTribeIdAsc(city.getId());
        if (existing.size() == 2) {
            return existing;
        }

        if (!existing.isEmpty()) {
            tribeHouseRepository.deleteAll(existing);
        }

        Random random = new Random(seedFor(city));
        TribeHouse tribeA = buildHouse(city, TRIBE_A, selectAnchor(random, true));
        TribeHouse tribeB = buildHouse(city, TRIBE_B, selectAnchor(random, false));
        return tribeHouseRepository.saveAll(List.of(tribeA, tribeB));
    }

    private TribeHouse buildHouse(City city, String tribeId, double[] anchor) {
        TribeHouse house = new TribeHouse();
        house.setCity(city);
        house.setTribeId(tribeId);
        house.setX(anchor[0]);
        house.setY(anchor[1]);
        return house;
    }

    private double[] selectAnchor(Random random, boolean leftSide) {
        List<double[]> anchors = leftSide
                ? List.of(
                        new double[]{0.18, 0.22},
                        new double[]{0.22, 0.34},
                        new double[]{0.20, 0.72},
                        new double[]{0.26, 0.60}
                )
                : List.of(
                        new double[]{0.82, 0.24},
                        new double[]{0.76, 0.36},
                        new double[]{0.80, 0.74},
                        new double[]{0.72, 0.62}
                );
        return anchors.get(random.nextInt(anchors.size()));
    }

    private long seedFor(City city) {
        long seed = 23L;
        seed = (seed * 31L) + city.getId();
        seed = (seed * 31L) + (city.getName() == null ? 0L : city.getName().hashCode());
        return seed;
    }
}
