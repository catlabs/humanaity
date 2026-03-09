import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../auth.service';
import { SignupRequest } from '@api';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    RouterLink
  ],
  templateUrl: './signup.page.html',
  styleUrl: './signup.page.scss'
})
export class SignupPage {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  signupForm: FormGroup;
  errorMessage: string = '';
  hidePassword = true;
  hideConfirmPassword = true;

  constructor() {
    this.signupForm = this.fb.group({
      username: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.signupForm.valid) {
      this.errorMessage = '';
      const { email, password, confirmPassword } = this.signupForm.value;

      const signupRequest: SignupRequest = { email, password, confirmPassword };
      this.authService.signup(signupRequest).subscribe({
        next: () => {
          this.router.navigate(['/cities']);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'An error occurred during signup';
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.signupForm.controls).forEach(key => {
      const control = this.signupForm.get(key);
      control?.markAsTouched();
    });
  }

  getErrorMessage(fieldName: string): string {
    const control = this.signupForm.get(fieldName);
    if (control?.hasError('required')) {
      return `${fieldName} is required`;
    }
    if (control?.hasError('email')) {
      return 'Invalid email format';
    }
    if (control?.hasError('minlength')) {
      return 'Password must be at least 6 characters';
    }
    return '';
  }

  getPasswordMismatchError(): string {
    if (this.signupForm.hasError('passwordMismatch') && this.signupForm.get('confirmPassword')?.touched) {
      return 'Passwords do not match';
    }
    return '';
  }
}

