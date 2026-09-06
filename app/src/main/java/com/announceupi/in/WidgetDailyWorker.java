// FILE: app/src/main/java/com/announceupi/in/WidgetDailyWorker.java
package com.announceupi.in;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Runs once every 24 hours via WorkManager.
 * Forces a full widget data refresh — useful for the date rollover
 * at midnight (today's total resets to zero) and to ensure the widget
 * stays accurate even after the phone was idle all night.
 *
 * The 10-second AlarmManager ticker handles live updates while the
 * phone is active. This worker is the safety net for long idle periods.
 */
public class WidgetDailyWorker extends Worker {

    public WidgetDailyWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Full widget refresh — reads latest prefs and pushes to all widgets
        AnnounceUPIWidget.updateAllWidgets(getApplicationContext());

        // Also restart the AlarmManager ticker in case it was killed
        AnnounceUPIWidget.scheduleAutoRefresh(getApplicationContext());

        return Result.success();
    }
}
