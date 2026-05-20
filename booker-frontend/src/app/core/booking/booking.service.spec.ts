import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { BookingService, BookingResponse, SlotResponse, EmployeeResponse, Page } from './booking.service';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockBooking: BookingResponse = {
  id: 1,
  clientId: 10,
  clientName: 'Alice',
  serviceId: 5,
  serviceName: 'Haircut',
  businessId: 2,
  businessName: 'Glam Studio',
  branchId: 3,
  branchName: 'Main Branch',
  employeeId: 7,
  employeeName: 'Jane Doe',
  startTime: '2026-06-01T09:00:00Z',
  endTime: '2026-06-01T10:00:00Z',
  status: 'PENDING',
  durationMin: 60,
  selectedAttributes: {},
  createdAt: '2026-05-18T12:00:00Z',
};

const mockEmployee: EmployeeResponse = {
  id: 7,
  businessId: 2,
  displayName: 'Jane Doe',
  isActive: true,
  createdAt: '2026-05-01T00:00:00Z',
};

const mockSlots: SlotResponse = {
  date: '2026-06-01',
  employeeId: 7,
  serviceId: 5,
  durationMin: 60,
  availableSlots: ['09:00:00', '10:00:00', '11:00:00'],
};

const mockPage = <T>(content: T[]): Page<T> => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20,
});

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('BookingService', () => {
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BookingService],
    });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ── getEmployees ──────────────────────────────────────────────

  describe('getEmployees()', () => {
    it('should GET /businesses/{id}/employees and return page', () => {
      const page = mockPage([mockEmployee]);

      service.getEmployees(2).subscribe(res => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].displayName).toBe('Jane Doe');
      });

      const req = http.expectOne(`${API}/businesses/2/employees`);
      expect(req.request.method).toBe('GET');
      req.flush(page);
    });
  });

  // ── getSlots ─────────────────────────────────────────────────

  describe('getSlots()', () => {
    it('should GET /slots with required params', () => {
      service.getSlots(5, '2026-06-01').subscribe(res => {
        expect(res.availableSlots).toEqual(['09:00:00', '10:00:00', '11:00:00']);
      });

      const req = http.expectOne(r =>
        r.url === `${API}/slots` &&
        r.params.get('serviceId') === '5' &&
        r.params.get('date') === '2026-06-01'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockSlots);
    });

    it('should include employeeId param when provided', () => {
      service.getSlots(5, '2026-06-01', 7).subscribe();

      const req = http.expectOne(r =>
        r.url === `${API}/slots` &&
        r.params.get('employeeId') === '7'
      );
      req.flush(mockSlots);
    });

    it('should include resourceId param when provided', () => {
      service.getSlots(5, '2026-06-01', undefined, 12).subscribe();

      const req = http.expectOne(r =>
        r.url === `${API}/slots` &&
        r.params.get('resourceId') === '12'
      );
      req.flush(mockSlots);
    });
  });

  // ── createBooking ─────────────────────────────────────────────

  describe('createBooking()', () => {
    it('should POST /bookings with request body', () => {
      const req = {
        serviceId: 5,
        employeeId: 7,
        branchId: 3,
        startTime: '2026-06-01T09:00:00Z',
      };

      service.createBooking(req).subscribe(res => {
        expect(res.id).toBe(1);
        expect(res.status).toBe('PENDING');
      });

      const httpReq = http.expectOne(`${API}/bookings`);
      expect(httpReq.request.method).toBe('POST');
      expect(httpReq.request.body).toEqual(req);
      httpReq.flush(mockBooking);
    });
  });

  // ── getMyBookings ─────────────────────────────────────────────

  describe('getMyBookings()', () => {
    it('should GET /bookings/my with pagination', () => {
      service.getMyBookings(0, 10).subscribe(res => {
        expect(res.content.length).toBe(1);
      });

      const req = http.expectOne(r =>
        r.url === `${API}/bookings/my` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([mockBooking]));
    });

    it('should default to page=0, size=20', () => {
      service.getMyBookings().subscribe();

      const req = http.expectOne(r =>
        r.url === `${API}/bookings/my` &&
        r.params.get('size') === '20'
      );
      req.flush(mockPage([]));
    });
  });

  // ── getBookingById ────────────────────────────────────────────

  describe('getBookingById()', () => {
    it('should GET /bookings/{id}', () => {
      service.getBookingById(1).subscribe(res => {
        expect(res.id).toBe(1);
      });

      const req = http.expectOne(`${API}/bookings/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBooking);
    });
  });

  // ── cancelBooking ─────────────────────────────────────────────

  describe('cancelBooking()', () => {
    it('should PATCH /bookings/{id}/cancel with reason', () => {
      const cancelled = { ...mockBooking, status: 'CANCELLED' as const };

      service.cancelBooking(1, 'Changed plans').subscribe(res => {
        expect(res.status).toBe('CANCELLED');
      });

      const req = http.expectOne(`${API}/bookings/1/cancel`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ reason: 'Changed plans' });
      req.flush(cancelled);
    });

    it('should send undefined reason when omitted', () => {
      service.cancelBooking(1).subscribe();

      const req = http.expectOne(`${API}/bookings/1/cancel`);
      expect(req.request.body).toEqual({ reason: undefined });
      req.flush({ ...mockBooking, status: 'CANCELLED' });
    });
  });

  // ── confirmBooking ────────────────────────────────────────────

  describe('confirmBooking()', () => {
    it('should PATCH /bookings/{id}/confirm', () => {
      const confirmed = { ...mockBooking, status: 'CONFIRMED' as const };

      service.confirmBooking(1).subscribe(res => {
        expect(res.status).toBe('CONFIRMED');
      });

      const req = http.expectOne(`${API}/bookings/1/confirm`);
      expect(req.request.method).toBe('PATCH');
      req.flush(confirmed);
    });
  });

  // ── completeBooking ───────────────────────────────────────────

  describe('completeBooking()', () => {
    it('should PATCH /bookings/{id}/complete', () => {
      const completed = { ...mockBooking, status: 'COMPLETED' as const };

      service.completeBooking(1).subscribe(res => {
        expect(res.status).toBe('COMPLETED');
      });

      const req = http.expectOne(`${API}/bookings/1/complete`);
      expect(req.request.method).toBe('PATCH');
      req.flush(completed);
    });
  });

  // ── getBusinessBookings ────────────────────────────────────────

  describe('getBusinessBookings()', () => {
    it('should GET /bookings/business/{id} with pagination', () => {
      service.getBusinessBookings(2, undefined, undefined, 0, 5).subscribe(res => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].businessId).toBe(2);
      });

      const req = http.expectOne(r =>
        r.url === `${API}/bookings/business/2` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '5'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([mockBooking]));
    });

    it('should include from/to params when provided', () => {
      service.getBusinessBookings(2, '2026-06-01T00:00:00Z', '2026-06-30T23:59:59Z').subscribe();

      const req = http.expectOne(r =>
        r.url === `${API}/bookings/business/2` &&
        r.params.get('from') === '2026-06-01T00:00:00Z' &&
        r.params.get('to') === '2026-06-30T23:59:59Z'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([]));
    });
  });

  // ── Error propagation ─────────────────────────────────────────

  describe('error handling', () => {
    it('createBooking 409 → observable errors with HttpErrorResponse', () => {
      let caughtError: any;

      service.createBooking({ serviceId: 5, branchId: 3, startTime: 'X' }).subscribe({
        error: err => (caughtError = err),
      });

      http.expectOne(`${API}/bookings`).flush(
        { message: 'Slot no longer available' },
        { status: 409, statusText: 'Conflict' }
      );

      expect(caughtError.status).toBe(409);
    });

    it('getBookingById 404 → observable errors', () => {
      let caughtError: any;

      service.getBookingById(999).subscribe({ error: e => (caughtError = e) });

      http.expectOne(`${API}/bookings/999`).flush(
        { message: 'Booking not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(caughtError.status).toBe(404);
    });
  });
});
