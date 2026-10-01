# Clinic notifications setup (Nobat Mobile)

Staff get **in-app** alerts for appointments assigned to them. Under the hood the app uses a notification topic on **your** relay host — the UI never names the relay product. Staff only see **Clinic code** / **کد مطب** and **Clinic notifications · Connected**.

Persian: [اعلان‌های مطب](CLINIC-NOTIFICATIONS.fa.md)

## If you already use a notification relay

You can keep using the **same host**. Create a **new topic** (and optionally a token limited to that topic) for Mobile only. Do not reuse topics other apps already publish to, and do not change unrelated Compose, ports, or TLS unless you intend to.

---

## What you need

1. Relay base URL (HTTPS), e.g. `https://ntfy.example.com`  
2. A **new topic** name Mobile alone will use, e.g. `nobat-mobile-clinic1`  
3. A **token** (or user) that can publish and subscribe to **only** that topic if you use ACL  

Clinic code in the app = base URL + topic + token (baked into one shareable string).

---

## 1. Create a dedicated topic (+ token)

On your relay (examples — adjust to your image’s CLI):

```bash
ntfy token add --expires=never
ntfy access <user-or-token> nobat-mobile-clinic1 read-write
```

If authenticated clients may use any topic, you can skip ACL and just pick a **fresh topic name** nothing else uses.

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

With the app killed or Doze, listen-only HTTP can be late. For better “phone in pocket” delivery, enable the relay’s push upstream (e.g. FCM) when you can. Always-on tablets on clinic Wi‑Fi are usually fine with subscribe alone.

## Non-standard HTTPS ports

You may embed `https://host:8443` (or another TLS port) in the Clinic code. Some mobile networks block non-443; prefer standard HTTPS on 443 when possible.

## Need help?

[Open an Issue](https://github.com/PouryaFA81/Nobat-Mobile/issues) from **Account → About**.
