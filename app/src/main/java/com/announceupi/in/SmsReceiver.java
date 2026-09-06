// FILE: app/src/main/java/com/announceupi/in/SmsReceiver.java

package com.announceupi.in;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SMS FALLBACK RECEIVER
 * ─────────────────────
 * Catches bank SMS messages when the UPI app does NOT post a system notification.
 * Common with older bank apps (SBI, BOB, Canara etc.) that send only SMS for credits.
 *
 * Registered in AndroidManifest with priority 999 so it runs before the system SMS app.
 * Requires READ_SMS + RECEIVE_SMS permissions.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG       = "AnnounceUPI_SMS";
    private static final String PREF_FILE = "upi_prefs";
    private static final String KEY_SMS   = "sms_fallback_enabled";
    private static final String KEY_HAPTIC= "haptic_enabled";
    private static final String KEY_LANG  = "tts_language";

    // Known bank SMS sender IDs (short codes)
    private static final Set<String> BANK_SENDERS = new HashSet<>(Arrays.asList(
        "SBIINB", "SBIPSG", "HDFCBK", "ICICIB", "AXISBK", "KOTAKB",
        "PNBSMS", "BOIIND", "CANBNK", "UNIONB", "INDBNK", "IDBIBN",
        "PAYTMB", "PHONEPE", "GPAY", "AMAZONP", "YESBNK", "IDBBNK",
        "CENTBK", "SYNDBK", "ALLBNK", "OBCBNK", "VIJBNK"
    ));

    // UPI credit keywords to look for in bank SMS
    private static final Set<String> SMS_CREDIT_KEYWORDS = new HashSet<>(Arrays.asList(
        "credited", "credit", "received", "deposited",
        "upi", "imps", "neft", "rtgs", "added to your account",
        "money received", "payment received", "transfer received",
        "debited from", "a/c.*credited"                  // some banks format
    ));

    // Debit / block keywords
    private static final Set<String> SMS_DEBIT_KEYWORDS = new HashSet<>(Arrays.asList(
        "debited", "deducted", "withdrawn", "payment of", "spent",
        "failed", "declined", "reversed", "blocked", "otp", "password",
        "login", "pin", "cvv", "security", "alert: your"
    ));

    // Amount extraction regex (same as notification service)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:₹|rs\\.?\\s?|inr\\s?|rs\\s)([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    );

    // Simple TTS for SMS (created fresh, released after speak)
    private TextToSpeech smsTts;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        // Check if SMS fallback is enabled by the user
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_SMS, true)) return;

        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        String   format = intent.getStringExtra("format");
        if (pdus == null || pdus.length == 0) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (sms == null) continue;

            String sender  = sms.getOriginatingAddress();
            String body    = sms.getMessageBody();

            if (sender == null || body == null) continue;
            String senderUpper = sender.toUpperCase().replaceAll("[^A-Z0-9]", "");

            // Only process messages from known bank senders
            if (!isKnownBankSender(senderUpper)) continue;

            String lowerBody = body.toLowerCase();
            if (!isCreditSms(lowerBody)) continue;

            double amount = extractAmount(lowerBody);
            if (amount <= 0) continue;

            String bankName = resolveBankName(senderUpper);
            Log.d(TAG, "SMS credit detected from " + bankName + ": ₹" + amount);

            // Save transaction (same SharedPreferences as notification path)
            MainActivity.saveTransaction(context, amount, bankName + " (SMS)");

            // Announce via TTS
            speakSmsPayment(context, amount, prefs.getString(KEY_LANG, "en"));

            // Haptic
            if (prefs.getBoolean(KEY_HAPTIC, true)) {
                triggerHaptic(context);
            }
        }
    }

    // ── Credit / spam check ───────────────────────────────────────────────
    private boolean isCreditSms(String text) {
        for (String kw : SMS_DEBIT_KEYWORDS) {
            if (text.contains(kw)) return false;
        }
        for (String kw : SMS_CREDIT_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isKnownBankSender(String sender) {
        for (String s : BANK_SENDERS) {
            if (sender.contains(s)) return true;
        }
        return false;
    }

    // ── Amount extraction ─────────────────────────────────────────────────
    private double extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) { /* ignore */ }
        }
        return 0;
    }

    // ── TTS for SMS (fresh instance each time) ────────────────────────────
    private void speakSmsPayment(Context ctx, double amount, String langCode) {
        smsTts = new TextToSpeech(ctx, status -> {
            if (status != TextToSpeech.SUCCESS) return;

            Locale locale = PaymentNotificationService.getLocaleForSms(langCode);
            if (smsTts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                smsTts.setLanguage(locale);
            } else {
                smsTts.setLanguage(Locale.US);
            }

            String text = buildSpeechText(amount, langCode);

            try {
                smsTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sms_announce");
            } catch (Exception e) {
                Log.e(TAG, "SMS TTS error: " + e.getMessage());
            }
        });
    }

    // ── Build speech string (same logic as notification service) ──────────
    private String buildSpeechText(double amount, String lang) {
        long  rupees = (long) amount;
        int   paise  = (int) Math.round((amount - rupees) * 100);
        String amt   = String.valueOf(rupees);

        switch (lang) {
            case "hi": return paise > 0 ? "Paisa aaya " + amt + " rupaye " + paise + " paise"
                                        : "Paisa aaya " + amt + " rupaye";
            case "ta": return paise > 0 ? "Panam vandhadu " + amt + " rubai " + paise + " paisa"
                                        : "Panam vandhadu " + amt + " rubai";
            case "te": return paise > 0 ? "Chellinci " + amt + " rupaayalu " + paise + " paisalu"
                                        : "Chellinci " + amt + " rupaayalu";
            case "kn": return paise > 0 ? "Hana bandide " + amt + " rupai " + paise + " paisa"
                                        : "Hana bandide " + amt + " rupai";
            case "bn": return paise > 0 ? "Taka esheche " + amt + " taka " + paise + " paisa"
                                        : "Taka esheche " + amt + " taka";
            case "mr": return paise > 0 ? "Paise aale " + amt + " rupaye " + paise + " paise"
                                        : "Paise aale " + amt + " rupaye";
            default:   return paise > 0 ? "Payment received " + amt + " rupees " + paise + " paise"
                                        : "Payment received " + amt + " rupees";
        }
    }

    // ── Haptic ────────────────────────────────────────────────────────────
    private void triggerHaptic(Context ctx) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) return;
            long[] pattern = {0, 120, 80, 180};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (Exception ignored) {}
    }

    // ── Bank name resolver ────────────────────────────────────────────────
    private String resolveBankName(String sender) {
        if (sender.contains("SBI"))    return "SBI";
        if (sender.contains("HDFC"))   return "HDFC Bank";
        if (sender.contains("ICICI"))  return "ICICI Bank";
        if (sender.contains("AXIS"))   return "Axis Bank";
        if (sender.contains("KOTAK"))  return "Kotak Bank";
        if (sender.contains("PNB"))    return "PNB";
        if (sender.contains("BOI"))    return "Bank of India";
        if (sender.contains("CAN"))    return "Canara Bank";
        if (sender.contains("UNION"))  return "Union Bank";
        if (sender.contains("YES"))    return "Yes Bank";
        if (sender.contains("PAYTM"))  return "Paytm";
        if (sender.contains("PHONE"))  return "PhonePe";
        if (sender.contains("GPAY"))   return "Google Pay";
        return "Bank";
    }
}
