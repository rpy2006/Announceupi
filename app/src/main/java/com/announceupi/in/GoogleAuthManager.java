// FILE: app/src/main/java/com/announceupi/in/GoogleAuthManager.java
package com.announceupi.in;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

/**
 * Centralised Google Sign-In helper.
 *
 * Stores whether the user is logged in plus their basic profile info
 * in SharedPreferences so it persists across app restarts.
 */
public class GoogleAuthManager {

    private static final String PREF_FILE      = "upi_prefs";
    private static final String KEY_LOGGED_IN  = "google_logged_in";
    private static final String KEY_USER_NAME  = "google_user_name";
    private static final String KEY_USER_EMAIL = "google_user_email";
    private static final String KEY_USER_PHOTO = "google_user_photo";
    private static final String KEY_SKIP_LOGIN = "skip_login";

    // ── Build a configured GoogleSignInClient ─────────────────────────────
    public static GoogleSignInClient getClient(Context ctx) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        return GoogleSignIn.getClient(ctx, gso);
    }

    private static String getWebClientId(Context ctx) {
        return ""; // not needed without requestIdToken
    }

    // ── Called after a successful sign-in ─────────────────────────────────
    public static void saveUser(Context ctx, GoogleSignInAccount account) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN,  true)
            .putString(KEY_USER_NAME,   account.getDisplayName() != null
                                        ? account.getDisplayName() : "")
            .putString(KEY_USER_EMAIL,  account.getEmail() != null
                                        ? account.getEmail() : "")
            .putString(KEY_USER_PHOTO,  account.getPhotoUrl() != null
                                        ? account.getPhotoUrl().toString()
                                            .replace("s96-c", "s400-c")
                                            .replace("s48-c", "s400-c") : "")
            .putBoolean(KEY_SKIP_LOGIN, false)
            .apply();
    }

    // ── Called when user taps "Continue without signing in" ───────────────
    public static void setSkipLogin(Context ctx, boolean skip) {
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
           .edit().putBoolean(KEY_SKIP_LOGIN, skip).apply();
    }

    // ── Check if user is authenticated (signed in OR skipped) ─────────────
    public static boolean isAuthenticated(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SKIP_LOGIN, false)) return true;
        // Also check live Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
        if (account != null) {
            saveUser(ctx, account); // refresh stored data
            return true;
        }
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    // ── Sign out completely ───────────────────────────────────────────────
    public static void signOut(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN,  false)
            .putBoolean(KEY_SKIP_LOGIN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .apply();
        getClient(ctx).signOut();
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public static String getUserName(Context ctx) {
        return ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                  .getString(KEY_USER_NAME, "");
    }

    public static String getUserEmail(Context ctx) {
        return ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                  .getString(KEY_USER_EMAIL, "");
    }

    public static String getUserPhoto(Context ctx) {
        return ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                  .getString(KEY_USER_PHOTO, "");
    }

    public static boolean isGoogleSignedIn(Context ctx) {
        return GoogleSignIn.getLastSignedInAccount(ctx) != null;
    }
}
