# ARYAA — Feature Documentation

**Emergency Communication & Coordination Platform**
AS307 · Native Android (Kotlin + Jetpack Compose) · Backend: Fastify + Prisma + PostgreSQL on Render

**Positioning:** ARYAA helps the right people receive the right information at the right time during an emergency. It guarantees that *if* help can come, it will maximize the chances of it reaching you quickly, accurately, and intelligently — not that help will arrive. It serves two user types: the person in trouble, and the person responding.

---

## Status Key
- ✅ **Built & Verified** — implemented, tested (including real two-device verification where relevant)
- 🔧 **Spec Locked, In Progress** — plan finalized, implementation underway or next
- 📋 **Planned** — in the locked build order, not yet started
- ⏸️ **On Hold** — deferred pending external permission/legal clearance

---

## Phase 1 — Core Safety Features ✅

### Authentication & Trusted Contacts
Standard login/signup. Users add Trusted Contacts who can receive alerts. Contacts are later tiered (see Unit 21) by proximity.

### SOS Trigger & Countdown
Core emergency trigger: a countdown-based SOS activation (manually cancellable) that, once it completes, dispatches alerts to trusted contacts with the user's location.

### Foreground Service, Notification & Volume-Button Trigger
A persistent Android foreground service keeps the SOS alive in the background, shows a persistent notification, and can be triggered via a physical volume-button sequence for discreet activation.

### Offline SMS Fallback
Originally built to send SMS alerts when no data connection is available. **Known limitation:** blocked on modern Android/OEM combinations (observed on Nothing OS + Android 14) via `SmsManager`. Resolved architecturally by making FCM push the primary channel, with SMS as a secondary path where the OS allows it.

### Security Hardening
HS512 JWT signing, rate limiting, Helmet HTTP headers, ProGuard obfuscation on the Android build.

### What3Words Integration
Converts coordinates to a 3-word address for easier verbal communication with responders/police. **Known limitation:** built but non-functional in production — the free What3Words plan lacks the convert-to-3wa endpoint. Degrades gracefully (feature simply doesn't populate) rather than breaking the app.

### Fake Call
Lets a user schedule a fake incoming call (with a custom caller name and delay: Now / 5s / 10s / 30s) to safely exit an uncomfortable in-person situation.

### Profile & Medical ID
User profile with medical information, accessible directly from the lock screen without unlocking the device — critical for first responders.

---

## Phase 2 — Response Infrastructure ✅

### PostgreSQL Migration
Migrated from an earlier database to PostgreSQL on Render, resolving several deployment gotchas (Prisma migration file encoding, build ordering, dependency placement) now documented as standing operational knowledge.

### FCM Push Notifications
Firebase Cloud Messaging replaces SMS as the primary alert channel, given the offline-SMS limitation above. Includes a distinctive emergency siren sound (dedicated notification channel, `USAGE_ALARM` audio attributes, Do Not Disturb bypass).

### Duress Mode
Allows a user to cancel an active SOS under coercion in a way that *appears* cancelled to anyone watching the device, while the backend silently continues treating the event as active and continues notifying contacts. Rigorously verified for response parity between real and duress cancellation (identical API shape/timing), with local device storage kept completely clean of any duress indicator.

### Practice Mode
Lets a user rehearse the real SOS flow using the actual UI and state machine, but with simulated network calls — no real alert is sent. Includes a three-layer visual safeguard against confusion with a real SOS, and reuses the same backend-validated active-SOS state as the Home banner rather than an independent local check.

### Dead Zone Check-In (base)
A safety timer: the user sets a duration; if they don't check in ("I'm Safe") before it lapses, SOS auto-triggers. Uses a proportional grace period (25% of duration, floored at 5 min, capped at 30 min) rather than a flat buffer. Later extended (see Feature #3 below) into Safe Walk and Heartbeat modes.

### Rescuer Playbook & Response Architecture (Unit 21)
Transformed ARYAA from a one-way alert broadcast into an active coordination layer for responders:
- **Contact proximity tiers:** each trusted contact is marked `isNearby`: Yes / No / Sometimes. Contacts are grouped in the UI as "Nearby Responders" vs. "Faraway Family & Friends," with a warning banner if zero nearby responders are configured.
- **Tiered, timed dispatch:** on SOS trigger, Local (nearby) contacts get an instant, distinctly-worded FCM push ("You're the closest person — can you go there now?"). Non-local/Family contacts are only notified after a 30-second server-side timer, and only if no Local contact has registered a response in that window. (Documented as an accepted v1 tradeoff: the 30s timer is in-memory and would be lost on a server restart — flagged for a Redis-backed queue if traffic scales.)
- **Structured Rescuer Playbook:** the responder's landing screen on tapping an alert, with an "I Am Responding!" opt-in, Step 1 (Call Victim), Step 2 (Contact Local Responders), Step 3 (Call 112 — via `ACTION_DIAL`, never auto-dialed, with an auto-copied speech template containing victim name, coordinates, and What3Words), and Step 4 (Coordinate with others via a share template).
- **Escalation reminders:** the sender's own active-SOS screen has a persistent Call 112 button and background timers at 2, 5, and 15 minutes that escalate reminder urgency if no one has responded.

### Notification & Navigation Reliability (multiple rounds of fixes)
This was the single most-debugged area of the app, across several regressions:
- **Notification channel versioning:** Android notification channels are immutable once created, so sound/importance/DND-bypass changes require bumping the channel ID (v1 → v4) and, eventually, programmatically deleting stale channel versions on app start to force clean re-creation on-device.
- **Splash-screen race condition:** tapping an SOS notification could race against the splash screen's own timer-based navigation, which would pop the emergency screen off the backstack a moment after it appeared. Fixed by dynamically routing around the splash screen entirely when the launching intent is an emergency deep link.
- **PendingIntent staleness:** hardcoded PendingIntent request codes caused Android to reuse cached, stale intent data across different SOS events. Fixed by deriving request codes from each event's unique ID.
- **Lock-screen launch:** the emergency response screen now correctly launches directly from a locked device (`showWhenLocked` + `turnScreenOn`), without requiring device unlock — a deliberate design choice disclosed to users in onboarding (see Safety Limits, below), since it means anyone holding an unlocked responder's phone during an active alert can see it too.
- **Cold-start / killed-app recovery:** if a user swipes away the notification or force-quits the app before ever tapping it, a `GET /api/sos/active-incoming` endpoint is polled on every app resume/launch, so the incoming-alert banner still appears even without the notification tap — closing a gap where a responder could otherwise remain unaware of an active alert directed at them.

### Onboarding Safety Limits Screen (Feature H)
A mandatory, checkbox-gated onboarding screen disclosing what ARYAA cannot do: no guarantee of network/battery, imperfect GPS accuracy, lock-screen visibility of incoming alerts, no replacement for calling 112 directly, and the 30-second proximity-based escalation delay. Cannot be bypassed except by an incoming real emergency deep link (which always takes priority — onboarding is deferred to the next normal app open rather than blocking a live emergency).

---

## New Feature Roadmap — Locked Build Order

*Decision: any feature carrying meaningful legal or safety-override risk is pushed to the end of the roadmap, built only after everything else ships, and then held pending explicit legal/permission clearance rather than shipped speculatively.*

### 1. Live Location Sharing ✅
An opt-in, non-emergency location-sharing session — independent of SOS — for situations like walking home alone. User picks a duration (30m / 1h / 2h / custom) and contacts; a foreground service pushes location every 20–30s to a backend session identified by a long random token. A public, no-login tracking page (Leaflet + OpenStreetMap) lets contacts follow along. Sessions expire server-side and unconditionally at `expiresAt`, independent of whether the client ever calls Stop — verified against a force-killed-app scenario. Ownership checks prevent one user from stopping or updating another's session.

### 2. Nearby Services Lookup ✅
On-demand lookup of nearby police stations, hospitals, pharmacies, and fire stations via the MapMyIndia (Mappls) API, proxied server-side (API key never shipped to the client). Reachable from both the sender's own SOS screen (centered on their location) and the responder's Rescuer Playbook (centered on the *victim's* location, not the responder's own). Includes tap-to-call and tap-to-navigate actions. A local mock-data fallback exists for development when no API key is configured, explicitly gated to never activate in production — a production failure returns a clear "unavailable, contact emergency services directly" error instead of fabricated locations.

### 3. Dead Zone Check-In Extension ✅
Extends the base Dead Zone engine (rather than building parallel systems) with two new modes:
- **Safe Walk:** user sets a destination and duration; a live location-sharing session (reusing Feature 1's infrastructure) auto-starts and auto-stops on arrival, but stays active if the timer lapses into an SOS escalation, so contacts can keep tracking.
- **Heartbeat Monitoring:** periodic "are you OK" check-ins at a user-set interval, with a persistent notification carrying a one-tap "I'm OK" action. Missing a check-in past a 5-minute grace period auto-escalates to SOS.
Critically, the decision of whether a check-in was missed is evaluated by a genuinely autonomous server-side sweep (a 30-second interval scan, decoupled from any client request) — not by a client-side WorkManager job, which Android can delay or kill under battery optimization. Verified by starting a session, forcing it into the past, and confirming escalation with zero client requests made for 35 seconds. (Documented as a per-instance sweep — an accepted v1 tradeoff that would need a distributed lock before horizontal scaling.)

### 4. Public Tracking Page ✅
Extends the same public tracking infrastructure built for Live Location Sharing to also cover active SOS events. On SOS trigger, a long random public token is generated and included in both the trigger response and the Rescuer Playbook data — surfaced to responders via a "Copy Public Tracking Link" action in the playbook's coordination step, for sharing with people outside the trusted-contact circle (bystanders, police at the scene). The public endpoint exposes only first name, coordinates, path history, and status — never phone number, user ID, or duress state. Resolved, cancelled, *and duress-cancelled* events all return an identical `{ active: false }` shape, by construction, so there is no distinguishable side channel that could reveal a duress cancellation to anyone watching the link. Tokens return a stable inactive response forever rather than eventually 404ing. Rate-limited against enumeration.

### 5. Community Safety Map 🔧
*Moderation model locked; implementation next.* Users can report safety incidents (harassment, poor lighting, theft, unsafe roads) pinned to a location. A pin only becomes publicly visible once it has 3+ reports from distinct phone numbers within a rolling 90-day window, and only ever displays an aggregate category + count — never individual report text or reporter identity. Reports decay out of the visibility count after 12 months.

**Dispute model (designed to prevent abuse in both directions):** only a user whose own registered profile address falls within roughly 50 meters of a pin may dispute it — disputing does not hide the pin, it flags it `UNDER_REVIEW` with a visible "disputed" tag, leaving it live until a manual admin review upholds or removes it. Pins in public spaces (streets, parks — anywhere not tied to a specific claimable address) cannot be disputed by anyone. This prevents both a single bad-faith click from silencing a real report, and a flood of reports from anonymously targeting a specific address with no recourse. Moderation for v1 is manual (solo-developer review via an admin endpoint), not automated. Explicitly out of scope for v1: public comments on pins, photo uploads, and any visibility into who filed a report.

### 6. Audio Recording ⏸️ *(legal-risk tier)*
Background audio recording during an active SOS or Fake Call, with consent disclosure required before activation given jurisdiction-dependent recording-consent law. Deferred to the end of the build order pending that legal review.

### 7. Panic Trigger Detector ⏸️ *(legal-risk tier)*
Passive accelerometer/microphone-based auto-trigger inside a Safe Zone, with a 10-second confirmation dialog before escalating. Deferred alongside audio recording due to false-positive risk and the same override-safety-valve concerns as accident detection below.

### 8. Auto-Call 112 ⏸️ *(legal-risk tier)*
A short, cancellable countdown that auto-dials 112 if not stopped. Deferred pending a decision on countdown duration (an initially suggested 5 seconds was flagged as likely too short) and must remain cancellable in every version.

### 9. Automatic Accident/Road-Rage SOS ⏸️ *(legal-risk tier — highest priority to resolve before building)*
Impact/G-force-based automatic SOS trigger, originally proposed **without a manual override**. Explicitly flagged as the highest-risk item in the roadmap: removing the cancel safety valve present in every other ARYAA feature creates real false-positive liability (dropped phones, rough roads, handing the phone to someone), and directly conflicts with ARYAA's own "we do not guarantee help arrives" positioning. Requires real legal review before this is even scoped, not just before it's implemented.

### 10. Bluetooth Mesh ⏸️
BLE beacon framework to relay encrypted SOS events between nearby ARYAA users when offline. Deferred for complexity/infrastructure reasons (originally a Phase 4 item), not legal risk.

### 11. Community Forum Page + Website
Public-facing forum and marketing/informational website. Last item before the legal-risk tier is revisited.

---

## After Feature 11

Everything through the community forum + website will be complete, legally clean, and shippable. At that point, the legal-risk tier (Features 6–9 above) goes on **explicit hold**, to be revisited only once the relevant permissions and legal clearance are actually in hand — rather than being built speculatively under project momentum.
