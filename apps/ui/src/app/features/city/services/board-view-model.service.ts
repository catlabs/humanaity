import { Injectable } from '@angular/core';
import { SimulationSnapshotOutput } from '@api';

export type BoardMarkerState = 'active' | 'busy';

export type BoardMarkerViewModel = {
  id: number;
  name: string;
  leftPct: number;
  topPct: number;
  state: BoardMarkerState;
};

export type BoardViewModel = {
  markers: BoardMarkerViewModel[];
  population: number;
};

export type BoardPlaceViewModel = {
  id: string;
  label: string;
  leftPct: number;
  topPct: number;
};

export type BoardInteractionViewModel = {
  key: string;
  fromLeftPct: number;
  fromTopPct: number;
  toLeftPct: number;
  toTopPct: number;
  kind: 'collision' | 'dialogue';
};

export type BoardEventMarkerViewModel = {
  eventId: number;
  leftPct: number;
  topPct: number;
  tone: 'milestone' | 'interaction';
  label: string;
};

@Injectable({
  providedIn: 'root',
})
export class BoardViewModelService {
  fromHumans(humans: SimulationSnapshotOutput['humans']): BoardViewModel {
    const markers = humans.map((human, index) => ({
      id: human.id,
      name: human.name,
      leftPct: this.normalizeCoordinate(human.x, index, true),
      topPct: this.normalizeCoordinate(human.y, index, false),
      state: human.busy ? ('busy' as const) : ('active' as const),
    }));
    return {
      markers,
      population: humans.length,
    };
  }

  private normalizeCoordinate(
    value: number | null | undefined,
    index: number,
    horizontal: boolean
  ): number {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return this.clamp(value * 100, 2, 98);
    }
    return this.fallbackCoordinate(index, horizontal);
  }

  private fallbackCoordinate(index: number, horizontal: boolean): number {
    const base = horizontal ? 13 : 17;
    const stride = horizontal ? 19 : 23;
    const offset = (base + index * stride) % 86;
    return this.clamp(offset + 7, 2, 98);
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }
}
