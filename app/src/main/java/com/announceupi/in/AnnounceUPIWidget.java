// FILE: app/src/main/java/com/announceupi/in/AnnounceUPIWidget.java
package com.announceupi.in;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnounceUPIWidget extends AppWidgetProvider {

    private static final String PREF_FILE        = "upi_prefs";
    private static final String KEY_TRANSACTIONS = "transactions";

    // Broadcast actions
    static final String ACTION_REFRESH      = "com.announceupi.in.WIDGET_REFRESH";
    static final String ACTION_AUTO_REFRESH = "com.announceupi.in.WIDGET_AUTO_REFRESH";

    // AlarmManager request code
    private static final int ALARM_REQUEST_CODE = 1001;

    // ── Widget placed / screen on ─────────────────────────────────────────
    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
        scheduleAutoRefresh(ctx);     // start 10-second ticker
    }

    // ── First widget placed ───────────────────────────────────────────────
    @Override
    public void onEnabled(Context ctx) {
        super.onEnabled(ctx);
        updateAllWidgets(ctx);
        scheduleAutoRefresh(ctx);
        scheduleDailyWorker(ctx);    // WorkManager 24-hour refresh
    }

    // ── Last widget removed ───────────────────────────────────────────────
    @Override
    public void onDisabled(Context ctx) {
        super.onDisabled(ctx);
        cancelAutoRefresh(ctx);      // stop alarm when no widgets remain
        WorkManager.getInstance(ctx).cancelUniqueWork("widget_daily_update");
    }

    // ── Receive broadcasts ────────────────────────────────────────────────
    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action) || ACTION_AUTO_REFRESH.equals(action)) {
            updateAllWidgets(ctx);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  10-SECOND AUTO REFRESH via AlarmManager repeating broadcast
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Schedule a repeating AlarmManager alarm every 10 seconds.
     * Sends ACTION_AUTO_REFRESH → onReceive → updateAllWidgets.
     * Only active while at least one widget is on the home screen.
     */
    static void scheduleAutoRefresh(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = getAutoRefreshPendingIntent(ctx);

        // INTERVAL_FIFTEEN_MINUTES is Android's minimum for exact alarms,
        // but for inexact repeating we can use any interval including 10 seconds.
        am.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + 10_000L,   // first fire: 10s from now
            10_000L,                                  // repeat every 10 seconds
            pi
        );
    }

    static void cancelAutoRefresh(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(getAutoRefreshPendingIntent(ctx));
    }

    private static PendingIntent getAutoRefreshPendingIntent(Context ctx) {
        Intent intent = new Intent(ctx, AnnounceUPIWidget.class);
        intent.setAction(ACTION_AUTO_REFRESH);
        return PendingIntent.getBroadcast(
            ctx, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    //  24-HOUR DAILY UPDATE via WorkManager (survives reboots & app kills)
    // ═════════════════════════════════════════════════════════════════════

    static void scheduleDailyWorker(Context ctx) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            WidgetDailyWorker.class, 24, TimeUnit.HOURS)
            .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "widget_daily_update",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PUBLIC: called after every payment save
    // ═════════════════════════════════════════════════════════════════════

    public static void updateAllWidgets(Context ctx) {
        AppWidgetManager mgr  = AppWidgetManager.getInstance(ctx);
        ComponentName    comp = new ComponentName(ctx, AnnounceUPIWidget.class);
        int[]            ids  = mgr.getAppWidgetIds(comp);
        if (ids.length == 0) return;  // no widgets — skip
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CORE UPDATE: reads prefs and pushes data to RemoteViews
    // ═════════════════════════════════════════════════════════════════════

    static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_layout);

        double todayTotal = 0, allTotal = 0;
        int    todayCount = 0, allCount = 0;
        String lastAmount = "", lastSource = "";

        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(new Date());

        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(prefs.getString(KEY_TRANSACTIONS, "[]"));
            allCount = arr.length();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject txn = arr.getJSONObject(i);
                double amt = txn.optDouble("raw_amount", 0);
                allTotal += amt;
                if (i == 0) {
                    lastAmount = txn.optString("amount", "");
                    lastSource = txn.optString("source", "");
                }
                if (today.equalsIgnoreCase(txn.optString("date", ""))) {
                    todayTotal += amt;
                    todayCount++;
                }
            }
        } catch (Exception ignored) {}

        // Format
        String countText = todayCount == 0 && allCount == 0
            ? "No payments yet"
            : todayCount == 0
                ? allCount + " total payment" + (allCount == 1 ? "" : "s")
                : todayCount + " payment" + (todayCount == 1 ? "" : "s") + " today";

        String lastText = lastAmount.isEmpty()
            ? "Waiting for payment\u2026"
            : "Last: " + lastAmount + " \u2022 " + lastSource;

        // Show last-refreshed time so user knows it's live
        String refreshTime = new SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(new Date());

        // Push to views
        views.setTextViewText(R.id.widgetAmount, formatRupee(todayTotal));
        views.setTextViewText(R.id.widgetCount,  countText);
        views.setTextViewText(R.id.widgetDate,   today + "  \u2022  " + refreshTime);
        views.setTextViewText(R.id.widgetLast,   lastText);

        // Tap widget → open app
        Intent launch = new Intent(ctx, MainActivity.class);
        launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openApp = PendingIntent.getActivity(
            ctx, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetRoot, openApp);

        // Tap refresh icon → manual refresh
        Intent refreshIntent = new Intent(ctx, AnnounceUPIWidget.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPi = PendingIntent.getBroadcast(
            ctx, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetRefresh, refreshPi);

        mgr.updateAppWidget(widgetId, views);
    }

    private static String formatRupee(double amount) {
        if (amount == (long) amount)
            return "\u20B9" + String.format(Locale.getDefault(), "%,.0f", amount);
        return "\u20B9" + String.format(Locale.getDefault(), "%,.2f", amount);
    }
}
