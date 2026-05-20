# Phase 1 — Authentication & RBAC

## Overview

Core authentication module with JWT, role management and audit logging.

## Implemented

### Backend (`com.booker.auth`)

| Component | Class | Status |
|-----------|-------|--------|
| Controller | `AuthController` | ✅ |
| Service | `AuthService` | ✅ |
| Refresh tokens | `RefreshTokenService` | ✅ |
| Audit | `AuditLogService` | ✅ |
| UserDetails | `AppUserDetailsService` | ✅ |
| Security config | `SecurityConfig` | ✅ |

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login, returns JWT |
| `POST` | `/auth/refresh` | Refresh access token |
| `POST` | `/auth/logout` | Revoke refresh token |
| `POST` | `/auth/forgot-password` | Send password reset email |
| `POST` | `/auth/reset-password` | Reset password by token |

### Roles

| Role | Capabilities |
|------|-------------|
| `CLIENT` | Search, book, view own appointments |
| `EMPLOYEE` | View own schedule, confirm bookings |
| `BUSINESS_OWNER` | Manage business, staff, services |
| `ADMIN` | Full access, moderation, analytics |

### Security

- Passwords: **BCrypt** (strength 12)
- JWT secret: from env variable `JWT_SECRET` (min 256-bit base64)
- Refresh tokens: stored as SHA-256 hash in the DB
- Audit: all auth events are logged to `audit_logs`
- Refresh token delivered via `HttpOnly` cookie

### Database Tables

```sql
users                  -- main users table
refresh_tokens         -- refresh tokens (hashed)
password_reset_tokens  -- tokens for password reset
audit_logs             -- auth event audit log
```

## Frontend (`features/auth`)

| Component | Status |
|-----------|--------|
| `LoginComponent` | ✅ |
| `RegisterComponent` | ✅ |
| `AuthGuard` | ✅ |
| `AuthInterceptor` | ✅ |
| NgRx Auth Store | ✅ |

## DTO Contracts

```typescript
// LoginRequest
{ email: string, password: string }

// RegisterRequest
{ email: string, password: string, fullName: string, phone?: string }

// AuthResponse
{ accessToken: string, tokenType: "Bearer", expiresIn: number }
```
