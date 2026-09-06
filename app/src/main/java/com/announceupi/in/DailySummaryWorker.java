// FILE: app/src/main/java/com/announceupi/in/DailySummaryWorker.java
package com.announceupi.in;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DailySummaryWorker extends Worker {

    private static final String CHANNEL_ID  = "daily_summary";
    private static final String WORK_NAME   = "daily_summary_work";
    static final         String PREF_FILE   = "upi_prefs";
    static final         String KEY_ENABLED = "daily_summary_enabled";
    static final         String KEY_HOUR    = "summary_hour";
    static final         String KEY_MINUTE  = "summary_minute";
    static final         String KEY_WA_ENABLED = "whatsapp_summary_enabled";
    static final         String KEY_WA_PHONE   = "whatsapp_summary_phone";

    private static final int NOTIF_ID_SUMMARY  = 1001;
    private static final int NOTIF_ID_WHATSAPP = 1002;

    public DailySummaryWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx   = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_ENABLED, true)) return Result.success();

        double total = 0;
        int    count = 0;
        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

        try {
            JSONArray arr = new JSONArray(prefs.getString("transactions", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject txn = arr.getJSONObject(i);
                if (today.equalsIgnoreCase(txn.optString("date", ""))) {
                    total += txn.optDouble("raw_amount", 0);
                    count++;
                }
            }
        } catch (Exception ignored) {}

        if (count == 0) return Result.success();

        String amt     = "\u20B9" + String.format(Locale.getDefault(), "%,.2f", total);
        String title   = "Today's Payment Summary";
        String message = "You received " + amt + " from "
            + count + " payment" + (count == 1 ? "" : "s") + " today";

        // Build WhatsApp message (richer format)
        String waMessage = buildWhatsAppMessage(today, amt, count, total, prefs);

        // Show notification (with WhatsApp action if enabled)
        boolean waEnabled = prefs.getBoolean(KEY_WA_ENABLED, false);
        String  waPhone   = prefs.getString(KEY_WA_PHONE, "").trim();

        sendSummaryNotification(ctx, title, message,
            waEnabled && !waPhone.isEmpty(), waPhone, waMessage);

        return Result.success();
    }

    // ── Build a nicely formatted WhatsApp message ─────────────────────────
    private String buildWhatsAppMessage(String date, String amt, int count,
                                        double total, SharedPreferences prefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCB0 *AnnounceUPI Daily Summary*\n");
        sb.append("\uD83D\uDCC5 ").append(date).append("\n\n");
        sb.append("\u2705 Total Received: *").append(amt).append("*\n");
        sb.append("\uD83D\uDD22 Transactions: *").append(count).append("*\n\n");

        // Add payment app breakdown
        try {
            JSONArray arr = new JSONArray(prefs.getString("transactions", "[]"));
            java.util.Map<String, Double> byApp = new java.util.LinkedHashMap<>();
            String today = date;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject txn = arr.getJSONObject(i);
                if (today.equalsIgnoreCase(txn.optString("date", ""))) {
                    String src = txn.optString("source", "Other");
                    byApp.put(src, byApp.getOrDefault(src, 0.0)
                        + txn.optDouble("raw_amount", 0));
                }
            }
            if (!byApp.isEmpty()) {
                sb.append("\uD83D\uDCCA *Breakdown:*\n");
                for (java.util.Map.Entry<String, Double> e : byApp.entrySet()) {
                    sb.append("  \u2022 ").append(e.getKey()).append(": \u20B9")
                      .append(String.format(Locale.getDefault(), "%,.2f", e.getValue()))
                      .append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception ignored) {}

        sb.append("_Sent by AnnounceUPI_");
        return sb.toString();
    }

    // ── Notification with optional WhatsApp action button ─────────────────
    private void sendSummaryNotification(Context ctx, String title, String message,
                                         boolean showWaAction, String waPhone,
                                         String waMessage) {
        NotificationManager nm =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Daily Summary", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Daily payment summary");
            nm.createNotificationChannel(ch);
        }

        // Tap notification → open app
        Intent launchIntent = new Intent(ctx, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openAppPi = PendingIntent.getActivity(ctx, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add WhatsApp action button if enabled
        if (showWaAction) {
            Intent waIntent = new Intent(ctx, WhatsAppActionReceiver.class);
            waIntent.setAction(WhatsAppActionReceiver.ACTION_SEND_WHATSAPP);
            waIntent.putExtra(WhatsAppActionReceiver.EXTRA_PHONE,   waPhone);
            waIntent.putExtra(WhatsAppActionReceiver.EXTRA_MESSAGE, waMessage);
            PendingIntent waPi = PendingIntent.getBroadcast(ctx, 42, waIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(0, "\uD83D\uDCAC Send to WhatsApp", waPi);
        }

        nm.notify(NOTIF_ID_SUMMARY, builder.build());

        // If WhatsApp auto-send enabled, also fire it automatically
        if (showWaAction) {
            Intent autoWa = new Intent(ctx, WhatsAppActionReceiver.class);
            autoWa.setAction(WhatsAppActionReceiver.ACTION_SEND_WHATSAPP);
            autoWa.putExtra(WhatsAppActionReceiver.EXTRA_PHONE,   waPhone);
            autoWa.putExtra(WhatsAppActionReceiver.EXTRA_MESSAGE, waMessage);
            ctx.sendBroadcast(autoWa);
        }
    }

    // ── Schedule / reschedule ─────────────────────────────────────────────
    public static void schedule(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        int hour   = prefs.getInt(KEY_HOUR,   21);
        int minute = prefs.getInt(KEY_MINUTE,  0);

        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE,      minute);
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= now)
            cal.add(Calendar.DAY_OF_YEAR, 1);
        long delay = cal.getTimeInMillis() - now;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            DailySummaryWorker.class, 24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request);
    }
}
