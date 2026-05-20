# Multi-Tenancy

## Approach: Shared Database, Shared Schema

All businesses are stored in a single database and schema.
Isolation is enforced via a `business_id` foreign key on every tenant row.

## Application-Level Isolation

### Repositories

Every repository always filters by `business_id`:

```java
// Correct — always pass businessId
List<Service> findByBusinessIdAndIsActiveTrue(Long businessId);
Page<Booking> findByBusinessId(Long businessId, Pageable pageable);

// Forbidden — returns data for all tenants
List<Service> findAll(); // NEVER use without a filter
```

### Services

```java
// Verify ownership before any operation
@PreAuthorize("@securityHelper.isBusinessOwner(authentication, #businessId)")
public ServiceResponse createService(Long businessId, CreateServiceRequest req) { ... }
```

### Booking Engine

```java
// Verify the client accesses only their own bookings
booking.getClientId().equals(currentUserId)

// Owner can manage only their own business
booking.getBusinessId().equals(owner.getBusinessId())
```

## Tenant Data

| Table | Isolation field |
|-------|----------------|
| `businesses` | `owner_id` |
| `branches` | `business_id` |
| `employees` | `business_id` |
| `resources` | `business_id` |
| `services` | `business_id` |
| `schedule_rules` | via `employee_id`/`resource_id` → `business_id` |
| `bookings` | `business_id` |

## ADMIN Access

Administrators access all tenants via dedicated admin endpoints:
```
GET /admin/businesses
GET /admin/users
GET /admin/analytics/overview
```

Standard endpoints are also accessible to admins but restricted via `@PreAuthorize`.

## Potential Extension: Row-Level Security (PostgreSQL)

When migrating to microservices, PostgreSQL RLS can be added:
```sql
ALTER TABLE services ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON services
  USING (business_id = current_setting('app.tenant_id')::BIGINT);
```

Not needed currently — application-level isolation is sufficient for the monolithic architecture.
