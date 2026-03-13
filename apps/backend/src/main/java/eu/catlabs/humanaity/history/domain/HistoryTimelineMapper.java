package eu.catlabs.humanaity.history.domain;

public final class HistoryTimelineMapper {

    public static final long TICKS_PER_YEAR = 10L;

    private HistoryTimelineMapper() {
    }

    public static int yearForTick(long tick) {
        return (int) Math.floorDiv(Math.max(0L, tick), TICKS_PER_YEAR) + 1;
    }

    public static HistoryEra eraForTick(long tick) {
        return eraForYear(yearForTick(tick));
    }

    public static HistoryEra eraForYear(int year) {
        if (year <= 25) {
            return HistoryEra.FOUNDING;
        }
        if (year <= 50) {
            return HistoryEra.EXPANSION;
        }
        if (year <= 75) {
            return HistoryEra.CONSOLIDATION;
        }
        return HistoryEra.LEGACY;
    }
}
