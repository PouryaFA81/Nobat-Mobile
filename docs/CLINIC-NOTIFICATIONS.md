# Clinic notifications setup (Nobat Mobile)

Staff get **in-app** alerts for appointments **assigned to them** (book, cancel, move, and evening digest when that channel runs). Under the hood, **Your own relay** uses a notification topic on a host you control. The app UI never names the relay product — staff see **Clinic code** / **کد مطب** and **Clinic notifications · Connected**.

Persian: [اعلان‌های مطب](CLINIC-NOTIFICATIONS.fa.md)

**From 0.16.1:** Hosted **Share** uses an opaque code only (no host, topic, or token in what staff receive). A future resolution service can turn that id into credentials; that backend is **not** in this app yet.

---

## Dual path

Admin opens **Account → Advanced Settings → Clinic code** and picks one path:

### 1. Relay code (hosted)

1. Paste a relay code → **Redeem** / **فعال‌سازی** (or use **I have a code** / **کد دارم**).
2. Optionally **Get a relay code** / **دریافت کد رله** opens the purchase/contact URL from **Your own relay → Relay advanced** (`relay_purchase_url`; blank by default — set it only if you sell/hosted access).
3. Status shows **Connected** + **Hosted** / **میزبانی‌شده**. The UI **hides** URL, topic, and token.
4. **Share** shows a **Clinic code (hosted)** string: `nobatH1:` + opaque id only. It does **not** include your relay address or token.

Admin publish (book / cancel / move / digest) still uses credentials stored on the admin phone from redeem. Staff who only receive `nobatH1:` **cannot Connect** until a resolution service exists — see [Staff phone](#3-staff-phone) below.

### 2. Your own relay (advanced)

1. Enter base URL · topic · token → **Save & create Clinic code**.
2. **Share** uses `nobat1:` with host + topic + token (needed so staff can Connect without a backend).
3. **Regenerate** if a code was leaked (new topic; clear token and set a fresh one).

---

## Code formats

| Prefix | Payload | Use |
|--------|---------|-----|
| `nobat1:` | base64url(`{"u","t","k"}`) | Own-relay Clinic code (share with staff) **or** a plain Relay code for Redeem |
| `nobatR1:` | same JSON as above | Optional hosted-purchase wrapper; Redeem marks **Hosted** and stores credentials on the admin device |
| `nobatH1:` | base64url(`{"i"}`) | **Hosted Share only** — opaque id; **no** `u` / `t` / `k` |

Sellers who mint hosted redeem codes do so **offline**. Do **not** bake a real relay URL into the APK, repo, or defaults. Use placeholders like `https://example.com/relay` in docs and settings hints only.

---

## If you already run a notification relay

You can keep the **same host**. Create a **new topic** (and optionally a token limited to that topic) for Mobile only. Do not reuse topics other apps already publish to, and do not change unrelated Compose, ports, or TLS unless you intend to.

### What you need (Your own relay)

1. Relay base URL (HTTPS), e.g. `https://ntfy.example.com`
2. A **new topic** name Mobile alone will use, e.g. `nobat-mobile-clinic1`
3. A **token** (or user) that can publish and subscribe to **only** that topic if you use ACL

### Create a dedicated topic (+ token)

On your relay (examples — adjust to your image’s CLI):

```bash
ntfy token add --expires=never
ntfy access <user-or-token> nobat-mobile-clinic1 read-write
```

If authenticated clients may use any topic, you can skip ACL and just pick a **fresh topic name** nothing else uses.

---

## Admin phone

1. Open **Clinic code**
2. Pick **Relay code** (paste → Redeem) **or** **Your own relay** (URL · topic · token → Save)
3. Confirm **Connected** (and **Hosted** if redeemed)
4. Share the code shown:
   - **Own relay:** `nobat1:…` (contains host/topic/token — treat like a secret)
   - **Hosted:** `nobatH1:…` (opaque; safe to share relative to host leakage; staff Connect still needs future resolve)

When you book, cancel, or move with **Assign to**, or when **Evening digest** runs with Clinic configured, the admin phone publishes to the topic; staff phones that are connected show a local notification.

---

## Staff phone

1. Install the same Nobat Mobile APK  
2. Role = **Staff**, **Link to personnel**  
3. **Enter Clinic code** → **Connect**  
4. Status: **Clinic notifications · Connected**

**Today (0.16.1):** Staff Connect works with full credential codes (`nobat1:` / redeemed `nobatR1:` content). An opaque **`nobatH1:`** share alone is **not** enough to Connect until a hosted resolution service exists. Until then, multi-phone Clinic alerts for a hosted admin still need a credential-bearing path for staff, or wait for that service.

They receive in-app alerts for **their** appointments. SMTP / Telegram / SMS stay optional extras (also aimed at staff/personnel, not client confirmation).

---

## Evening digest (Clinic channel)

If **Send evening digest** is on, the daily job may publish `event=digest` on the Clinic topic (in addition to SMTP per staff, Telegram when Notify on book is on, and a local notification). Retries skip channels that already succeeded for that day (0.16.1).

---

## Reliability tip

With the app killed or Doze, listen-only HTTP can be late. A future push upstream (e.g. FCM) on a managed relay is optional; it is **not** required for Your own relay. Always-on tablets on clinic Wi‑Fi are usually fine with subscribe alone.

## Non-standard HTTPS ports

You may embed `https://host:8443` (or another TLS port) in an own-relay Clinic code. Some mobile networks block non-443; prefer standard HTTPS on 443 when possible.

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) from **Account → About**.
