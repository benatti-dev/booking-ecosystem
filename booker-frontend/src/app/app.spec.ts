import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { App } from './app';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { provideMockStore } from '@ngrx/store/testing';
import { NotificationService } from './core/notification/notification.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [App, NavbarComponent],
      imports: [RouterTestingModule, HttpClientTestingModule],
      providers: [
        provideMockStore({}),
        NotificationService,
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
