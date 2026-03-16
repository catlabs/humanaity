package eu.catlabs.humanaity.agent.application.command;

public record AgentChatCommand(
        AgentChatCommandType type,
        Integer stepCount,
        Long primaryHumanId,
        Long secondaryHumanId,
        String placeId
) {

    public static AgentChatCommand step(int count) {
        return new AgentChatCommand(AgentChatCommandType.STEP_SIMULATION, count, null, null, null);
    }

    public static AgentChatCommand pause() {
        return new AgentChatCommand(AgentChatCommandType.PAUSE_SIMULATION, null, null, null, null);
    }

    public static AgentChatCommand focusHuman(Long humanId) {
        return new AgentChatCommand(AgentChatCommandType.FOCUS_HUMAN, null, humanId, null, null);
    }

    public static AgentChatCommand moveToPlace(Long humanId, String placeId) {
        return new AgentChatCommand(AgentChatCommandType.MOVE_TO_PLACE, null, humanId, null, placeId);
    }

    public static AgentChatCommand meetHuman(Long humanId, Long otherHumanId) {
        return new AgentChatCommand(AgentChatCommandType.MEET_HUMAN, null, humanId, otherHumanId, null);
    }
}
