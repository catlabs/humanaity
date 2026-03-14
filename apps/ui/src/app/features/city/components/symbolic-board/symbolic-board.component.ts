import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BoardMarkerViewModel } from '../../services/board-view-model.service';

@Component({
  selector: 'app-symbolic-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './symbolic-board.component.html',
  styleUrl: './symbolic-board.component.scss',
})
export class SymbolicBoardComponent {
  @Input() markers: BoardMarkerViewModel[] = [];
  @Input() selectedHumanId: number | null = null;
  @Input() trackedHumanId: number | null = null;
  @Output() markerSelected = new EventEmitter<number>();

  trackByMarkerId(_: number, marker: BoardMarkerViewModel): number {
    return marker.id;
  }

  onMarkerClick(markerId: number): void {
    this.markerSelected.emit(markerId);
  }
}
