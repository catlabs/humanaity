import { Injectable } from '@angular/core';
import { AgentUiEffectOutput } from '@api';

export type AgentChatEffectResolution = {
  refreshSnapshot: boolean;
  refreshTimeline: boolean;
  selectedHumanId: number | null;
  selectedEventId: number | null;
  selectedInventionId: number | null;
};

@Injectable({
  providedIn: 'root',
})
export class AgentChatEffectsService {
  resolve(effects: AgentUiEffectOutput[]): AgentChatEffectResolution {
    const resolution: AgentChatEffectResolution = {
      refreshSnapshot: false,
      refreshTimeline: false,
      selectedHumanId: null,
      selectedEventId: null,
      selectedInventionId: null,
    };

    for (const effect of effects) {
      switch (effect.type) {
        case 'REFRESH_SNAPSHOT':
          resolution.refreshSnapshot = true;
          break;
        case 'REFRESH_TIMELINE':
          resolution.refreshTimeline = true;
          break;
        case 'FOCUS_HUMAN':
          if (typeof effect.humanId === 'number') {
            resolution.selectedHumanId = effect.humanId;
            resolution.selectedEventId = null;
            resolution.selectedInventionId = null;
          }
          break;
        case 'HIGHLIGHT_EVENT':
          if (typeof effect.eventId === 'number') {
            resolution.selectedEventId = effect.eventId;
            resolution.selectedHumanId = null;
            resolution.selectedInventionId = null;
          }
          break;
        case 'HIGHLIGHT_INVENTION':
          if (typeof effect.inventionId === 'number') {
            resolution.selectedInventionId = effect.inventionId;
            resolution.selectedHumanId = null;
            resolution.selectedEventId = null;
          }
          break;
        default:
          break;
      }
    }

    return resolution;
  }
}
