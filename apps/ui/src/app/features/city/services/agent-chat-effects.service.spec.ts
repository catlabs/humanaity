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
      { type: 'MARK_EVENT', eventId: 42 },
      { type: 'TRACK_HUMAN', humanId: 7 },
      { type: 'BOARD_INTERVENTION_PENDING', humanId: 7 },
    ];

    const resolved = service.resolve(effects);
    expect(resolved.refreshSnapshot).toBeTrue();
    expect(resolved.refreshTimeline).toBeTrue();
    expect(resolved.selectedEventId).toBe(42);
    expect(resolved.trackedHumanId).toBe(7);
    expect(resolved.directorInterventionState).toBe('pending');
    expect(resolved.selectedHumanId).toBeNull();
    expect(resolved.selectedInventionId).toBeNull();
  });

  it('resolves drawer and place highlighting effects with filtered event ids', () => {
    const effects: AgentUiEffectOutput[] = [
      {
        type: 'OPEN_EVENTS_DRAWER',
        eventType: 'DISCOVERY_UNLOCKED',
        eventIds: [11, 12, 'oops' as any],
      },
      { type: 'HIGHLIGHT_PLACE', placeId: 'river' },
    ];

    const resolved = service.resolve(effects);

    expect(resolved.openEventsDrawer).toBeTrue();
    expect(resolved.drawerEventType).toBe('DISCOVERY_UNLOCKED');
    expect(resolved.drawerEventIds).toEqual([11, 12]);
    expect(resolved.highlightedPlaceId).toBe('river');
    expect(resolved.refreshSnapshot).toBeFalse();
    expect(resolved.refreshTimeline).toBeFalse();
  });
});
