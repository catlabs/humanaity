import { Injectable } from '@angular/core';
import { AgentUiEffectOutput } from '@api';

export type AgentChatEffectResolution = {
  refreshSnapshot: boolean;
  refreshTimeline: boolean;
  selectedHumanId: number | null;
  trackedHumanId: number | null;
  selectedEventId: number | null;
  selectedInventionId: number | null;
  directorInterventionState: 'pending' | 'executed' | null;
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
      trackedHumanId: null,
      selectedEventId: null,
      selectedInventionId: null,
      directorInterventionState: null,
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
            resolution.trackedHumanId = effect.humanId;
            resolution.selectedEventId = null;
            resolution.selectedInventionId = null;
          }
          break;
        case 'TRACK_HUMAN':
          if (typeof effect.humanId === 'number') {
            resolution.trackedHumanId = effect.humanId;
          }
          break;
        case 'HIGHLIGHT_EVENT':
        case 'MARK_EVENT':
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
        case 'BOARD_INTERVENTION_PENDING':
          resolution.directorInterventionState = 'pending';
          if (typeof effect.humanId === 'number') {
            resolution.trackedHumanId = effect.humanId;
          }
          break;
        case 'BOARD_INTERVENTION_EXECUTED':
          resolution.directorInterventionState = 'executed';
          if (typeof effect.humanId === 'number') {
            resolution.trackedHumanId = effect.humanId;
          }
          break;
        default:
          break;
      }
    }

    return resolution;
  }
}
