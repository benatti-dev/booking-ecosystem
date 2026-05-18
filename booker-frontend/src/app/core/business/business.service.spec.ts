import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  BusinessService,
  BusinessResponse,
  BranchResponse,
  ServiceResponse,
  BusinessCategory,
  Page,
} from './business.service';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

// ── Fixtures ──────────────────────────────────────────────────────────────────

const mockCategory: BusinessCategory = {
  id: 1,
  name: 'beauty',
  label: 'Beauty Salon',
  resourceType: 'EMPLOYEE',
};

const mockBusiness: BusinessResponse = {
  id: 2,
  ownerId: 10,
  ownerName: 'Alice',
  category: { id: 1, name: 'beauty', label: 'Beauty Salon', resourceType: 'EMPLOYEE' },
  name: 'Glam Studio',
  status: 'PENDING',
  meta: {},
  createdAt: '2026-05-18T00:00:00Z',
  updatedAt: '2026-05-18T00:00:00Z',
};

const mockBranch: BranchResponse = {
  id: 3,
  businessId: 2,
  name: 'Main Branch',
  address: '123 Main St',
  city: 'Kyiv',
  country: 'UA',
  timezone: 'Europe/Kiev',
  isPrimary: true,
  status: 'ACTIVE',
  createdAt: '2026-05-18T00:00:00Z',
};

const mockService: ServiceResponse = {
  id: 5,
  businessId: 2,
  categoryId: 1,
  categoryName: 'beauty',
  name: 'Haircut',
  durationMin: 60,
  price: 500,
  currency: 'UAH',
  attributes: {},
  isActive: true,
  createdAt: '2026-05-18T00:00:00Z',
  updatedAt: '2026-05-18T00:00:00Z',
};

const mockPage = <T>(content: T[]): Page<T> => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20,
});

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('BusinessService', () => {
  let service: BusinessService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BusinessService],
    });
    service = TestBed.inject(BusinessService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ── getCategories ─────────────────────────────────────────────

  describe('getCategories()', () => {
    it('should GET /categories and return list', () => {
      service.getCategories().subscribe(res => {
        expect(res.length).toBe(1);
        expect(res[0].name).toBe('beauty');
      });

      const req = http.expectOne(`${API}/categories`);
      expect(req.request.method).toBe('GET');
      req.flush([mockCategory]);
    });
  });

  // ── createBusiness ────────────────────────────────────────────

  describe('createBusiness()', () => {
    it('should POST /businesses with payload', () => {
      const payload = { categoryId: 1, name: 'Glam Studio' };

      service.createBusiness(payload).subscribe(res => {
        expect(res.name).toBe('Glam Studio');
        expect(res.status).toBe('PENDING');
      });

      const req = http.expectOne(`${API}/businesses`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockBusiness);
    });
  });

  // ── getBusiness ───────────────────────────────────────────────

  describe('getBusiness()', () => {
    it('should GET /businesses/{id}', () => {
      service.getBusiness(2).subscribe(res => {
        expect(res.id).toBe(2);
        expect(res.ownerName).toBe('Alice');
      });

      const req = http.expectOne(`${API}/businesses/2`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBusiness);
    });

    it('should propagate 404 as error', () => {
      let err: any;
      service.getBusiness(999).subscribe({ error: e => (err = e) });

      http.expectOne(`${API}/businesses/999`).flush(
        { message: 'Business not found' },
        { status: 404, statusText: 'Not Found' }
      );

      expect(err.status).toBe(404);
    });
  });

  // ── getMyBusinesses ───────────────────────────────────────────

  describe('getMyBusinesses()', () => {
    it('should GET /businesses/my with pagination', () => {
      service.getMyBusinesses(0, 5).subscribe(res => {
        expect(res.content.length).toBe(1);
      });

      const req = http.expectOne(r =>
        r.url === `${API}/businesses/my` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '5'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([mockBusiness]));
    });
  });

  // ── getBusinessesByStatus ────────────────────────────────────

  describe('getBusinessesByStatus()', () => {
    it('should GET /businesses?status=PENDING', () => {
      service.getBusinessesByStatus('PENDING').subscribe(res => {
        expect(res.content[0].status).toBe('PENDING');
      });

      const req = http.expectOne(r =>
        r.url === `${API}/businesses` &&
        r.params.get('status') === 'PENDING'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([mockBusiness]));
    });
  });

  // ── changeBusinessStatus ──────────────────────────────────────

  describe('changeBusinessStatus()', () => {
    it('should PATCH /businesses/{id}/status', () => {
      const active = { ...mockBusiness, status: 'ACTIVE' as const };

      service.changeBusinessStatus(2, 'ACTIVE').subscribe(res => {
        expect(res.status).toBe('ACTIVE');
      });

      const req = http.expectOne(`${API}/businesses/2/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'ACTIVE', reason: undefined });
      req.flush(active);
    });
  });

  // ── createBranch ──────────────────────────────────────────────

  describe('createBranch()', () => {
    it('should POST /businesses/{id}/branches', () => {
      const payload = { name: 'Main Branch', address: '123 St', city: 'Kyiv' };

      service.createBranch(2, payload).subscribe(res => {
        expect(res.name).toBe('Main Branch');
        expect(res.isPrimary).toBeTrue();
      });

      const req = http.expectOne(`${API}/businesses/2/branches`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockBranch);
    });
  });

  // ── getBranches ───────────────────────────────────────────────

  describe('getBranches()', () => {
    it('should GET /businesses/{id}/branches', () => {
      service.getBranches(2).subscribe(res => {
        expect(res.length).toBe(1);
        expect(res[0].city).toBe('Kyiv');
      });

      const req = http.expectOne(`${API}/businesses/2/branches`);
      expect(req.request.method).toBe('GET');
      req.flush([mockBranch]);
    });
  });

  // ── getServices ───────────────────────────────────────────────

  describe('getServices()', () => {
    it('should GET /businesses/{id}/services with pagination', () => {
      service.getServices(2, 0, 10).subscribe(res => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].name).toBe('Haircut');
      });

      const req = http.expectOne(r =>
        r.url === `${API}/businesses/2/services` &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage([mockService]));
    });
  });

  // ── getAttributeDefinitions ───────────────────────────────────

  describe('getAttributeDefinitions()', () => {
    it('should GET /categories/{id}/attribute-definitions', () => {
      service.getAttributeDefinitions(1).subscribe(res => {
        expect(Array.isArray(res)).toBeTrue();
      });

      const req = http.expectOne(`${API}/categories/1/attribute-definitions`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  // ── Error propagation ─────────────────────────────────────────

  describe('error handling', () => {
    it('createBusiness 403 → error observable', () => {
      let caughtError: any;

      service.createBusiness({ categoryId: 1, name: 'X' }).subscribe({
        error: e => (caughtError = e),
      });

      http.expectOne(`${API}/businesses`).flush(
        { message: 'Forbidden' },
        { status: 403, statusText: 'Forbidden' }
      );

      expect(caughtError.status).toBe(403);
    });
  });
});
