# ARYAA Privacy Policy

**Last updated:** July 19, 2026

ARYAA ("we", "our", "the App") is an emergency communication and coordination platform developed by AS307. This Privacy Policy explains what information ARYAA collects, why, how it is used, and the rights you have over it. It applies to the ARYAA Android application and its backend services.

We built ARYAA to help the right people reach you quickly during an emergency — not to collect more information than that purpose requires. This policy is written to reflect exactly what the app does, not a general template.

---

## 1. Information We Collect

### 1.1 Account Information
When you create an ARYAA account, we collect your name, phone number, and email address, and store a securely hashed password. Your phone number is used to identify you to your Trusted Contacts and to verify report authenticity on the Community Safety Map (see Section 1.5).

### 1.2 Location Information
ARYAA collects location data only in the following situations, and never in the background without an active feature running:
- **During an active SOS alert**, to share your location with your Trusted Contacts and, where you choose to generate one, on the Public Tracking Page (Section 3).
- **During an opt-in Live Location Sharing session**, for a duration you choose (30 minutes, 1 hour, 2 hours, or a custom duration), shared only with the contacts you select.
- **During a Safe Walk or Dead Zone Check-In**, if you choose to use these features, for the duration of the session.
- **When you use Nearby Services lookup**, to find nearby police stations, hospitals, pharmacies, or fire stations relevant to your or a victim's location.

Location sharing always has a defined end: it stops when you cancel it, when the session's duration expires, or when the associated emergency is resolved. We do not track your location outside of these features.

### 1.3 Medical ID Information
You may optionally provide medical information (such as allergies, conditions, or medications) that is accessible from your device's lock screen without requiring your device to be unlocked, so that a first responder can access it in an emergency. This information is stored on our servers and is only visible: (a) on your own lock screen, and (b) to responders viewing an active Emergency Response/Rescuer Playbook during your active SOS.

### 1.4 Home Address (for Community Safety Map dispute eligibility only)
If you choose to participate in the Community Safety Map and wish to be eligible to dispute a safety report near your home, you may optionally provide your home address and coordinates. This is used solely to verify, via distance calculation, that a dispute is being raised by someone genuinely near the reported location. It is never displayed publicly and is not used for any other purpose.

### 1.5 Community Safety Map Reports
If you submit a safety report, we store the category, description, coordinates, your account ID, and a snapshot of your phone number at the time of submission. The phone number is used only to prevent the same person from artificially inflating a report count using multiple accounts — it is never displayed publicly. Publicly, a safety report only becomes visible on the map once it has been independently corroborated by three or more reports from distinct phone numbers within a 90-day period, and even then, the public map shows only an aggregate category and count — never individual report text, descriptions, or any reporter-identifying information.

### 1.6 Trusted Contacts
You may add contacts who will be notified during your SOS alerts. We store their name, phone number, and your designation of whether they are a "nearby" or "faraway" responder, which determines how quickly they are notified during an emergency.

### 1.7 Device and Technical Information
We collect a Firebase Cloud Messaging (FCM) push token to deliver emergency notifications to your device, and standard technical logs (such as API request timestamps) for security, debugging, and abuse prevention.

---

## 2. How We Use Your Information

We use the information above solely to operate ARYAA's features as described in the app: triggering and coordinating emergency responses, sharing your location with people you've chosen during an active session, displaying your Medical ID to responders during an emergency, aggregating anonymous safety reports on the Community Safety Map, and maintaining the security and reliability of the service.

We do not sell your personal data. We do not use your data for advertising. We do not share your data with third parties except as described in Section 3 (Public Tracking Pages, which you control) and Section 6 (service providers necessary to operate the app).

---

## 3. Public Tracking Pages

Certain ARYAA features — Live Location Sharing and active SOS events — can generate a public tracking link that does not require the viewer to log in or have an ARYAA account. This is intentional: it allows you or a responder to share your location with people outside your Trusted Contacts list, such as bystanders or police at the scene.

These links are protected by a long, randomly generated, unguessable security token — not a predictable or sequential identifier — and cannot be discovered by browsing or searching. The public page displays only your first name, current location, recent path, and status (active or resolved). **It never displays your phone number, email, full name, account identifiers, or any information about whether an alert was cancelled under duress.** Public tracking links stop showing live data the moment the associated session or SOS event ends, and this expiry is enforced by our servers independent of whether your device is online, so a link cannot continue to function after you intend it to stop.

---

## 4. Duress Mode

ARYAA includes a Duress Mode that allows you to cancel an active SOS alert in a way that appears cancelled on your device, while your Trusted Contacts continue to be notified, in situations where you may be forced to cancel an alert against your will. We do not store any indicator, log, or flag on your device that would reveal you used Duress Mode, and our public-facing systems (including the Public Tracking Page above) are specifically designed so that a duress cancellation is indistinguishable from a genuine one to anyone observing it externally.

---

## 5. Your Rights and Choices

You may access, correct, or delete your Medical ID, home address, and Trusted Contacts at any time within the app. You may delete individual safety reports you've submitted by contacting us, or dispute a report near your home address through the app if you are eligible to do so. You may request deletion of your account and associated data by contacting us at [insert contact email]. Some information tied to an active emergency or an active dispute under review may be retained briefly for safety and accountability purposes even after a deletion request, consistent with the purpose it was collected for.

Location sharing, Live Location Sharing, and Dead Zone/Safe Walk/Heartbeat features are entirely opt-in. You choose when to start them, for how long, and with whom you share.

---

## 6. Data Storage and Third-Party Services

Your data is stored on servers operated by our hosting provider (Render) using a PostgreSQL database. We use the following third-party services to operate specific features, each of which processes only the minimum data necessary for that feature:

- **Firebase Cloud Messaging (Google)** — to deliver push notifications for emergency alerts.
- **MapMyIndia (Mappls)** — to look up nearby police stations, hospitals, pharmacies, and fire stations. Your coordinates are sent to Mappls only when you actively use this feature.
- **What3Words** — to convert coordinates into a memorable three-word address for communicating your location, where available.

We do not permit these providers to use your data for any purpose other than providing the specific service ARYAA requests from them.

---

## 7. Data Retention

We retain account data for as long as your account is active. Location data associated with a specific SOS event or sharing session is retained as a historical record of that event for accountability and safety-review purposes, but is not used for ongoing tracking once the event ends. Community Safety Map reports are automatically excluded from public visibility after 12 months, though the underlying record may be retained for abuse-review purposes.

---

## 8. Children's Privacy

ARYAA is not directed at children and is not intended for use by anyone under the age of 18.

---

## 9. Grievance Officer

In accordance with the Information Technology Rules, 2021, if you have a complaint or grievance regarding how your information is handled, you may contact our Grievance Officer:

**Name:** [Insert name]
**Email:** [Insert email]
**Address:** [Insert address]

We will acknowledge and work to resolve grievances in a timely manner, consistent with applicable law.

---

## 10. Changes to This Policy

We may update this Privacy Policy as ARYAA's features evolve. We will update the "Last updated" date above when we do, and material changes will be communicated within the app.

---

## 11. Contact Us

Questions about this policy or your data can be directed to [insert contact email].
