ARYAA — PROJECT HANDOVER
Copy this into onboarding doc / new session context.

═══════════════════════════════════════
PROJECT OVERVIEW
═══════════════════════════════════════
ARYAA — native Android personal safety / emergency communication
app, Pune-first launch. Built by AS307. Development followed a
strict prompt → plan review → implementation → verified report
loop with an AI coding agent (Antigravity/OpenCode), reviewed and
verified after each unit — real two-device testing was used
repeatedly to catch regressions the emulator alone missed.

STACK:
- Android: Kotlin + Jetpack Compose, Hilt, Retrofit, Room,
  DataStore. minSdk 26, target/compile 34. "Aetheric Security"
  design system (Navy #0A0F1E / Saffron #FF6B1A dark theme,
  Playfair Display + Inter + JetBrains Mono).
- Backend: Fastify (Node/TS) + Prisma + PostgreSQL, deployed on
  Render at https://aryaa-backend.onrender.com.
  Repo: github.com/AS307India/aryaa-backend
  Kept alive via UptimeRobot pinging /health every 5 min.
- Push: Firebase Cloud Messaging, project "ARYAA", package
  com.as307.aryaa
- Local paths: Android at D:\AS307, backend at D:\AS307\apps\backend

POSITIONING (locked in, use consistently):
"ARYAA is an Emergency Communication & Coordination Platform.
It helps the right people receive the right information at the
right time during an emergency."
Promise: "ARYAA guarantees that if help can come, we will
maximize the chances of it reaching you as quickly, accurately,
and intelligently as possible." NOT a guarantee that help arrives.
Repositioned from an earlier "Personal Safety Companion" framing
after adversarial product questioning from a mentor.
User research (~20 respondents, Pune) validated Practice Mode,
Fake Call, and the offline-first architecture direction.

═══════════════════════════════════════
FEATURE STATUS — EVERYTHING BUILT & VERIFIED
═══════════════════════════════════════

PHASE 1 (complete): Auth, Trusted Contacts, SOS trigger+countdown,
foreground service+notification+volume-button trigger+offline SMS
(known-broken on modern OEMs, see gotchas), security hardening
(HS512/rate-limit/helmet/ProGuard), What3Words (non-functional on
free plan, degrades gracefully), Fake Call, Profile + Medical ID
(lock-screen accessible).

PHASE 2 (complete): PostgreSQL migration, FCM push notifications,
Duress Mode (response-parity verified, zero local trace), Practice
Mode (reuses real SOS state machine, three-layer visual safeguard),
Dead Zone Check-In base engine, Unit 21 Response Architecture
(contact proximity tiers, tiered/timed FCM dispatch with a 30s
Local→Family escalation window, Rescuer Playbook with I Am
Responding/Call Victim/Contact Responders/Call 112/Coordinate
steps), multiple rounds of notification/navigation reliability
fixes (splash-race condition, PendingIntent staleness, lock-screen
launch, cold-start recovery via active-incoming polling), and the
mandatory Safety Limits onboarding screen (Feature H).

NEW FEATURES — locked build order, ALL COMPLETE:
1. Live Location Sharing — opt-in, independent of SOS, token-gated
   public tracking page, server-side unconditional expiry.
2. Nearby Services Lookup — MapMyIndia (Mappls), server-proxied,
   context-aware (victim's location when opened by a responder).
3. Dead Zone Check-In Extension — Safe Walk + Heartbeat modes on
   the same engine, with a genuinely autonomous 30s server-side
   sweep for missed check-ins (not client-side WorkManager).
4. Public Tracking Page (SOS) — extends #1's infrastructure to
   active SOS events. Duress-safe by construction: DURESS,
   CANCELLED, and RESOLVED all return an identical response shape.
5. Community Safety Map — anti-abuse aggregation (3+ reports from
   distinct phone numbers, 90-day window), asymmetric dispute model
   (only users within ~50m of a pin can dispute it, and disputing
   flags rather than hides), manual admin review, 12-month decay.

ALL FIVE ABOVE: 100% built, tested, verified. No known open bugs.

═══════════════════════════════════════
DELIBERATELY NOT BUILT — LEGAL/PERMISSION-GATED
═══════════════════════════════════════
Dropped entirely (not paused) for the current hackathon-facing
build, given unresolved legal/regulatory questions:
- Feature E: background audio recording (consent law unresolved)
- Feature G: Panic Trigger Detector (false-positive/no-override risk)
- Auto-call 112 with countdown (same override concern)
- Automatic accident/road-rage SOS via impact detection (highest-
  risk item — originally spec'd with NO manual override, which
  conflicts with ARYAA's own "no guarantee" positioning)
- Volunteer Responder Network (needs Aadhaar eKYC / UIDAI KUA
  licensing — not something a hackathon team should represent as
  "in progress")
- SMS gateway at scale (needs TRAI DLT registration)
- Background volume-trigger via AccessibilityService (Google Play
  policy review, not just a legal question)
- Bluetooth mesh (Feature I) — deferred for complexity, not legal
  risk, still just lower priority
- Community forum + website — last roadmap item, not yet started

Full compiled inventory of every legal/regulatory touchpoint across
the whole project (including ones affecting already-shipped
features, like DPDP Act compliance on Medical ID/location data, and
the IT Rules 2021 grievance-officer requirement that applies once
the Community Forum ships) is preserved in project memory — ask
for it explicitly if your team needs the full list again.

═══════════════════════════════════════
PRIVACY POLICY & TERMS OF USE
═══════════════════════════════════════
Both drafted from scratch (not template-derived) against ARYAA's
actual shipped behavior — covers exactly what data each feature
collects and why, explains the Public Tracking Page and Duress
Mode's privacy design in plain language, and includes the IT Rules
2021 Grievance Officer section.

STATUS: placeholders still need filling in ([Insert date],
[Insert name/email/address] for the Grievance Officer) before
publishing. Access point (link from Profile screen + display during
onboarding) was intentionally folded into the UI polish pass rather
than built as a separate task — confirm with whoever picks up
polish work that this actually landed before launch.

═══════════════════════════════════════
RECURRING GOTCHAS — DON'T RELEARN THESE
═══════════════════════════════════════
- Prisma migration .sql files MUST be UTF-8 WITHOUT BOM.
  PowerShell's default encodings add a BOM. Verify with:
  [System.IO.File]::ReadAllBytes(path) — first 3 bytes must be
  45 45 32 ("-- "), NOT 239 187 191 (BOM). Broke Render deploys
  3+ times before this became standard practice.
- Render build command MUST run `prisma generate` BEFORE
  `tsc build`.
- @types/node, @types/bcrypt, @types/jsonwebtoken must be in
  `dependencies`, NOT `devDependencies` — Render production
  install skips devDependencies.
- A failed Prisma migration leaves a stuck record in
  _prisma_migrations that blocks ALL future migrations until
  manually cleared (direct psql/pg client DELETE).
- Android emulator has a HARDCODED mock location (18.5204,
  73.8567, central Pune). Real location testing needs a physical
  device — this bit the team hard on the original notification/
  lock-screen bugs.
- Notification channels are IMMUTABLE after creation — changing
  sound/importance requires bumping the channel ID and, eventually,
  programmatically deleting stale channel versions on app start
  (now standard practice, currently on v4).
- ANY screen needing "is there an active X right now" (SOS,
  incoming alert, location share) MUST read from shared,
  backend-validated state — never an independent local-only check.
  This bug class has appeared multiple times across this project
  (Home banner sticking, cold-start recovery gap, etc.) — treat it
  as a standing architectural rule, not a one-off fix.
- Any auto-escalation logic (Dead Zone, Heartbeat, the old 30s SOS
  timer) must be evaluated server-side on an autonomous sweep, never
  trust a client-side timer (WorkManager, foreground service
  countdown) as the actual trigger — OEM battery optimization and
  app kills will silently break client-only timing.
- PowerShell ISO-8601 UTC datetime parsing is unreliable — prefer
  Start-Sleep with a fixed second count over computed wait times
  in test scripts.
- Any permission-gated platform API (location, SMS, notifications,
  audio) MUST check ContextCompat.checkSelfPermission() before
  calling.
- adb not recognized in PowerShell → add platform-tools to PATH
  (User scope works without admin rights: 
  [Environment]::SetEnvironmentVariable("Path", $env:Path + 
  ";<sdk_path>\platform-tools", "User"), then open a genuinely
  new terminal window — PATH changes never apply to the current
  session).

═══════════════════════════════════════
HANDOFF TO SECURITY & CLOUD TEAM
═══════════════════════════════════════
This is the team's next focus. Starting points and known gaps,
organized so nothing gets silently skipped:

SECRETS & CONFIGURATION
- Confirm MAPPLS_API_KEY, ADMIN_EMAIL, ADMIN_USER_ID, and all FCM/
  Firebase credentials are actually set in Render's production
  environment — several backend features (Nearby Services, Safety
  Map admin routes) have code-level fallbacks that are meant ONLY
  for local dev and must never silently activate in production:
  - Nearby Services falls back to mock data if MAPPLS_API_KEY is
    missing — verify this fallback is gated behind NODE_ENV and
    genuinely cannot fire in production.
  - Safety Map admin routes fall back to a hardcoded default
    admin@aryaa.com if ADMIN_EMAIL isn't set — confirm this env
    var IS actually set in Render, and confirm your signup flow
    can't let an arbitrary user claim that email address.
- Rotate/verify the FCM service account credentials aren't
  committed anywhere in the repo history.

AUTH & ACCESS CONTROL
- JWT signing uses HS512 — confirm the signing secret is strong,
  stored only in environment config, and rotated if it was ever
  used during earlier dev/test phases with a weaker placeholder.
- Every write endpoint that operates on a session/event by ID
  (location-share stop/update, dead-zone checkin, safety-report
  dispute) has an ownership check pattern (userId === session.userId)
  — worth an explicit audit pass confirming every such endpoint has
  this, not just the ones that were flagged during review.
- Admin routes (Safety Map resolve/list) currently use a simple
  env-var identity check layered on JWT — fine for solo-dev scale,
  worth evaluating whether a proper role-based system is needed
  before this app has more than one admin.

DATA & PRIVACY
- DPDP Act (India) compliance pass — not yet formally done. Medical
  ID, live location data, and Safety Map reporter phone numbers are
  the highest-sensitivity data in this system and should be the
  first things reviewed.
- Public Tracking Page and Duress Mode were specifically designed
  and tested so a duress cancellation is indistinguishable from a
  genuine one at the API response level — this is a genuine safety
  property, not just a nice-to-have. Any future backend change
  touching SosEvent status handling MUST be checked against this
  invariant before merging.
- Confirm rate limiting is actually active in production on every
  public/unauthenticated endpoint (Safety Map pins, SOS public
  tracking, dispute submission) — these were built with rate limits
  but worth a live verification pass, not just a code-review
  assumption.

INFRASTRUCTURE
- Single Render instance currently. The Dead Zone/Heartbeat
  auto-escalation sweep uses a per-instance `setInterval` — this is
  documented in code as an accepted v1 tradeoff, but MUST be
  addressed (distributed lock, e.g. Redis/Redlock, or a centralized
  cron trigger) before horizontal scaling to multiple instances, or
  expired sessions will be double-processed and contacts could get
  duplicate escalation pushes.
- UptimeRobot keeps the free-tier Render instance warm — confirm
  this is still configured and firing before any demo/launch, since
  a cold Render instance adds real latency to the first SOS trigger
  after idle time.
- Consider whether the current Render free/hobby tier is sufficient
  for expected load, especially once/if the Community Forum ships
  and public traffic increases beyond what a solo-dev testing phase
  generated.

WHAT'S ALREADY SOLID (don't re-litigate without reason)
- Token design across Live Location Sharing and Public SOS
  Tracking: long random UUIDs, DB-lookup validation (not app-layer
  string comparison), unconditional server-side expiry independent
  of client state — verified against force-killed-app scenarios.
- The shared-state architectural rule (never independent local
  checks for "is X currently active") is now consistently applied
  across SOS, location sharing, and dead zone — a real strength of
  this codebase, worth preserving as new features get added.

═══════════════════════════════════════
DOCUMENTATION ON FILE
═══════════════════════════════════════
- ARYAA_Project_Status_and_Roadmap_v2.docx — original 13-page status
  doc (may be stale relative to this handover; this document
  supersedes it for feature status).
- ARYAA_Feature_Documentation.md — detailed writeup of every
  feature and how it works, written for internal/team reference.
- ARYAA_App_Guide.md — user/presentation-facing version of the same,
  organized for a hackathon audience, includes the held-back
  legal-risk section framed transparently.
- ARYAA_Privacy_Policy.md / ARYAA_Terms_of_Use.md — drafted from
  scratch, placeholders need filling before publishing.

═══════════════════════════════════════
IMMEDIATE NEXT STEPS FOR THE TEAM
═══════════════════════════════════════
1. Security/Cloud: work through the audit list above, starting with
   the two production-fallback risks (Mappls mock data, admin email
   default) since those are the cheapest to silently get wrong.
2. Whoever owns UI: complete the polish pass, including wiring the
   Privacy Policy/Terms access point into Profile + onboarding.
3. Fill in the Privacy Policy/Terms placeholders (date, Grievance
   Officer contact) before any public-facing demo or launch.
4. When ready to resume the legal-risk tier, start with a real
   DPDP compliance pass — it applies to already-shipped features,
   not just the future ones, so it doesn't need to wait for the
   rest of the held-back list.
