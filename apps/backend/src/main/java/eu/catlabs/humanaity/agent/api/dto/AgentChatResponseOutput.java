package eu.catlabs.humanaity.agent.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentChatResponseOutput {

    private String conversationId;
    private String message;
    private String commandClass;
    private String interpretationProvenance;
    private String interpretedCommandSummary;
    private List<AgentActionOutput> executedActions = new ArrayList<>();
    private AgentReferencedEntitiesOutput referencedEntities = new AgentReferencedEntitiesOutput();
    private List<AgentUiEffectOutput> uiEffects = new ArrayList<>();
    private Map<String, Object> structuredData;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCommandClass() {
        return commandClass;
    }

    public void setCommandClass(String commandClass) {
        this.commandClass = commandClass;
    }

    public String getInterpretationProvenance() {
        return interpretationProvenance;
    }

    public void setInterpretationProvenance(String interpretationProvenance) {
        this.interpretationProvenance = interpretationProvenance;
    }

    public String getInterpretedCommandSummary() {
        return interpretedCommandSummary;
    }

    public void setInterpretedCommandSummary(String interpretedCommandSummary) {
        this.interpretedCommandSummary = interpretedCommandSummary;
    }

    public List<AgentActionOutput> getExecutedActions() {
        return executedActions;
    }

    public void setExecutedActions(List<AgentActionOutput> executedActions) {
        this.executedActions = executedActions;
    }

    public AgentReferencedEntitiesOutput getReferencedEntities() {
        return referencedEntities;
    }

    public void setReferencedEntities(AgentReferencedEntitiesOutput referencedEntities) {
        this.referencedEntities = referencedEntities;
    }

    public List<AgentUiEffectOutput> getUiEffects() {
        return uiEffects;
    }

    public void setUiEffects(List<AgentUiEffectOutput> uiEffects) {
        this.uiEffects = uiEffects;
    }

    public Map<String, Object> getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(Map<String, Object> structuredData) {
        this.structuredData = structuredData;
    }
}
