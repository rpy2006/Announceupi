// FILE: app/src/main/java/com/announceupi/in/WhatsAppActionReceiver.java
package com.announceupi.in;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.net.URLEncoder;

/**
 * Receives the "Send to WhatsApp" notification action tap.
 * Opens WhatsApp with the pre-filled summary message.
 * Works with both WhatsApp and WhatsApp Business.
 */
public class WhatsAppActionReceiver extends BroadcastReceiver {

    static final String ACTION_SEND_WHATSAPP = "com.announceupi.in.SEND_WHATSAPP";
    static final String EXTRA_PHONE          = "whatsapp_phone";
    static final String EXTRA_MESSAGE        = "whatsapp_message";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ACTION_SEND_WHATSAPP.equals(intent.getAction())) return;

        String phone   = intent.getStringExtra(EXTRA_PHONE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(ctx, "No WhatsApp number set", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clean phone — keep only digits and leading +
        phone = phone.trim().replaceAll("[^\\d+]", "");
        // Add country code if missing (assume India +91)
        if (!phone.startsWith("+") && phone.length() == 10) {
            phone = "+91" + phone;
        }

        try {
            String encoded = URLEncoder.encode(message != null ? message : "", "UTF-8");
            String url     = "https://wa.me/" + phone + "?text=" + encoded;

            // Try WhatsApp app first
            Intent wa = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            wa.setPackage("com.whatsapp");
            wa.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (wa.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(wa);
            } else {
                // Try WhatsApp Business
                wa.setPackage("com.whatsapp.w4b");
                if (wa.resolveActivity(ctx.getPackageManager()) != null) {
                    ctx.startActivity(wa);
                } else {
                    // Fall back to browser wa.me link
                    Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(browser);
                }
            }
        } catch (Exception e) {
            Toast.makeText(ctx, "Could not open WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }
}
