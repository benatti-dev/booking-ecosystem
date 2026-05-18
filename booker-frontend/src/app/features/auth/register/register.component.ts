import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';
import { Store } from '@ngrx/store';
import { AuthActions } from '../../../store/auth/auth.actions';
import { selectAuthLoading, selectAuthError } from '../../../store/auth/auth.selectors';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIf, AsyncPipe],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  private readonly fb    = inject(FormBuilder);
  private readonly store = inject(Store);

  readonly loading$ = this.store.select(selectAuthLoading);
  readonly error$   = this.store.select(selectAuthError);

  protected readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    email:    ['', [Validators.required, Validators.email]],
    phone:    [''],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit(): void {
    this.store.dispatch(AuthActions.clearError());
  }

  protected isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { fullName, email, phone, password } = this.form.value;
    this.store.dispatch(AuthActions.register({
      req: { fullName: fullName!, email: email!, phone: phone || undefined, password: password! }
    }));
  }
}
