import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  BoardMarkerViewModel,
  BoardPlaceViewModel,
} from '../../services/board-view-model.service';

@Component({
  selector: 'app-simulation-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './simulation-board.component.html',
  styleUrl: './simulation-board.component.scss',
})
export class SimulationBoardComponent {
  @Input() markers: BoardMarkerViewModel[] = [];
  @Input() places: BoardPlaceViewModel[] = [];
  @Input() selectedHumanId: number | null = null;
  @Output() markerSelected = new EventEmitter<number>();

  trackByMarkerId(_: number, marker: BoardMarkerViewModel): number {
    return marker.id;
  }

  onMarkerClick(markerId: number): void {
    this.markerSelected.emit(markerId);
  }
}
