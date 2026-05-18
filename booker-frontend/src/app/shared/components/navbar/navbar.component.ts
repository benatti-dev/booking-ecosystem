import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';
import { Store } from '@ngrx/store';
import { AuthActions } from '../../../store/auth/auth.actions';
import { selectIsAuthenticated, selectUserEmail } from '../../../store/auth/auth.selectors';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, AsyncPipe, NgIf],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  private readonly store = inject(Store);

  readonly isAuthenticated$ = this.store.select(selectIsAuthenticated);
  readonly userEmail$       = this.store.select(selectUserEmail);

  logout(): void {
    this.store.dispatch(AuthActions.logout());
  }
}