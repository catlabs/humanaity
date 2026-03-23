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
    INVENTION_EMERGED(EventCategory.MILESTONE),
    TRIBE_PLACE_DISCOVERED(EventCategory.DISCOVERY),
    TRIBE_DISCOVERY_REPORTED(EventCategory.DISCOVERY),
    TRIBE_SCOUT_REPORT(EventCategory.INTERACTION),
    TRIBE_PLAN_CHOSEN(EventCategory.INTERACTION),
    TRIBE_GROUP_TRAVEL_COORDINATED(EventCategory.INTERACTION);

    private final EventCategory category;

    EventType(EventCategory category) {
        this.category = category;
    }

    public EventCategory getCategory() {
        return category;
    }
}
