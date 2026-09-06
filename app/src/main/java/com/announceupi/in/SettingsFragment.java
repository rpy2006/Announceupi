// FILE: app/src/main/java/com/announceupi/in/SettingsFragment.java
package com.announceupi.in;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private TextView tvPermNotifStatus, tvPermSmsStatus, tvPermPostNotifStatus;
    private TextView tvLanguageValue, tvSpeedValue, tvMinAmountValue, tvDndTime, tvSummaryTime;
    private TextView tvUserName, tvUserEmail, tvUserAvatar, tvAuthStatus, tvSignInOutLabel;
    private android.widget.ImageView ivUserPhoto;
    private SwitchCompat switchDarkMode, switchRepeat, switchDnd, switchDailySummary,
                         switchAppLock, switchWhatsApp;
    private TextView tvWhatsAppNumber;

    private final String[] LANGUAGES = {"English","Hindi","Tamil","Telugu","Kannada","Bengali","Marathi","Malayalam","Gujarati","Punjabi"};
    private final String[] LANG_CODES= {"en","hi","ta","te","kn","bn","mr","ml","gu","pa"};
    private final String[] SPEEDS    = {"Slow","Normal","Fast"};
    private final String[] SPEED_KEYS= {"slow","normal","fast"};
    private final float[]  MIN_AMOUNTS = {0,1,10,50,100,500,1000};
    private final String[] MIN_LABELS  = {"₹0 (all)","₹1+","₹10+","₹50+","₹100+","₹500+","₹1000+"};

    private final ActivityResultLauncher<String> smsPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
            updatePermissionStatuses());

    private final ActivityResultLauncher<String> postNotifLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
            updatePermissionStatuses());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = requireContext().getSharedPreferences(MainActivity.PREF_FILE, 0);

        bindViews(root);
        loadValues();
        setupListeners(root);
        updatePermissionStatuses();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionStatuses();
    }

    private void bindViews(View root) {
        tvPermNotifStatus      = root.findViewById(R.id.tvPermNotifStatus);
        tvPermSmsStatus        = root.findViewById(R.id.tvPermSmsStatus);
        tvPermPostNotifStatus  = root.findViewById(R.id.tvPermPostNotifStatus);
        tvLanguageValue        = root.findViewById(R.id.tvLanguageValue);
        tvSpeedValue           = root.findViewById(R.id.tvSpeedValue);
        tvMinAmountValue       = root.findViewById(R.id.tvMinAmountValue);
        tvDndTime              = root.findViewById(R.id.tvDndTime);
        tvSummaryTime          = root.findViewById(R.id.tvSummaryTime);
        tvUserName             = root.findViewById(R.id.tvUserName);
        tvUserEmail            = root.findViewById(R.id.tvUserEmail);
        tvUserAvatar           = root.findViewById(R.id.tvUserAvatar);
        tvAuthStatus           = root.findViewById(R.id.tvAuthStatus);
        tvSignInOutLabel       = root.findViewById(R.id.tvSignInOutLabel);
        ivUserPhoto            = root.findViewById(R.id.ivUserPhoto);
        switchDarkMode         = root.findViewById(R.id.switchDarkMode);
        switchRepeat           = root.findViewById(R.id.switchRepeat);
        switchDnd              = root.findViewById(R.id.switchDnd);
        switchDailySummary     = root.findViewById(R.id.switchDailySummary);
        switchAppLock          = root.findViewById(R.id.switchAppLock);
        switchWhatsApp         = root.findViewById(R.id.switchWhatsApp);
        tvWhatsAppNumber       = root.findViewById(R.id.tvWhatsAppNumber);
    }

    private void loadValues() {
        // Dark mode
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", true));
        // Account
        updateAccountUI();

        // Language
        String lang = prefs.getString("tts_language", "en");
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(lang)) { tvLanguageValue.setText(LANGUAGES[i]); break; }
        }

        // Speed
        String speed = prefs.getString("tts_speed", "normal");
        for (int i = 0; i < SPEED_KEYS.length; i++) {
            if (SPEED_KEYS[i].equals(speed)) { tvSpeedValue.setText(SPEEDS[i]); break; }
        }

        // Repeat
        switchRepeat.setChecked(prefs.getBoolean("tts_repeat", false));

        // Min amount
        float minAmt = prefs.getFloat("min_amount", 0f);
        for (int i = 0; i < MIN_AMOUNTS.length; i++) {
            if (MIN_AMOUNTS[i] == minAmt) { tvMinAmountValue.setText(MIN_LABELS[i]); break; }
        }

        // DND
        switchDnd.setChecked(prefs.getBoolean("dnd_enabled", false));
        updateDndTimeLabel();

        // WhatsApp summary
        switchWhatsApp.setChecked(prefs.getBoolean(DailySummaryWorker.KEY_WA_ENABLED, false));
        String waPhone = prefs.getString(DailySummaryWorker.KEY_WA_PHONE, "").trim();
        tvWhatsAppNumber.setText(waPhone.isEmpty() ? "Not set" : waPhone);

        // Daily summary
        switchDailySummary.setChecked(prefs.getBoolean("daily_summary_enabled", true));
        updateSummaryTimeLabel();

        // App lock
        switchAppLock.setChecked(LockManager.isLockEnabled(requireContext()));
    }

    private void setupListeners(View root) {
        // ── Account — sign in / sign out ──────────────────────────────────
        root.findViewById(R.id.rowSignInOut).setOnClickListener(v -> {
            if (GoogleAuthManager.isGoogleSignedIn(requireContext())) {
                // Sign out
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Sign Out", (d, w) -> {
                        GoogleAuthManager.signOut(requireContext());
                        updateAccountUI();
                        Toast.makeText(requireContext(),
                            "Signed out", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                // Go to login
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });

        // ── Permissions ──────────────────────────────────────────────────
        root.findViewById(R.id.rowPermNotification).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        );
        root.findViewById(R.id.rowPermSms).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                smsPermLauncher.launch(Manifest.permission.RECEIVE_SMS);
            } else {
                openAppSettings();
            }
        });
        root.findViewById(R.id.rowPermPostNotif).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    openAppSettings();
                }
            } else {
                openAppSettings();
            }
        });

        // ── Appearance ───────────────────────────────────────────────────
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyTheme(checked);
            }
        });

        // ── Language ─────────────────────────────────────────────────────
        root.findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            String cur = prefs.getString("tts_language", "en");
            int selected = 0;
            for (int i = 0; i < LANG_CODES.length; i++) if (LANG_CODES[i].equals(cur)) { selected = i; break; }
            final int[] sel = {selected};
            new AlertDialog.Builder(requireContext())
                .setTitle("Announcement Language")
                .setSingleChoiceItems(LANGUAGES, selected, (d, which) -> sel[0] = which)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putString("tts_language", LANG_CODES[sel[0]]).apply();
                    tvLanguageValue.setText(LANGUAGES[sel[0]]);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // ── Speed ────────────────────────────────────────────────────────
        root.findViewById(R.id.rowSpeed).setOnClickListener(v -> {
            String cur = prefs.getString("tts_speed", "normal");
            int selected = 1;
            for (int i = 0; i < SPEED_KEYS.length; i++) if (SPEED_KEYS[i].equals(cur)) { selected = i; break; }
            final int[] sel = {selected};
            new AlertDialog.Builder(requireContext())
                .setTitle("Speech Speed")
                .setSingleChoiceItems(SPEEDS, selected, (d, which) -> sel[0] = which)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putString("tts_speed", SPEED_KEYS[sel[0]]).apply();
                    tvSpeedValue.setText(SPEEDS[sel[0]]);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // ── Repeat ───────────────────────────────────────────────────────
        switchRepeat.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean("tts_repeat", checked).apply()
        );

        // ── Min amount ───────────────────────────────────────────────────
        root.findViewById(R.id.rowMinAmount).setOnClickListener(v -> {
            float cur = prefs.getFloat("min_amount", 0f);
            int selected = 0;
            for (int i = 0; i < MIN_AMOUNTS.length; i++) if (MIN_AMOUNTS[i] == cur) { selected = i; break; }
            final int[] sel = {selected};
            new AlertDialog.Builder(requireContext())
                .setTitle("Minimum Amount")
                .setSingleChoiceItems(MIN_LABELS, selected, (d, which) -> sel[0] = which)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit().putFloat("min_amount", MIN_AMOUNTS[sel[0]]).apply();
                    tvMinAmountValue.setText(MIN_LABELS[sel[0]]);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // ── DND ──────────────────────────────────────────────────────────
        switchDnd.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("dnd_enabled", checked).apply();
        });

        // ── WhatsApp auto-send ────────────────────────────────────────────
        switchWhatsApp.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                // If no number set, prompt first
                String phone = prefs.getString(DailySummaryWorker.KEY_WA_PHONE, "").trim();
                if (phone.isEmpty()) {
                    switchWhatsApp.setChecked(false);
                    showWhatsAppNumberDialog();
                    return;
                }
            }
            prefs.edit().putBoolean(DailySummaryWorker.KEY_WA_ENABLED, checked).apply();
        });

        root.findViewById(R.id.rowWhatsAppNumber).setOnClickListener(v ->
            showWhatsAppNumberDialog()
        );

        // ── Daily summary ────────────────────────────────────────────────
        switchDailySummary.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean("daily_summary_enabled", checked).apply()
        );

        // ── Summary time picker ───────────────────────────────────────────
        root.findViewById(R.id.rowSummaryTime).setOnClickListener(v -> {
            int savedHour   = prefs.getInt(DailySummaryWorker.KEY_HOUR,   21);
            int savedMinute = prefs.getInt(DailySummaryWorker.KEY_MINUTE,  0);
            new android.app.TimePickerDialog(
                requireContext(),
                (picker, hour, minute) -> {
                    prefs.edit()
                        .putInt(DailySummaryWorker.KEY_HOUR,   hour)
                        .putInt(DailySummaryWorker.KEY_MINUTE, minute)
                        .apply();
                    updateSummaryTimeLabel();
                    DailySummaryWorker.schedule(requireContext());
                    Toast.makeText(requireContext(),
                        "Summary time set to " + fmt12hMin(hour, minute),
                        Toast.LENGTH_SHORT).show();
                },
                savedHour, savedMinute, false
            ).show();
        });

        // ── App lock ─────────────────────────────────────────────────────
        switchAppLock.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && !LockManager.isBiometricAvailable(requireContext())) {
                switchAppLock.setChecked(false);
                Toast.makeText(requireContext(),
                    "No biometric/PIN found. Set up screen lock first.", Toast.LENGTH_LONG).show();
                return;
            }
            LockManager.setLockEnabled(requireContext(), checked);
        });

        // ── About ────────────────────────────────────────────────────────
        root.findViewById(R.id.rowEmail).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:rohitprasadyadav06@gmail.com"));
            startActivity(Intent.createChooser(i, "Send Email"));
        });
        root.findViewById(R.id.rowInstagram).setOnClickListener(v ->
            startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://instagram.com/rohiit.md")))
        );

        // Version
        try {
            String ver = requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            ((TextView) root.findViewById(R.id.tvSettingsVersion)).setText("v" + ver);
        } catch (Exception ignored) {}
    }

    private void updatePermissionStatuses() {
        if (tvPermNotifStatus == null) return;

        // Notification listener
        boolean hasNotif = isNotificationListenerEnabled();
        setStatus(tvPermNotifStatus, hasNotif);

        // SMS
        boolean hasSms = ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        setStatus(tvPermSmsStatus, hasSms);

        // Post notifications
        boolean hasPost;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPost = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasPost = true; // Not required below Android 13
        }
        setStatus(tvPermPostNotifStatus, hasPost);
    }

    private void setStatus(TextView tv, boolean granted) {
        if (granted) {
            tv.setText("GRANTED");
            tv.setTextColor(0xFF3DDC84);
        } else {
            tv.setText("GRANT");
            tv.setTextColor(0xFFF5A623);
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(
            requireContext().getContentResolver(),
            "enabled_notification_listeners");
        return flat != null && flat.contains(requireContext().getPackageName());
    }

    private void updateDndTimeLabel() {
        int start = prefs.getInt("dnd_start", 22);
        int end   = prefs.getInt("dnd_end", 7);
        tvDndTime.setText(fmt12h(start) + " – " + fmt12h(end));
    }

    private String fmt12h(int hour) {
        String ampm = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12; if (h12 == 0) h12 = 12;
        return h12 + " " + ampm;
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(i);
    }

    private void showWhatsAppNumberDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("e.g. +919876543210");
        input.setText(prefs.getString(DailySummaryWorker.KEY_WA_PHONE, ""));
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        input.setSingleLine(true);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("WhatsApp Number")
            .setMessage("Enter the number to receive the daily summary.\nInclude country code e.g. +919876543210")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String phone = input.getText().toString().trim();
                if (phone.isEmpty()) {
                    prefs.edit()
                        .putString(DailySummaryWorker.KEY_WA_PHONE, "")
                        .putBoolean(DailySummaryWorker.KEY_WA_ENABLED, false)
                        .apply();
                    switchWhatsApp.setChecked(false);
                    tvWhatsAppNumber.setText("Not set");
                    return;
                }
                // Validate: must be digits + optional leading +, min 10 digits
                String digits = phone.replaceAll("[^\\d]", "");
                if (digits.length() < 10) {
                    Toast.makeText(requireContext(),
                        "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                prefs.edit()
                    .putString(DailySummaryWorker.KEY_WA_PHONE, phone)
                    .putBoolean(DailySummaryWorker.KEY_WA_ENABLED, true)
                    .apply();
                switchWhatsApp.setChecked(true);
                tvWhatsAppNumber.setText(phone);
                Toast.makeText(requireContext(),
                    "WhatsApp number saved", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", (d, w) -> {
                prefs.edit()
                    .putString(DailySummaryWorker.KEY_WA_PHONE, "")
                    .putBoolean(DailySummaryWorker.KEY_WA_ENABLED, false)
                    .apply();
                switchWhatsApp.setChecked(false);
                tvWhatsAppNumber.setText("Not set");
            })
            .show();
    }

    private void updateAccountUI() {
        if (tvUserName == null) return;
        boolean signedIn = GoogleAuthManager.isGoogleSignedIn(requireContext());
        if (signedIn) {
            String name  = GoogleAuthManager.getUserName(requireContext());
            String email = GoogleAuthManager.getUserEmail(requireContext());
            String photo = GoogleAuthManager.getUserPhoto(requireContext());

            tvUserName.setText(name.isEmpty() ? "Google User" : name);
            tvUserEmail.setText(email);
            tvUserAvatar.setText(name.isEmpty() ? "G" :
                String.valueOf(name.charAt(0)).toUpperCase());
            tvAuthStatus.setText("SIGNED IN");
            tvAuthStatus.setTextColor(0xFF3DDC84);
            tvAuthStatus.setBackgroundColor(0x1A3DDC84);
            tvSignInOutLabel.setText("Sign out");

            // Load profile photo with Glide if available
            if (!photo.isEmpty() && ivUserPhoto != null) {
                ivUserPhoto.setVisibility(android.view.View.VISIBLE);
                tvUserAvatar.setVisibility(android.view.View.INVISIBLE);
                com.bumptech.glide.Glide.with(requireContext())
                    .load(photo)
                    .apply(new com.bumptech.glide.request.RequestOptions()
                        .circleCrop()
                        .override(120, 120)
                        .placeholder(R.drawable.circle_green_bg)
                        .error(R.drawable.circle_green_bg))
                    .listener(new com.bumptech.glide.request.RequestListener
                            <android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                com.bumptech.glide.load.engine.GlideException e,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            // Show initial letter if photo fails
                            ivUserPhoto.setVisibility(android.view.View.GONE);
                            tvUserAvatar.setVisibility(android.view.View.VISIBLE);
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(
                                android.graphics.drawable.Drawable resource,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource,
                                boolean isFirstResource) {
                            tvUserAvatar.setVisibility(android.view.View.GONE);
                            return false;
                        }
                    })
                    .into(ivUserPhoto);
            } else {
                if (ivUserPhoto != null) {
                    ivUserPhoto.setVisibility(android.view.View.GONE);
                    tvUserAvatar.setVisibility(android.view.View.VISIBLE);
                }
            }
        } else {
            tvUserName.setText("Not signed in");
            tvUserEmail.setText("Sign in for account protection");
            tvUserAvatar.setText("?");
            tvUserAvatar.setVisibility(android.view.View.VISIBLE);
            tvAuthStatus.setText("GUEST");
            tvAuthStatus.setTextColor(0xFFF5A623);
            tvAuthStatus.setBackgroundColor(0x1AF5A623);
            tvSignInOutLabel.setText("Sign in with Google");
            if (ivUserPhoto != null)
                ivUserPhoto.setVisibility(android.view.View.GONE);
        }
    }

    private void updateSummaryTimeLabel() {
        if (tvSummaryTime == null) return;
        int hour   = prefs.getInt(DailySummaryWorker.KEY_HOUR,   21);
        int minute = prefs.getInt(DailySummaryWorker.KEY_MINUTE,  0);
        tvSummaryTime.setText(fmt12hMin(hour, minute));
    }

    private String fmt12hMin(int hour, int minute) {
        String ampm = hour < 12 ? "AM" : "PM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        return String.format(java.util.Locale.getDefault(), "%d:%02d %s", h12, minute, ampm);
    }
}
