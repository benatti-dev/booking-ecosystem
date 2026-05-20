# Email Notifications — Mock Service & SMTP Migration Guide

## Overview

Booker implements a **two-tier email abstraction**: a `MockEmailService` that logs
all outgoing messages (default in development/CI) and a thin interface that can be
swapped for a real SMTP sender without changing any caller code.

---

## Current Implementation

### Interface

```java
// com.booker.notification.service.EmailService
public interface EmailService {
    void sendBookingCreated(Booking booking);
    void sendBookingConfirmed(Booking booking);
    void sendBookingCancelled(Booking booking, String reason);
    void sendBookingReminder(Booking booking);
}
```

### MockEmailService (active by default)

`com.booker.notification.service.MockEmailService` is the only registered bean.
It writes a single `INFO` log line per email call — no real email is sent, no SMTP
connection is made.

```
[EMAIL-MOCK] → client@example.com | Booking #42 created — Haircut on 15 Jul, 10:00 UTC
[EMAIL-MOCK] → client@example.com | Booking #42 CONFIRMED — Haircut on 15 Jul, 10:00 UTC
[EMAIL-MOCK] → client@example.com | REMINDER — Haircut on 15 Jul, 10:00 UTC (booking #42)
```

---

## Migrating to Real SMTP

### 1. Configure environment variables

| Variable       | Description                              | Example                |
|----------------|------------------------------------------|------------------------|
| `MAIL_HOST`    | SMTP server hostname                     | `smtp.sendgrid.net`    |
| `MAIL_PORT`    | SMTP port                                | `587`                  |
| `MAIL_USER`    | SMTP username (or API key user)          | `apikey`               |
| `MAIL_PASS`    | SMTP password / API key                  | `SG.xxxxx`             |
| `MAIL_FROM`    | Sender address shown in From: header     | `noreply@booker.app`   |
| `MAIL_TLS`     | Enable STARTTLS (`true` / `false`)       | `true`                 |

> Set these as OS environment variables, Docker env vars, or Kubernetes secrets.
> **Never commit credentials to source control.**

`application.yml` already reads them:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.sendgrid.net}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USER:}
    password: ${MAIL_PASS:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: ${MAIL_TLS:true}
```

### 2. Add Thymeleaf email templates

Create HTML templates under `src/main/resources/templates/email/`:

```
templates/email/
  booking-created.html
  booking-confirmed.html
  booking-cancelled.html
  booking-reminder.html
```

Minimal template example (`booking-confirmed.html`):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
  <h2>Your booking is confirmed!</h2>
  <p>Service: <strong th:text="${booking.serviceName}"></strong></p>
  <p>Date: <strong th:text="${booking.startTime}"></strong></p>
  <p>Thank you for choosing <strong th:text="${booking.businessName}"></strong>.</p>
</body>
</html>
```

### 3. Implement SmtpEmailService

Create a new bean that implements `EmailService` and uses
`org.springframework.mail.javamail.JavaMailSender` + `ThymeleafTemplateEngine`:

```java
@Service
@ConditionalOnProperty(name = "spring.mail.host")
@Primary   // takes priority over MockEmailService when MAIL_HOST is set
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${MAIL_FROM:noreply@booker.app}")
    private String from;

    @Override
    public void sendBookingCreated(Booking booking) {
        sendHtml(
            booking.getClient().getEmail(),
            "Booking Created — " + booking.getService().getName(),
            "booking-created",
            buildContext(booking)
        );
    }

    // ... implement the other three methods similarly

    private void sendHtml(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

    private Context buildContext(Booking booking) {
        Context ctx = new Context();
        ctx.setVariable("booking", booking);
        return ctx;
    }
}
```

Because `@Primary` is declared, Spring will prefer `SmtpEmailService` over
`MockEmailService` whenever the SMTP host property is present. No other code changes
are needed.

### 4. Testing email in staging

Use [Mailhog](https://github.com/mailhog/MailHog) or [Mailtrap](https://mailtrap.io):

```yaml
# docker-compose.override.yml
services:
  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # Web UI
```

```
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_TLS=false
```

---

## Triggered Events

| Trigger                   | Recipient       | Template             |
|---------------------------|-----------------|----------------------|
| Client creates booking    | Client          | `booking-created`    |
| Business confirms booking | Client          | `booking-confirmed`  |
| Any actor cancels booking | Client          | `booking-cancelled`  |
| 24h before appointment    | Client          | `booking-reminder`   |

Emails are sent from `BookingEventListener` (async Spring event) and
`ReminderScheduler` (`@Scheduled`, every 15 minutes).
