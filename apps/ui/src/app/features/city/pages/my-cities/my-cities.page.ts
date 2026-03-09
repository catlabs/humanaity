import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CityOutput } from '@api';
import { CityCardComponent } from '../../components/card/city-card.component';

@Component({
  selector: 'app-my-cities',
  standalone: true,
  imports: [
    CommonModule,
    CityCardComponent,
    MatButtonModule,
    MatCardModule,
    RouterLink
  ],
  templateUrl: './my-cities.page.html',
  styleUrl: './my-cities.page.scss'
})
export class MyCitiesPage {
  private route = inject(ActivatedRoute);

  cities = signal<CityOutput[]>(this.route.snapshot.data['cities'] || []);
}

