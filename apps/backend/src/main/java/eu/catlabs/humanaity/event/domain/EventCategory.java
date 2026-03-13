package eu.catlabs.humanaity.event.domain;

public enum EventCategory {
    LIFECYCLE(1),
    INTERACTION(2),
    DISCOVERY(3),
    DIALOGUE(4),
    MILESTONE(5);

    private final int precedence;

    EventCategory(int precedence) {
        this.precedence = precedence;
    }

    public int getPrecedence() {
        return precedence;
    }
}
