import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CityService } from '../../city.service';

@Component({
  selector: 'app-create-city',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './create-city.page.html',
  styleUrl: './create-city.page.scss'
})
export class CreateCityPage {
  private fb = inject(FormBuilder);
  private cityService = inject(CityService);
  private router = inject(Router);

  cityForm: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor() {
    this.cityForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]]
    });
  }

  onSubmit(): void {
    if (this.cityForm.valid) {
      this.errorMessage = '';
      this.isLoading = true;
      const { name } = this.cityForm.value;

      this.cityService.createCity({ name }).subscribe({
        next: (city) => {
          this.isLoading = false;
          // Navigate to the newly created city details page
          this.router.navigate(['/cities', city.id]);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || 'Failed to create city. Please try again.';
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.cityForm.controls).forEach(key => {
      const control = this.cityForm.get(key);
      control?.markAsTouched();
    });
  }

  getErrorMessage(fieldName: string): string {
    const control = this.cityForm.get(fieldName);
    if (control?.hasError('required')) {
      return `${fieldName} is required`;
    }
    if (control?.hasError('minlength')) {
      return 'City name must be at least 1 character';
    }
    if (control?.hasError('maxlength')) {
      return 'City name must not exceed 100 characters';
    }
    return '';
  }
}

