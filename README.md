
<div align="center">
  <img src="https://raw.githubusercontent.com/rpy2006/Announceupi/main/Assets/file_00000000ac90822f9f19d95c8ba9821f.png" alt="AnnounceUPI Banner" width="100%">
  <br><br>

  **Your UPI payments, announced out loud — the moment they arrive.**

  [![Download APK](https://img.shields.io/badge/⬇_Download_APK-v1.0.0-3DDC84?style=for-the-badge)](https://github.com/rpy2006/Announceupi/releases/download/v1.0.0/app-debug.apk)
  &nbsp;
  [![Android](https://img.shields.io/badge/Android-6.0%2B-3DDC84?style=flat&logo=android&logoColor=white)](https://android.com)
  &nbsp;
  [![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org)
  &nbsp;
  [![Version](https://img.shields.io/badge/Version-2.0-5B9CF6?style=flat)](https://github.com/rpy2006/Announceupi/releases)
  &nbsp;
  [![Free](https://img.shields.io/badge/Free-No_Ads-3DDC84?style=flat)](https://github.com/rpy2006/Announceupi)

</div>

---

## 📥 Download

> **[⬇ Download AnnounceUPI v1.0.0 APK](https://github.com/rpy2006/Announceupi/releases/download/v1.0.0/app-debug.apk)**

After downloading:
1. Enable **Install from unknown sources** in your Android settings
2. Open the downloaded APK and tap Install
3. Grant **Notification Access** when prompted

---

## 🤔 What is AnnounceUPI?

AnnounceUPI is a free Android app built for **shop owners, vendors, and merchants** who accept UPI payments. It listens for incoming payment notifications and **speaks the amount out loud** in your language — so you never miss a payment, even with your phone in your pocket.

---

## ✨ Features

### 🔊 Voice Announcement
- Speaks the payment amount and app name the moment money arrives
- Works with PhonePe, Google Pay, Paytm, and 20+ UPI apps
- Runs silently in the background — no interaction needed
- Audio focus request before speaking — always heard clearly

### 🌐 10 Indian Languages
Hindi · Tamil · Telugu · Kannada · Bengali · Marathi · Malayalam · Gujarati · Punjabi · English

### 📊 Dashboard & Analytics
- Today's total, this month, and all-time totals
- 30-day daily bar chart
- Last 7-day line chart
- Per-app payment breakdown

### 🧾 Transaction History
- Full history with amount, source, time, and date
- Search by amount, app, or date
- Double-tap to add a personal note
- Long-press to share any transaction
- Delete individual transactions
- Clear all history in one tap
- Export full history as a CSV file

### 📅 Daily Summary
- Push notification at a custom time each evening
- Auto-send summary to a WhatsApp number with full breakdown
- Shows total received and per-app breakdown

### 🌙 Do Not Disturb
- Set quiet hours — no announcements after e.g. 10 PM
- Minimum amount filter — skip small payments below your threshold

### 🔒 Security
- Biometric / PIN app lock
- 5-minute session — no re-prompt while actively using the app
- Google Sign-In for account protection
- All data stored locally — nothing sent to any server

### 🏠 Home Screen Widget
- Square widget showing today's total at a glance
- Auto-refreshes every 10 seconds
- Survives phone reboots

### 💬 WhatsApp Daily Summary
Configure a WhatsApp number and receive your daily summary automatically:
```
📣 AnnounceUPI Daily Summary
📅 17 Aug 2026

Total Received: ₹4,250.00
Transactions: 3

Breakdown:
  - PhonePe: ₹2,000.00
  - Google Pay: ₹1,500.00
  - Paytm: ₹750.00
```

---

## 📱 Screenshots

| 🏠 Home | 📊 Dashboard | ⚙ Settings |
|--------|------------|-----------|
| Live total, transaction history, search | Charts, stats, CSV export | Permissions, toggles, Google Sign-In |

---

## 📲 Supported UPI Apps

PhonePe · Google Pay · Paytm · Amazon Pay · BHIM · MobiKwik · Freecharge · Jio Money · Airtel Money · PayZapp · iMobile · Yono SBI · BHIM SBI · Axis Pay · Kotak Pay · HDFC PayZapp · IndusInd · Bank of Baroda · Federal Bank · UCO Bank · and more

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Min SDK | Android 6.0 (API 23) |
| Target SDK | Android 14 (API 34) |
| UI | Material 3, ConstraintLayout, RecyclerView |
| Navigation | Bottom Navigation + Fragment caching |
| Charts | MPAndroidChart |
| Background Jobs | WorkManager |
| Image Loading | Glide |
| Auth | Google Sign-In |
| Storage | SharedPreferences (JSON) |
| Build | Gradle 8.2.2, R8 minification |

---

## 📁 Project Structure

```
app/src/main/java/com/announceupi/in/
│
├── MainActivity.java               # Bottom nav host, fragment caching
├── SplashActivity.java             # 1.8s splash screen
├── LoginActivity.java              # Google Sign-In screen
│
├── HomeFragment.java               # Transaction history + totals
├── DashboardFragment.java          # Charts + CSV export
├── SettingsFragment.java           # All settings and permissions
│
├── PaymentNotificationService.java # Core: listens + announces payments
├── SmsReceiver.java                # SMS fallback detection
├── TransactionAdapter.java         # RecyclerView adapter
│
├── GoogleAuthManager.java          # Google Sign-In helper
├── LockManager.java                # Biometric session management
├── DailySummaryWorker.java         # Daily summary WorkManager job
├── ExportManager.java              # CSV export via FileProvider
├── AnnounceUPIWidget.java          # Home screen widget provider
├── WidgetDailyWorker.java          # 24-hour widget refresh worker
├── BootReceiver.java               # Restart alarms after reboot
└── WhatsAppActionReceiver.java     # WhatsApp summary sender
```

---

## 🔐 Permissions

| Permission | Purpose |
|-----------|---------|
| 🔔 `BIND_NOTIFICATION_LISTENER_SERVICE` | Detect UPI payment notifications |
| 💬 `RECEIVE_SMS` | SMS fallback payment detection |
| 📖 `READ_SMS` | Read bank SMS content |
| 🌐 `INTERNET` | Google Sign-In, profile picture |
| ⚡ `RECEIVE_BOOT_COMPLETED` | Restart widget alarm after reboot |
| 📳 `VIBRATE` | Haptic feedback on payment |
| 🔔 `POST_NOTIFICATIONS` | Daily summary notification (Android 13+) |
| 🔑 `USE_BIOMETRIC` | App lock via fingerprint / PIN |

---

## 🚀 Getting Started

### Prerequisites
- AndroidIDE or Android Studio
- Android device running Android 6.0+
- JDK 17

### Build from Source

```bash
# Clone the repository
git clone https://github.com/rpy2006/Announceupi.git

# Build debug APK
./gradlew assembleDebug
```

### First Launch Setup

1. **Grant Notification Access**
   Settings → Special app access → Notification access → AnnounceUPI ✓

2. **Grant SMS Permission** *(optional — for SMS fallback)*

3. **Receive a test payment** — the app announces it immediately

### Google Sign-In Setup *(optional)*

```bash
# Get your debug SHA-1 fingerprint
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android -keypass android
```

Then register your app at [console.cloud.google.com](https://console.cloud.google.com) with the SHA-1 and package name `com.announceupi.in`.

---

## 🗄 Data Schema

### Transaction JSON Format

```json
{
  "amount":     "₹500",
  "raw_amount": 500.0,
  "source":     "PhonePe",
  "time":       "10:30 PM",
  "date":       "17 Aug 2026",
  "note":       "Customer payment"
}
```

### Key SharedPreferences

| Key | Default | Purpose |
|-----|---------|---------|
| `dark_mode` | `true` | App theme |
| `tts_language` | `"en"` | Announcement language |
| `tts_speed` | `"normal"` | Speech speed |
| `min_amount` | `0` | Minimum amount to announce |
| `dnd_enabled` | `false` | Do Not Disturb |
| `daily_summary_enabled` | `true` | Evening summary notification |
| `summary_hour` | `21` | Summary time hour |
| `whatsapp_summary_enabled` | `false` | Auto WhatsApp summary |
| `whatsapp_summary_phone` | `""` | WhatsApp recipient number |
| `lock_enabled` | `false` | Biometric app lock |

---

## 📋 Version History

### 🟢 v2.0 — Current
- Bottom navigation — Home, Dashboard, Settings
- Google Sign-In with profile picture
- WhatsApp auto daily summary
- Per-transaction delete button
- Custom daily summary time picker
- Home screen widget with 10s auto-refresh
- Splash screen
- Daily summary no longer counted as a transaction
- Beep sound fixed before announcement
- Fragment caching for smooth, instant navigation

### 🏁 v1.0 — Initial Release
- Core UPI payment announcement
- Basic transaction history
- Dark / Light theme
- SMS fallback detection

---

## ⚠ Known Limitations

- WhatsApp summary opens the app for the user to tap Send — silent auto-send is not possible on Android without root
- Widget refresh is approximately every 10 seconds — Android limits exact alarm frequency for battery efficiency
- Google Sign-In requires SHA-1 registration in Google Cloud Console
- Some banks use non-standard SMS formats — notification listener covers these cases

---

## 👤 Developer

**Rohit Prasad Yadav** — Yadav Enterprises

📧 [rohitprasadyadav06@gmail.com](mailto:rohitprasadyadav06@gmail.com)
📸 [@rohiit.md](https://instagram.com/rohiit.md)
🐙 [github.com/rpy2006](https://github.com/rpy2006)

---

## 📄 License

```
Copyright 2026 Rohit Prasad Yadav — Yadav Enterprises

Licensed under the Apache License, Version 2.0.
You may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<div align="center">

**Built with ❤ using AndroidIDE — entirely on Android, no PC required**

⭐ Star this repo if you found it useful!

</div>
