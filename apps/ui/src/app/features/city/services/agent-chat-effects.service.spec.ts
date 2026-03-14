import { AgentUiEffectOutput } from '@api';
import { AgentChatEffectsService } from './agent-chat-effects.service';

describe('AgentChatEffectsService', () => {
  let service: AgentChatEffectsService;

  beforeEach(() => {
    service = new AgentChatEffectsService();
  });

  it('resolves refresh flags and highlighted event', () => {
    const effects: AgentUiEffectOutput[] = [
      { type: 'REFRESH_SNAPSHOT' },
      { type: 'REFRESH_TIMELINE' },
      { type: 'HIGHLIGHT_EVENT', eventId: 42 },
    ];

    const resolved = service.resolve(effects);
    expect(resolved.refreshSnapshot).toBeTrue();
    expect(resolved.refreshTimeline).toBeTrue();
    expect(resolved.selectedEventId).toBe(42);
    expect(resolved.selectedHumanId).toBeNull();
    expect(resolved.selectedInventionId).toBeNull();
  });
});
