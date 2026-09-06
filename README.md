# AnnounceUPI

<p align="center">
  <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/megaphone-fill.svg" width="64" height="64" style="filter:invert(52%) sepia(97%) saturate(400%) hue-rotate(100deg);"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-6.0%2B-3DDC84?style=flat&logo=android&logoColor=white"/>
  &nbsp;
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?style=flat&logo=openjdk&logoColor=white"/>
  &nbsp;
  <img src="https://img.shields.io/badge/Version-2.0-blue?style=flat"/>
  &nbsp;
  <img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=flat"/>
  &nbsp;
  <img src="https://img.shields.io/badge/Price-Free-3DDC84?style=flat"/>
</p>

> **Your UPI payments, announced out loud — the moment they arrive.**

AnnounceUPI is a free Android app that listens for incoming UPI payment notifications and speaks the received amount aloud in your chosen language. Built for shop owners, vendors, and anyone who can't always look at their phone screen.

---

## Screenshots

| Home | Dashboard | Settings |
|------|-----------|----------|
| Transaction history with live total | Charts, stats, CSV export | Permissions, toggles, Google Sign-In |

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/lightning-fill.svg" width="20"/> Features

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/megaphone-fill.svg" width="18"/> Voice Announcement
- Speaks payment amount and source app the moment money arrives
- Works with PhonePe, Google Pay, Paytm, and 20+ UPI apps
- Runs silently in the background — no interaction needed
- Requests audio focus before speaking so it's always heard

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/translate.svg" width="18"/> Multi-Language Support
Announces in 10 Indian languages:
Hindi · Tamil · Telugu · Kannada · Bengali · Marathi · Malayalam · Gujarati · Punjabi · English

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/bar-chart-fill.svg" width="18"/> Dashboard & Analytics
- Today's total, this month's total, all-time total
- Daily bar chart (30-day view)
- Weekly line chart (last 7 days)
- Per-app payment breakdown

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/clock-history.svg" width="18"/> Transaction History
- Full history of every payment with source, amount, time, date
- Search by amount, app, or date
- Add personal notes to any transaction (double-tap)
- Share any transaction (long-press)
- Delete individual transactions
- Clear all history with one tap
- Exports as CSV file

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/calendar-check-fill.svg" width="18"/> Daily Summary
- Push notification at a custom time showing day's total
- Auto-send summary to a WhatsApp number with full breakdown
- Configurable — enable/disable independently

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/moon-fill.svg" width="18"/> Do Not Disturb
- Set quiet hours — no announcements between e.g. 10 PM and 7 AM
- Minimum amount filter — skip payments below a set threshold

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/shield-lock-fill.svg" width="18"/> Security
- Biometric / PIN app lock (fingerprint or device PIN)
- 5-minute session — no re-prompt while actively using
- Google Sign-In for account protection
- All data stored locally on device — nothing sent to servers

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/grid-fill.svg" width="18"/> Home Screen Widget
- 1×1 square widget showing today's total
- Auto-refreshes every 10 seconds
- Full 24-hour refresh via WorkManager
- Survives phone reboots

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/chat-text-fill.svg" width="18"/> SMS Fallback
- Detects payments via SMS if notification access is unavailable
- Parses standard bank SMS formats

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/code-slash.svg" width="20"/> Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| Min SDK | Android 6.0 (API 23) |
| Target SDK | Android 14 (API 34) |
| UI | Material 3, ConstraintLayout, RecyclerView |
| Navigation | Bottom Navigation + Fragment show/hide caching |
| Charts | MPAndroidChart (JitPack) |
| Background Jobs | WorkManager |
| Image Loading | Glide |
| Auth | Google Sign-In (play-services-auth) |
| Storage | SharedPreferences (JSON) |
| Build | Gradle 8.2.2, R8 minification |

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/folder-fill.svg" width="20"/> Project Structure

```
app/src/main/java/com/announceupi/in/
│
├── MainActivity.java # Bottom nav host, fragment caching
├── SplashActivity.java # 1.8s splash screen
├── LoginActivity.java # Google Sign-In screen
│
├── HomeFragment.java # Transaction history + totals
├── DashboardFragment.java # Charts + CSV export
├── SettingsFragment.java # All settings and permissions
│
├── PaymentNotificationService.java # Core: listens + announces UPI payments
├── SmsReceiver.java # SMS fallback detection
├── TransactionAdapter.java # RecyclerView adapter
│
├── GoogleAuthManager.java # Google Sign-In helper
├── LockManager.java # Biometric session management
├── DailySummaryWorker.java # WorkManager daily summary job
├── ExportManager.java # CSV export via FileProvider
├── AnnounceUPIWidget.java # Home screen widget provider
├── WidgetDailyWorker.java # 24h widget refresh worker
├── BootReceiver.java # Restart alarms after reboot
├── WhatsAppActionReceiver.java # WhatsApp summary sender
└── MonthlyChartActivity.java # Full-screen chart view
```

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/shield-fill-check.svg" width="20"/> Permissions

| Permission | Why |
|-----------|-----|
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/bell-fill.svg" width="14"/> `BIND_NOTIFICATION_LISTENER_SERVICE` | Detect UPI payment notifications |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/chat-fill.svg" width="14"/> `RECEIVE_SMS` | SMS fallback payment detection |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/chat-text-fill.svg" width="14"/> `READ_SMS` | Read bank SMS content |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/wifi.svg" width="14"/> `INTERNET` | Google Sign-In, profile picture |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/power.svg" width="14"/> `RECEIVE_BOOT_COMPLETED` | Restart widget alarm after reboot |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/phone-vibrate-fill.svg" width="14"/> `VIBRATE` | Haptic feedback on payment |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/bell-fill.svg" width="14"/> `POST_NOTIFICATIONS` | Daily summary notification (Android 13+) |
| <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/fingerprint.svg" width="14"/> `USE_BIOMETRIC` | App lock |

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/rocket-takeoff-fill.svg" width="20"/> Getting Started

### Prerequisites
- Android Studio / AndroidIDE
- Android device or emulator running Android 6.0+
- JDK 17

### Build

```bash
# Clone the project
git clone https://github.com/yourusername/announceupi.git

# Open in Android Studio and sync Gradle
# Or build from terminal:
./gradlew assembleDebug
```

### Google Sign-In Setup (optional)

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create a project → APIs & Services → Credentials
3. Create an OAuth 2.0 Android client
4. Enter package name: `com.announceupi.in`
5. Enter your debug SHA-1:
 ```bash
 keytool -list -v -keystore ~/.android/debug.keystore \
 -alias androiddebugkey -storepass android -keypass android
 ```
6. Place `google-services.json` in `app/`

### First Launch

1. Install the APK
2. Grant **Notification Access** — Settings → Special app access → Notification access → AnnounceUPI
3. Optionally grant SMS permission for fallback detection
4. Receive a UPI payment — the app will announce it aloud

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/phone-fill.svg" width="20"/> Supported UPI Apps

PhonePe · Google Pay · Paytm · Amazon Pay · BHIM · MobiKwik · Freecharge · Jio Money · Airtel Money · PayZapp · iMobile · Yono SBI · BHIM SBI · Axis Pay · Kotak Pay · HDFC PayZapp · IndusInd · Bank of Baroda · Federal Bank · UCO Bank · and more

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/database-fill.svg" width="20"/> SharedPreferences Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `dark_mode` | boolean | true | App theme |
| `tts_language` | String | "en" | Announcement language |
| `tts_speed` | String | "normal" | Speech speed |
| `tts_repeat` | boolean | false | Repeat announcement |
| `min_amount` | float | 0 | Min payment to announce |
| `dnd_enabled` | boolean | false | Do Not Disturb |
| `dnd_start` | int | 22 | DND start hour |
| `dnd_end` | int | 7 | DND end hour |
| `daily_summary_enabled` | boolean | true | Daily summary notification |
| `summary_hour` | int | 21 | Summary time (hour) |
| `summary_minute` | int | 0 | Summary time (minute) |
| `whatsapp_summary_enabled` | boolean | false | Auto WhatsApp summary |
| `whatsapp_summary_phone` | String | "" | WhatsApp recipient number |
| `lock_enabled` | boolean | false | Biometric app lock |
| `transactions` | String | "[]" | JSON array of transactions |

### Transaction JSON Format

```json
{
 "amount": "₹500",
 "raw_amount": 500.0,
 "source": "PhonePe",
 "time": "10:30 PM",
 "date": "17 Aug 2026",
 "note": "Customer payment"
}
```

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/whatsapp.svg" width="20"/> WhatsApp Daily Summary

Enable in Settings → Notifications & Security → Send to WhatsApp.

The summary is sent at your configured time and looks like this:

```
*AnnounceUPI Daily Summary*
Date: 17 Aug 2026

Total Received: *₹4,250.00*
Transactions: *3*

*Breakdown:*
 - PhonePe: ₹2,000.00
 - Google Pay: ₹1,500.00
 - Paytm: ₹750.00

_Sent by AnnounceUPI_
```

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/clock-history.svg" width="20"/> Version History

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/star-fill.svg" width="16"/> v2.0 (Current)
- Bottom navigation — Home, Dashboard, Settings
- Google Sign-In with profile picture
- WhatsApp daily summary auto-send
- Per-transaction delete
- Custom daily summary time picker
- Home screen widget with auto-refresh
- Splash screen
- Daily summary no longer counted as transaction
- Beep sound fixed
- Fragment caching for smooth navigation

### <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/flag-fill.svg" width="16"/> v1.0
- Initial release
- Core UPI announcement
- Basic transaction history
- Dark/Light theme

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/exclamation-triangle-fill.svg" width="20"/> Known Limitations

- Google Sign-In requires SHA-1 registration in Google Console
- WhatsApp summary opens WhatsApp for the user to tap Send (cannot send silently due to Android restrictions)
- Widget refresh interval is ~10 seconds (Android limits exact alarm frequency)
- Some banking apps use different notification formats — SMS fallback covers these

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/person-fill.svg" width="20"/> Developer

**Rohit Prasad Yadav**
- <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/envelope-fill.svg" width="14"/> rohitprasadyadav06@gmail.com
- <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/instagram.svg" width="14"/> [@rohiit.md](https://instagram.com/rohiit.md)
- <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/building-fill.svg" width="14"/> Yadav Enterprises

---

## <img src="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/icons/file-earmark-text-fill.svg" width="20"/> License

```
Copyright 2026 Rohit Prasad Yadav — Yadav Enterprises

Licensed under the Apache License, Version 2.0.
You may not use this file except in compliance with the License.
```

---

> Built with AndroidIDE on Android · No PC required
