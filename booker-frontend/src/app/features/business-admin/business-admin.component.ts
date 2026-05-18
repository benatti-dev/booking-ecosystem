import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-business-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './business-admin.component.html',
  styleUrl: './business-admin.component.scss'
})
export class BusinessAdminComponent {
  sidenavOpen = true;

  navItems = [
    { label: 'My Businesses', icon: '🏢', path: 'my-businesses' },
    { label: 'Register Business', icon: '➕', path: 'register' },
  ];
}