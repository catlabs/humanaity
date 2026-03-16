package eu.catlabs.humanaity.agent.application.command;

public record FallbackCommandMatch(
        FallbackCommandMatchStatus status,
        AgentChatCommand command,
        String reason
) {

    public static FallbackCommandMatch matched(AgentChatCommand command) {
        return new FallbackCommandMatch(FallbackCommandMatchStatus.MATCHED, command, null);
    }

    public static FallbackCommandMatch invalid(String reason) {
        return new FallbackCommandMatch(FallbackCommandMatchStatus.INVALID, null, reason);
    }

    public static FallbackCommandMatch notApplicable() {
        return new FallbackCommandMatch(FallbackCommandMatchStatus.NOT_APPLICABLE, null, null);
    }
}
