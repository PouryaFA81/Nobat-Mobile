# Clinic notifications setup (Nobat Mobile)

Staff get **in-app** alerts for appointments assigned to them. Under the hood the app uses a notification topic on **your** existing ntfy host — the UI never says “ntfy.” Staff only see **Clinic code** / **کد مطب** and **Clinic notifications · Connected**.

Persian: [اعلان‌های مطب](CLINIC-NOTIFICATIONS.fa.md)

## Important — friend’s Nobat stack

If you already run Nobat (PWA) with ntfy for a friend:

- **Do not** add a second Docker Compose or a new subdomain  
- **Do not** edit their Compose, ports, TLS, or shared topics  
- **Do** create a **new topic** (and optionally a token limited to that topic) on the **same** ntfy  

Their old topics stay untouched. Mobile only publishes/subscribes on the new one.

---

## What you need

1. Your ntfy base URL (usually `https://ntfy.yourdomain` on port 443)  
2. A **new topic** name Mobile alone will use, e.g. `nobat-mobile-clinic1`  
3. A **token** (or user) that can publish and subscribe to **only** that topic if you use ACL  

Clinic code in the app = base URL + topic + token (baked into one shareable string).

---

## 1. Create a dedicated topic (+ token)

On the **existing** ntfy container (examples — adjust to your image’s CLI):

```bash
# Inside the ntfy container — create a token and grant access to ONE topic only
ntfy token add --expires=never
ntfy access <user-or-token> nobat-mobile-clinic1 read-write
```

If your ntfy already allows any authenticated topic, you can skip ACL and just pick a **fresh topic name** the PWA never uses.

---

## 2. Admin phone (Nobat Mobile)

1. Open **Account → Advanced Settings → Clinic code**  
2. Enter **Relay** (advanced): base URL · topic · token — or generate/share the combined **Clinic code**  
3. Status shows **Clinic notifications · Connected**  
4. Share the **Clinic code** with staff (QR / copy). Use **Regenerate** if a code was leaked  

When you book or cancel with **Assign to**, the admin phone publishes to the topic; staff phones listening show a local notification.

---

## 3. Staff phone

1. Install the same Nobat Mobile APK  
2. Role = **Staff**, **Link to personnel**  
3. **Enter Clinic code** → **Connect**  
4. Status: **Clinic notifications · Connected**  

They receive in-app alerts for **their** appointments. SMTP / Telegram / SMS stay optional extras.

---

## Reliability tip

With the app killed or Doze, listen-only HTTP can be late. For “phone in pocket” reliability, prefer ntfy’s **FCM upstream** on your relay when you can. Clinic LAN / always-on tablets are fine with subscribe alone.

## IR VPS without port 443

Possible as `https://host:8443` inside the Clinic code, but many mobile networks block non-443. Prefer the friend’s **443** ntfy for staff phones.

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) from **Account → About**.
