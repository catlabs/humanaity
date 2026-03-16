package eu.catlabs.humanaity.event.domain;

public enum EventType {
    SIMULATION_STARTED(EventCategory.LIFECYCLE),
    SIMULATION_PAUSED(EventCategory.LIFECYCLE),
    SIMULATION_RESUMED(EventCategory.LIFECYCLE),
    SIMULATION_COMPLETED(EventCategory.LIFECYCLE),
    HUMANS_COLLIDED(EventCategory.INTERACTION),
    GOAL_ASSIGNED(EventCategory.INTERACTION),
    GOAL_COMPLETED(EventCategory.MILESTONE),
    HUMAN_ACTION_PERFORMED(EventCategory.INTERACTION),
    DISCOVERY_UNLOCKED(EventCategory.DISCOVERY),
    DIALOGUE_EXCHANGED(EventCategory.DIALOGUE),
    INVENTION_EMERGED(EventCategory.MILESTONE);

    private final EventCategory category;

    EventType(EventCategory category) {
        this.category = category;
    }

    public EventCategory getCategory() {
        return category;
    }
}
