import {Component, input, OnInit} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {CityOutput} from '@api';

@Component({
  selector: 'app-city-card',
  imports: [
    MatButtonModule,
    MatCardModule
  ],
  templateUrl: './city-card.component.html',
  styleUrl: './city-card.component.scss'
})
export class CityCardComponent implements OnInit {
  city = input.required<CityOutput>();

  ngOnInit() {

  }

  
}
