// FILE: app/src/main/java/com/announceupi/in/BootReceiver.java
package com.announceupi.in;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restarts the widget auto-refresh AlarmManager after phone reboot.
 * AlarmManager alarms are cleared when the phone restarts — this
 * receiver listens for BOOT_COMPLETED and reschedules them.
 * Requires: RECEIVE_BOOT_COMPLETED permission (already in manifest).
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Restart 10-second alarm
            AnnounceUPIWidget.scheduleAutoRefresh(ctx);

            // Restart 24-hour WorkManager worker
            AnnounceUPIWidget.scheduleDailyWorker(ctx);

            // Do an immediate refresh so widget shows current data
            AnnounceUPIWidget.updateAllWidgets(ctx);
        }
    }
}
