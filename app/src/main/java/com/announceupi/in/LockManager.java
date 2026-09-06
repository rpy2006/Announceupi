// FILE: app/src/main/java/com/announceupi/in/LockManager.java

package com.announceupi.in;

import android.content.Context;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * Manages biometric / PIN lock.
 *
 * Session design (simple & reliable):
 *  - Static long sessionGrantedAt tracks when the user last authenticated.
 *  - isSessionValid() returns true for 5 minutes after that.
 *  - Session is NEVER cleared on onStop/onPause — only expires by timeout
 *    or when the app process is killed (static field reset).
 *
 * This means:
 *  ✅ Open app → authenticate once → use freely for 5 min
 *  ✅ Open MoreOptions → back → no re-auth (session still valid)
 *  ✅ Press Home → return within 5 min → no re-auth
 *  ✅ Force-close app → reopen → re-auth required
 *  ✅ 5+ minutes idle → re-auth required
 */
public class LockManager {

    private static final String PREF_FILE       = "upi_prefs";
    static final         String KEY_LOCK_ENABLED = "lock_enabled";

    /** Session stays valid for 5 minutes after successful auth */
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000L;

    /** Timestamp of last successful authentication (0 = not authenticated) */
    private static long sessionGrantedAt = 0L;

    // ── Session ───────────────────────────────────────────────────────────

    /** Call immediately after successful authentication */
    public static void grantSession() {
        sessionGrantedAt = System.currentTimeMillis();
    }

    /** True if authenticated within the last SESSION_TIMEOUT_MS */
    public static boolean isSessionValid() {
        if (sessionGrantedAt == 0L) return false;
        return (System.currentTimeMillis() - sessionGrantedAt) < SESSION_TIMEOUT_MS;
    }

    // ── Lock preference ───────────────────────────────────────────────────

    public static boolean isLockEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                  .getBoolean(KEY_LOCK_ENABLED, false);
    }

    public static void setLockEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
           .edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
    }

    // ── Device capability ─────────────────────────────────────────────────

    public static boolean isBiometricAvailable(Context ctx) {
        int result = BiometricManager.from(ctx).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK |
            BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // ── Prompt ────────────────────────────────────────────────────────────

    public interface AuthCallback {
        void onSuccess();
        void onFailed(String reason);
    }

    public static void authenticate(FragmentActivity activity, AuthCallback callback) {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("AnnounceUPI")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build();

        new BiometricPrompt(activity,
            ContextCompat.getMainExecutor(activity),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                        @androidx.annotation.NonNull BiometricPrompt.AuthenticationResult r) {
                    grantSession();
                    callback.onSuccess();
                }
                @Override
                public void onAuthenticationError(int code,
                        @androidx.annotation.NonNull CharSequence err) {
                    callback.onFailed(err.toString());
                }
                @Override
                public void onAuthenticationFailed() {
                    // Finger not recognized — prompt stays open, user can retry
                }
            }
        ).authenticate(info);
    }
}
