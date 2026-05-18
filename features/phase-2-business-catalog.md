# Phase 2 — Business & Catalog

## Overview

Business registration and management: businesses, branches, service catalog with dynamic attributes.

## Implemented

### Backend

#### Business module (`com.booker.business`)

| Component | Class | Status |
|-----------|-------|--------|
| Business controller | `BusinessController` | ✅ |
| Branch controller | `BranchController` | ✅ |
| Category controller | `BusinessCategoryController` | ✅ |
| Business service | `BusinessService` | ✅ |
| Branch service | `BranchService` | ✅ |

#### Catalog module (`com.booker.catalog`)

| Component | Class | Status |
|-----------|-------|--------|
| Service controller | `ServiceController` | ✅ |
| Attribute controller | `AttributeDefinitionController` | ✅ |
| Attribute validator | `ServiceAttributeValidator` | ✅ |

### Business Status Machine

```
PENDING ──(admin approve)──► ACTIVE
PENDING ──(admin reject)───► REJECTED
ACTIVE  ──(admin suspend)──► SUSPENDED
SUSPENDED ─(admin restore)─► ACTIVE
```

A `BusinessActivatedEvent` is published on transition to `ACTIVE`.

### Dynamic Service Attributes

```
POST /businesses/{id}/services
  body: { name, duration, attributes: { "hair_length": "long", "coloring": true } }
    → ServiceAttributeValidator.validate(categoryId, attributes)
       → Loads definitions for the category (cached)
       → Checks required fields
       → Validates types (string, boolean, valid option)
       → Throws ValidationException on failure
```

### Database Tables

```sql
business_categories           -- business category types
businesses                    -- businesses
branches                      -- branches (with PostGIS geo point)
services                      -- services
service_attribute_definitions -- dynamic attribute schema
service_employees             -- M2M: service ↔ employee (V3)
service_resources             -- M2M: service ↔ resource (V3)
```

### Endpoints

| Method | Path | Role |
|--------|------|------|
| `POST` | `/businesses` | `BUSINESS_OWNER` |
| `GET` | `/businesses/{id}` | Public |
| `PUT` | `/businesses/{id}` | `OWNER/ADMIN` |
| `PATCH` | `/businesses/{id}/status` | `ADMIN` |
| `GET/POST` | `/businesses/{id}/branches` | Public/OWNER |
| `POST` | `/businesses/{id}/services` | `OWNER` |
| `GET` | `/businesses/{id}/services` | Public |
| `GET` | `/categories` | Public |
| `POST` | `/admin/categories` | `ADMIN` |

## Frontend

| Component | Status |
|-----------|--------|
| BusinessRegistrationComponent (multi-step) | ✅ |
| ServiceListComponent | ✅ |
| ServiceFormComponent (dynamic fields) | ✅ |
| Admin: pending businesses | ✅ |
| My Businesses | ✅ |
