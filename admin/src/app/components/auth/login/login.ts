import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { toApiProblem } from '../../../services/api-error';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  /** ?caducada=1 cuando se llega aquí porque no se pudo renovar la sesión. */
  readonly caducada = input<string>();

  protected readonly form = inject(FormBuilder).nonNullable.group({
    identificador: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    if (this.auth.isAdmin()) {
      this.router.navigate(['/']);
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { identificador, password } = this.form.getRawValue();

    this.auth.login(identificador.trim(), password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (cause: unknown) => {
        this.error.set(toApiProblem(cause).message);
        this.submitting.set(false);
      },
    });
  }
}
