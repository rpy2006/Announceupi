// FILE: app/src/main/java/com/announceupi/in/PaymentNotificationService.java

package com.announceupi.in;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentNotificationService extends NotificationListenerService {

    private static final String TAG       = "AnnounceUPI";
    private static final String PREF_FILE = "upi_prefs";
    private static final String KEY_LANG  = "tts_language";
    private static final String KEY_HAPTIC= "haptic_enabled";

    // ── TTS ───────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean      ttsReady  = false;

    // Pending speech — queued when TTS not ready yet
    private String       pendingSpeech = null;
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());

    // ── Amount regex ──────────────────────────────────────────────────────
    // Matches: ₹500  Rs.500  Rs 500  INR500  INR 500  500 rupees  500.00 INR
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:₹|rs\\.?\\s*|inr\\s*)([\\d,]+(?:\\.\\d{1,2})?)" +
        "|([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:inr|rupees)",
        Pattern.CASE_INSENSITIVE
    );

    // ── Known UPI / payment apps — only need amount + no debit keyword ────
    private static final Set<String> UPI_WHITELIST = new HashSet<>(Arrays.asList(
        "com.phonepe.app",
        "com.phonepe.app.preprod",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",    // Google Pay
        "in.amazon.mShop.android.shopping",           // Amazon Pay
        "com.mobikwik_new",
        "com.freecharge.android",
        "com.whatsapp",                                // WhatsApp Pay
        "com.axis.mobile",
        "com.csam.icici.bank.imobile",                // ICICI iMobile
        "com.sbi.SBIFreedomPlus",
        "com.sbi.lotusintouch",                       // YONO SBI
        "com.hdfcbank.hdfcbankmobilebanking",
        "com.idbi.mPassbook",
        "com.kotak.mahindra.kotak811",
        "in.org.npci.upiapp",                         // BHIM
        "com.dreamplug.androidapp",                   // CRED
        "com.boi.mobile",
        "com.epifi.money",                            // Fi Money
        "com.msf.kash"                                // Jupiter
    ));

    // ── Hard block — never announce from these ────────────────────────────
    private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
        "com.announceupi.in",              // OWN APP — prevents daily summary being saved as transaction
        "com.google.android.gm",
        "com.microsoft.teams",
        "com.slack",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "org.telegram.messenger",
        "com.twitter.android",
        "com.facebook.katana",
        "com.instagram.android",
        "com.snapchat.android"
    ));

    // ── Credit keywords ───────────────────────────────────────────────────
    private static final Set<String> CREDIT_KEYWORDS = new HashSet<>(Arrays.asList(
        "credited", "received", "money received", "payment received",
        "added to", "money added", "amount added", "loaded to",
        "deposited", "topped up", "credit", "cashback received",
        "refund", "reward credited", "transfer received",
        "cr ", "cr.", "a/c cr", "acct cr", "account credited",
        "has been credited", "is credited",
        "sent you", "paid you", "transferred to you",
        "from upi", "upi credit", "upi ref"
    ));

    // ── Debit keywords ────────────────────────────────────────────────────
    private static final Set<String> DEBIT_KEYWORDS = new HashSet<>(Arrays.asList(
        "debited", "sent to", "paid to", "deducted", "payment sent",
        "transfer sent", "failed", "declined", "pending",
        "refund initiated", "payment failed", "insufficient"
    ));

    // ── Spam — keep SHORT, false positives silence real payments ──────────
    private static final Set<String> SPAM_KEYWORDS = new HashSet<>(Arrays.asList(
        "lucky draw", "scratch card", "coupon", "limited time offer",
        "install now", "click here", "download now", "win prizes",
        "get cashback on", "flat off on"
    ));

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        initTTS();
        Log.d(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        ttsReady = false;
        super.onDestroy();
    }

    // ── TTS init ──────────────────────────────────────────────────────────
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Force TTS to use the MUSIC stream so it plays regardless
                // of ringer mode (silent/vibrate won't block it)
                tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
                applyLanguage();
                ttsReady = true;
                Log.d(TAG, "TTS ready ✅");

                // Speak anything queued before TTS was ready
                if (pendingSpeech != null) {
                    String toSpeak = pendingSpeech;
                    pendingSpeech  = null;
                    mainHandler.postDelayed(() -> speakNow(toSpeak), 300);
                }
            } else {
                Log.e(TAG, "TTS init FAILED — status=" + status);
                ttsReady = false;
                // Retry after 4 seconds
                mainHandler.postDelayed(() -> {
                    if (!ttsReady) initTTS();
                }, 4000);
            }
        });
    }

    private void applyLanguage() {
        if (tts == null) return;
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        String langCode = prefs.getString(KEY_LANG, "en");
        Locale locale   = getLocaleForCode(langCode);
        int avail = tts.isLanguageAvailable(locale);
        if (avail >= TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
        } else {
            tts.setLanguage(Locale.US);
            Log.w(TAG, "Lang unavailable: " + langCode + " — fallback English");
        }
    }

    private static Locale getLocaleForCode(String code) {
        switch (code) {
            case "hi": return new Locale("hi", "IN");
            case "ta": return new Locale("ta", "IN");
            case "te": return new Locale("te", "IN");
            case "kn": return new Locale("kn", "IN");
            case "bn": return new Locale("bn", "IN");
            case "mr": return new Locale("mr", "IN");
            case "ml": return new Locale("ml", "IN");
            case "gu": return new Locale("gu", "IN");
            case "pa": return new Locale("pa", "IN");
            default:   return Locale.US;
        }
    }

    // ── Main notification handler ─────────────────────────────────────────
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String pkg = sbn.getPackageName();
        if (pkg == null || BLACKLIST.contains(pkg)) return;

        Notification notif = sbn.getNotification();
        if (notif == null) return;

        // Skip group summary notifications (avoid duplicate announcements)
        if ((notif.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        // Extract all text from the notification
        String title   = safeExtra(notif, Notification.EXTRA_TITLE);
        String text    = safeExtra(notif, Notification.EXTRA_TEXT);
        String bigText = safeExtra(notif, Notification.EXTRA_BIG_TEXT);
        String subText = safeExtra(notif, Notification.EXTRA_SUB_TEXT);
        String infoText= safeExtra(notif, Notification.EXTRA_INFO_TEXT);

        String combined = (title + " " + text + " " + bigText + " "
                          + subText + " " + infoText).toLowerCase().trim();

        if (combined.isEmpty()) return;

        Log.d(TAG, "Notification from [" + pkg + "]: " + combined);

        // Extract amount first — no amount = nothing to announce
        double amount = extractAmount(combined);
        if (amount <= 0) {
            Log.d(TAG, "No amount found — skipped");
            return;
        }

        // Decide if this is a credit transaction
        boolean isWhitelisted = UPI_WHITELIST.contains(pkg);
        if (isWhitelisted) {
            if (isDebit(combined)) {
                Log.d(TAG, "Debit detected on whitelisted app — skipped");
                return;
            }
        } else {
            if (!isCreditPayment(combined)) {
                Log.d(TAG, "Not a credit payment — skipped");
                return;
            }
        }

        if (isSpam(combined)) {
            Log.d(TAG, "Spam detected — skipped");
            return;
        }

        // Min amount filter
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        float minAmount = sp.getFloat("min_amount", 0f);
        if ((float) amount < minAmount) {
            Log.d(TAG, "Below min amount " + minAmount + " — skipped");
            return;
        }

        // Do Not Disturb
        if (sp.getBoolean("dnd_enabled", false)) {
            int hour     = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            int dndStart = sp.getInt("dnd_start", 22);
            int dndEnd   = sp.getInt("dnd_end", 7);
            boolean inDnd = (dndStart > dndEnd)
                ? (hour >= dndStart || hour < dndEnd)
                : (hour >= dndStart && hour < dndEnd);
            if (inDnd) {
                Log.d(TAG, "DND active — saving silently");
                MainActivity.saveTransaction(this, amount, resolveAppName(pkg));
                return;
            }
        }

        String source = resolveAppName(pkg);
        Log.d(TAG, "MATCH: ₹" + amount + " from " + source);
        MainActivity.saveTransaction(this, amount, source);
        triggerHaptic();
        announcePayment(amount);
    }

    // ── Payment classification ────────────────────────────────────────────
    private boolean isDebit(String text) {
        for (String kw : DEBIT_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isCreditPayment(String text) {
        for (String kw : DEBIT_KEYWORDS) {
            if (text.contains(kw)) return false;
        }
        for (String kw : CREDIT_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isSpam(String text) {
        for (String kw : SPAM_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ── Amount extraction ─────────────────────────────────────────────────
    private double extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        while (m.find()) {
            String raw = m.group(1) != null ? m.group(1) : m.group(2);
            if (raw == null) continue;
            try {
                double val = Double.parseDouble(raw.replace(",", ""));
                if (val >= 1 && val <= 10_000_000) return val; // ₹1 – ₹1Cr
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    // ── Announcement ──────────────────────────────────────────────────────
    private void announcePayment(double amount) {
        String speechText = buildSpeechText(amount);
        Log.d(TAG, "announcePayment called: " + speechText
              + " | ttsReady=" + ttsReady);

        if (!ttsReady || tts == null) {
            // TTS not ready yet — save and speak when it finishes initializing
            Log.w(TAG, "TTS not ready — queuing: " + speechText);
            pendingSpeech = speechText;
            if (tts == null) initTTS();
            return;
        }

        speakNow(speechText);
    }

    private void speakNow(String text) {
        if (tts == null || !ttsReady) {
            Log.e(TAG, "speakNow called but TTS null/not ready");
            return;
        }

        SharedPreferences p = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        String  speed  = p.getString("tts_speed", "normal");
        boolean repeat = p.getBoolean("tts_repeat", false);
        float   rate   = speed.equals("slow") ? 0.6f
                       : speed.equals("fast") ? 1.5f : 1.0f;

        applyLanguage();
        tts.setSpeechRate(rate);
        tts.setPitch(1.0f);

        // Request audio focus so TTS is heard over music/calls
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest req = new AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                    .setAcceptsDelayedFocusGain(false)
                    .build();
                am.requestAudioFocus(req);
            } else {
                am.requestAudioFocus(null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        }

        // Use Bundle to set stream so TTS plays even on silent (media channel)
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM,
                      AudioManager.STREAM_MUSIC);

        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "upi_" + System.currentTimeMillis());
        Log.d(TAG, "tts.speak result=" + result + " text=" + text);

        if (repeat) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, "upi_repeat");
        }
    }

    // ── Build speech text ─────────────────────────────────────────────────
    private String buildSpeechText(double amount) {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, "en");

        long   rupees = (long) amount;
        int    paise  = (int) Math.round((amount - rupees) * 100);
        String amtStr = String.valueOf(rupees);

        switch (lang) {
            case "hi": return "Paisa aaya " + amtStr + " rupaye";
            case "ta": return "Panam vandhadu " + amtStr + " rubai";
            case "te": return "Chellinci " + amtStr + " rupaayalu";
            case "kn": return "Hana bandide " + amtStr + " rupai";
            case "bn": return "Taka esheche " + amtStr + " taka";
            case "mr": return "Paise aale " + amtStr + " rupaye";
            case "ml": return "Panam kittichu " + amtStr + " rupaye";
            case "gu": return "Chukvanyu " + amtStr + " rupiya";
            case "pa": return "Paisa aaya " + amtStr + " rupaye";
            default:   return paise > 0
                ? "Payment received " + amtStr + " rupees " + paise + " paise"
                : "Payment received " + amtStr + " rupees";
        }
    }

    // ── Haptic feedback ───────────────────────────────────────────────────
    private void triggerHaptic() {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAPTIC, true)) return;
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator == null || !vibrator.hasVibrator()) return;
            long[] pattern = {0, 120, 80, 180};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Haptic error: " + e.getMessage());
        }
    }

    // ── App name resolver ─────────────────────────────────────────────────
    private String resolveAppName(String pkg) {
        if (pkg == null) return "Unknown";
        switch (pkg) {
            case "com.phonepe.app":                         return "PhonePe";
            case "net.one97.paytm":                         return "Paytm";
            case "com.google.android.apps.nbu.paisa.user":  return "Google Pay";
            case "in.org.npci.upiapp":                      return "BHIM";
            case "in.amazon.mShop.android.shopping":        return "Amazon Pay";
            case "com.whatsapp":                            return "WhatsApp Pay";
            case "com.dreamplug.androidapp":                return "CRED";
            case "com.sbi.lotusintouch":                    return "YONO SBI";
            case "com.csam.icici.bank.imobile":             return "iMobile Pay";
            case "com.axis.mobile":                         return "Axis Bank";
            case "com.mobikwik_new":                        return "MobiKwik";
            case "com.freecharge.android":                  return "FreeCharge";
            case "com.epifi.money":                         return "Fi Money";
            case "com.msf.kash":                            return "Jupiter";
            default:
                try {
                    return getPackageManager()
                        .getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0))
                        .toString();
                } catch (Exception e) {
                    return pkg.substring(pkg.lastIndexOf('.') + 1);
                }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String safeExtra(Notification n, String key) {
        Bundle extras = n.extras;
        if (extras == null) return "";
        CharSequence val = extras.getCharSequence(key);
        return val != null ? val.toString() : "";
    }

    public static Locale getLocaleForSms(String code) {
        switch (code) {
            case "hi": return new Locale("hi", "IN");
            case "ta": return new Locale("ta", "IN");
            case "te": return new Locale("te", "IN");
            case "kn": return new Locale("kn", "IN");
            case "bn": return new Locale("bn", "IN");
            case "mr": return new Locale("mr", "IN");
            case "ml": return new Locale("ml", "IN");
            case "gu": return new Locale("gu", "IN");
            case "pa": return new Locale("pa", "IN");
            default:   return Locale.US;
        }
    }
}
