import { BoardViewModelService } from './board-view-model.service';

describe('BoardViewModelService', () => {
  let service: BoardViewModelService;

  beforeEach(() => {
    service = new BoardViewModelService();
  });

  it('maps normalized coordinates into board percentages', () => {
    const viewModel = service.fromHumans([
      { id: 1, name: 'Ada', x: 0.25, y: 0.75, busy: false } as any,
    ]);

    expect(viewModel.population).toBe(1);
    expect(viewModel.markers[0].leftPct).toBe(25);
    expect(viewModel.markers[0].topPct).toBe(75);
    expect(viewModel.markers[0].state).toBe('active');
  });

  it('clamps out-of-range coordinates and uses deterministic fallback for null values', () => {
    const viewModel = service.fromHumans([
      { id: 1, name: 'Ada', x: -2, y: 3, busy: false } as any,
      { id: 2, name: 'Ben', x: null, y: null, busy: true } as any,
    ]);

    expect(viewModel.markers[0].leftPct).toBe(2);
    expect(viewModel.markers[0].topPct).toBe(98);
    expect(viewModel.markers[1].leftPct).toBeGreaterThan(2);
    expect(viewModel.markers[1].topPct).toBeGreaterThan(2);
    expect(viewModel.markers[1].state).toBe('busy');
  });
});
