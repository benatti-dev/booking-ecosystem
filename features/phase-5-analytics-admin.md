# Phase 5 — Analytics & Admin Panel

## Overview

Analytics dashboard for business owners and platform-level admin panel.

## Status: ✅ Complete

## Business Analytics

### Key Metrics

- Bookings per day (last 30 days)
- Revenue by service
- Employee utilization
- Cancellation rate

### SQL Example

```sql
SELECT DATE(start_time AT TIME ZONE b.timezone) AS day,
       COUNT(*) AS total_bookings,
       SUM(price_snapshot) AS revenue
FROM bookings bk
JOIN businesses b ON bk.business_id = b.id
WHERE bk.business_id = :businessId
  AND bk.status = 'COMPLETED'
  AND bk.start_time >= NOW() - INTERVAL '30 days'
GROUP BY day
ORDER BY day;
```

## Admin Panel

### Capabilities

| Feature | Endpoint |
|---------|----------|
| List businesses by status | `GET /admin/businesses?status=` |
| List users | `GET /admin/users` |
| Suspend/restore user | `PATCH /admin/users/{id}/status` |
| Platform overview stats | `GET /admin/analytics/overview` |
| Audit log | `GET /admin/audit-logs` |

### Platform Metrics

- Total businesses (active/pending/suspended)
- Total users per role
- Total bookings per day
- Platform revenue (if platform fee model applies)
- Registration trends

## Backend Components

| Class | Responsibility |
|-------|---------------|
| `AnalyticsController` | Business + admin analytics endpoints |
| `AdminController` | User/business management |
| `AnalyticsService` | Aggregate query execution |

## Frontend Components

| Component | Description |
|-----------|-------------|
| Business analytics dashboard | Chart.js/ngx-charts visualizations |
| Admin businesses table | Approve/reject/suspend actions |
| Admin users table | Role/status management |
| Audit log viewer | Filter by date/action |
