package eu.catlabs.humanaity.agent.api.dto;

public class AgentChatRequestInput {

    private String message;
    private String conversationId;
    private Long selectedHumanId;
    private Long selectedEventId;
    private Long selectedInventionId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSelectedHumanId() {
        return selectedHumanId;
    }

    public void setSelectedHumanId(Long selectedHumanId) {
        this.selectedHumanId = selectedHumanId;
    }

    public Long getSelectedEventId() {
        return selectedEventId;
    }

    public void setSelectedEventId(Long selectedEventId) {
        this.selectedEventId = selectedEventId;
    }

    public Long getSelectedInventionId() {
        return selectedInventionId;
    }

    public void setSelectedInventionId(Long selectedInventionId) {
        this.selectedInventionId = selectedInventionId;
    }
}
