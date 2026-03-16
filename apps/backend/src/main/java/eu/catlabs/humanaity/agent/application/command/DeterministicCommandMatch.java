package eu.catlabs.humanaity.agent.application.command;

public record DeterministicCommandMatch(
        DeterministicCommandMatchStatus status,
        AgentChatCommand command,
        String reason
) {

    public static DeterministicCommandMatch matched(AgentChatCommand command) {
        return new DeterministicCommandMatch(DeterministicCommandMatchStatus.MATCHED, command, null);
    }

    public static DeterministicCommandMatch unsupported(String reason) {
        return new DeterministicCommandMatch(DeterministicCommandMatchStatus.UNSUPPORTED, null, reason);
    }

    public static DeterministicCommandMatch ambiguous(String reason) {
        return new DeterministicCommandMatch(DeterministicCommandMatchStatus.AMBIGUOUS, null, reason);
    }
}
