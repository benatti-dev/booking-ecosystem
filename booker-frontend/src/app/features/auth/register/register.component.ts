import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { AuthActions } from '../../../store/auth/auth.actions';
import { selectAuthLoading, selectAuthError } from '../../../store/auth/auth.selectors';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  standalone: false,
})
export class RegisterComponent implements OnInit, OnDestroy {
  loading = false;
  error: string | null = null;

  protected readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    email:    ['', [Validators.required, Validators.email]],
    phone:    [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  private sub = new Subscription();

  constructor(
    private readonly fb: FormBuilder,
    private readonly store: Store,
  ) {}

  ngOnInit(): void {
    this.store.dispatch(AuthActions.clearError());
    this.sub.add(this.store.select(selectAuthLoading).subscribe(v => (this.loading = v)));
    this.sub.add(this.store.select(selectAuthError).subscribe(v => (this.error = v)));
  }

  protected isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl.touched);
  }

  protected onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { fullName, email, phone, password } = this.form.value;
    this.store.dispatch(AuthActions.register({
      req: { fullName: fullName!, email: email!, phone: phone || undefined, password: password! },
    }));
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
