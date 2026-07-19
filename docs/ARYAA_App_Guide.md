# ARYAA — Complete App Guide

**Emergency Communication & Coordination Platform**
Built by AS307

ARYAA helps the right people receive the right information at the right time during an emergency. It does not promise that help will always arrive — it promises to maximize the chances of it arriving, as quickly and accurately as possible, for two kinds of people: the person in trouble, and the person responding.

This document explains every feature currently in the app, how each one actually works, and — separately — the features that exist in the roadmap but have been deliberately left out for now, and why.

---

## SECTION 1 — Core Safety Features

### Sign Up & Trusted Contacts
You create an account with your name, phone number, and email. You then add Trusted Contacts — people who will be notified if you ever trigger an SOS. For each contact, you mark whether they're usually **nearby** you or **faraway**, which changes how quickly they're alerted (see "Rescuer Playbook" below).

### SOS Trigger
A large, easy-to-find button starts a countdown. If you don't cancel it, an alert goes out to your Trusted Contacts with your live location. The countdown is fully cancellable at any point before it completes — nothing is sent by accident.

### Foreground Persistent Alert & Volume-Button Trigger
Once an SOS is active, a persistent notification keeps it running even if you switch apps or lock your phone. You can also trigger an SOS discreetly using a physical volume-button sequence, without needing to open the app or look at your screen — useful if you can't safely reach for your phone normally.

### Fake Call
Lets you schedule a fake incoming call, with a caller name you choose (like "Maa") and a delay (now, 5s, 10s, or 30s), so you have a believable reason to step away from an uncomfortable situation.

### Medical ID
Your medical information (allergies, conditions, medications) is stored and shown directly on your **lock screen** — no unlocking needed — so a first responder can see it immediately if you're unable to communicate.

### Duress Mode
If you're ever forced to cancel a real SOS against your will, Duress Mode lets you do it in a way that *looks* cancelled on your screen — but your Trusted Contacts keep getting notified in the background. Nothing on your device stores any trace that this happened.

### Practice Mode
Lets you rehearse the entire SOS experience — same screens, same flow — without sending anything real. It's built so it's visually impossible to confuse with an actual emergency, so you can practice with confidence.

---

## SECTION 2 — Response & Coordination (for the person helping)

### Rescuer Playbook
When someone in your Trusted Contacts triggers an SOS, you're taken straight to a structured response screen — even from a locked phone, no unlocking required, so you can act instantly. It walks you through: confirming you're responding, calling the person in trouble, seeing other nearby responders, calling 112 with a ready-made script (their name, location, and coordinates already filled in — you just tap dial), and coordinating with others.

### Tiered, Timed Alerts
"Nearby" contacts are notified the instant an SOS triggers. "Faraway" contacts are only notified after a short delay — and only if no nearby person has already confirmed they're on it — so family far away isn't unnecessarily alarmed the moment a local friend is already responding.

### Nearby Services Lookup
From an active alert, you can pull up the nearest police stations, hospitals, pharmacies, and fire stations relative to *the person in trouble's* location — not your own — with one tap to call or navigate there.

---

## SECTION 3 — Location & Check-In Tools

### Live Location Sharing
Completely separate from SOS — an everyday, opt-in tool. Pick a duration (30 min, 1 hour, 2 hours, or your own custom time) and choose who to share your live location with. Stops automatically when time's up, or the moment you tap Stop.

### Public Tracking Page
When you're sharing your location or have an active SOS, you can generate a link that anyone can open — no login, no app needed — to see your live position on a map. Useful for handing off to a bystander or police at the scene. The link only ever shows a first name and location, nothing else about you, and stops working the second the situation resolves.

### Dead Zone Check-In
A safety timer for situations without reliable signal. Set a duration; if you don't check in before it runs out, an SOS triggers automatically. This same engine powers two more specific modes:
- **Safe Walk** — set a destination and expected time; your location shares live with your contacts as you walk, and one tap ("I've Arrived") ends it cleanly.
- **Heartbeat Monitoring** — periodic "are you OK?" check-ins at an interval you choose, with a one-tap "I'm OK" button right on the notification. Miss too many, and it escalates automatically — even if your phone dies or the app gets closed, this check runs independently on our servers, not just on your device.

---

## SECTION 4 — Community Features

### Community Safety Map
See safety-related reports (poor lighting, past incidents, etc.) from other users, shown only in aggregate — never individual write-ups or who reported them. A location only shows up on the map once at least three different people have independently reported it, which keeps single false reports from appearing. If you believe a report near your own home is wrong, you can flag it for review — it stays visible with a "disputed" tag while it's looked at, rather than disappearing the moment someone clicks a button.

---

## SECTION 5 — Held Back for Now (Legal / Permission-Dependent)

These are real, designed features that are **intentionally not included** in the current build. Each one touches a law, a government registration process, or a consent question that needs to be properly resolved before it ships — not something we're comfortable shipping "for now" and fixing later. We'd rather have a smaller app that's fully above board than a bigger one with an open legal question.

- **Background audio recording during an SOS** — recording-consent law varies and needs direct legal review before this is built, not just before it's turned on.
- **Automatic panic-trigger detection** (motion/sound-based auto-SOS) — real risk of false triggers with no manual override, which we're not comfortable with until that tradeoff is properly resolved.
- **Auto-dial to 112** with a short countdown — same override concern; needs a properly considered cancellable design first.
- **Automatic accident/impact-detection SOS** — the highest-risk item on the list. As originally proposed it had no manual cancel at all, which conflicts with the honest promise this app makes: that it helps, but never guarantees.
- **Volunteer Responder Network** — would require government eKYC licensing (Aadhaar-based identity verification) that we don't currently hold.
- **SMS gateway at scale** — requires a telecom regulatory registration (DLT) that's a straightforward but unfinished business process, not a technical blocker.

---

## SECTION 6 — Privacy Policy & Terms of Use

Two documents govern how ARYAA works, written specifically for this app — not adapted from a template:

**Privacy Policy** — explains exactly what data ARYAA collects (only what each feature needs — location only during an active session, medical info only if you choose to add it, phone numbers only to identify Trusted Contacts and prevent fake safety reports), why, and who can see it. It also explains, in plain terms, how the Public Tracking Page and Duress Mode are built so they never leak more than intended.

**Terms of Use** — explains what ARYAA is and isn't (a coordination tool, not a guarantee of rescue), the honest limitations of the app (needs signal, needs battery, GPS isn't perfect), and the rules for using features like the Safety Map and Duress Mode responsibly.

**Where to find them:** both are shown during onboarding before you can use the app, and are always reachable afterward from your Profile screen. You don't need to hunt for them — they're written to actually be read, not just clicked past.

---

*This document reflects ARYAA's build as of the current development phase. Section 5 will move into the main feature list only once its underlying legal or regulatory question is actually resolved — not simply because it's technically ready.*
