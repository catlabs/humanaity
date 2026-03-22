package eu.catlabs.humanaity.human.application;

import eu.catlabs.humanaity.human.domain.Human;

public final class HumanDisplayNameFormatter {

    private HumanDisplayNameFormatter() {
    }

    public static String displayName(Human human) {
        if (human == null) {
            return "Human";
        }
        return displayName(human.getId(), human.getName());
    }

    public static String displayName(Long humanId, String name) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return humanId == null ? "Human" : "Human " + humanId;
    }
}
