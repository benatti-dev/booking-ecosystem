# Phase 4 — Search, Notifications & Real-Time

## Overview

Geo-search for businesses and services, email/in-app notifications, WebSocket real-time push.

## Status: ⏳ Planned

## Search Architecture

### Geo-Search (PostGIS)

```sql
SELECT b.id, b.name,
       ST_Distance(br.location, ST_MakePoint(:lng, :lat)::geography) AS distance_m
FROM businesses b
JOIN branches br ON br.business_id = b.id AND br.status = 'ACTIVE'
WHERE b.status = 'ACTIVE'
  AND ST_DWithin(br.location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
ORDER BY distance_m ASC;
```

### Full-Text Search (PostgreSQL tsvector)

```sql
SELECT s.* FROM services s
WHERE s.search_vector @@ plainto_tsquery('simple', :query)
ORDER BY ts_rank(s.search_vector, plainto_tsquery('simple', :query)) DESC;
```

## Search Endpoints

| Method | Path | Parameters |
|--------|------|------------|
| `GET` | `/search/businesses` | lat, lng, radiusKm, categoryId, query, page, size |
| `GET` | `/search/services` | businessId, query, page, size |

## Notifications

### Email (Thymeleaf templates)

| Template | Trigger |
|----------|---------|
| `booking-confirmation.html` | Booking confirmed |
| `booking-reminder.html` | 24h before appointment |
| `booking-cancellation.html` | Booking cancelled |
| `password-reset.html` | Password reset request |
| `business-approved.html` | Business activated |

### In-App Notifications

```sql
notifications -- user_id, type, title, body, is_read, reference_id
```

### WebSocket (STOMP)

```
Endpoint: ws://.../ws
Subscribe: /user/queue/notifications
```

### Event Listener Pattern

```java
@EventListener
@Async
public void onBookingConfirmed(BookingConfirmedEvent event) {
    emailService.sendBookingConfirmation(event.booking());
    inAppService.create(...);
    messagingTemplate.convertAndSendToUser(...);
}
```

## Backend Components

| Class | Responsibility |
|-------|---------------|
| `SearchController` | Search endpoints |
| `SearchService` | Orchestrates geo + text + filter queries |
| `EmailService` | Sends HTML emails |
| `InAppNotificationService` | Persists notifications to DB |
| `NotificationController` | GET/PATCH notifications |
| `BookingNotificationListener` | Event listeners |
| `BookingReminderScheduler` | @Scheduled cron job |

## Frontend Components

| Component | Description |
|-----------|-------------|
| `HomeSearchComponent` | Search bar + categories + map |
| `SearchResultsComponent` | Business list with filters |
| `NotificationCenterComponent` | Notification dropdown in navbar |
| WebSocket service | Connection and subscription |
