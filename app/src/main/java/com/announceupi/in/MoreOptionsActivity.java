// FILE: app/src/main/java/com/announceupi/in/MoreOptionsActivity.java

package com.announceupi.in;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.card.MaterialCardView;

public class MoreOptionsActivity extends AppCompatActivity {

    private static final String PREF_FILE = "upi_prefs";

    // Keys (read by PaymentNotificationService too)
    static final String KEY_LANG            = "tts_language";
    static final String KEY_SPEED           = "tts_speed";
    static final String KEY_REPEAT          = "tts_repeat";
    static final String KEY_COPY_AMOUNT     = "copy_on_tap";
    static final String KEY_SHARE           = "share_enabled";
    static final String KEY_NOTE            = "note_enabled";
    static final String KEY_MIN_AMOUNT      = "min_amount";
    static final String KEY_DND_ENABLED     = "dnd_enabled";
    static final String KEY_DND_START       = "dnd_start";   // hour 0-23
    static final String KEY_DND_END         = "dnd_end";
    static final String KEY_DAILY_SUMMARY   = "daily_summary_enabled";

    private static final String[] LANG_LABELS = {
        "🇬🇧  English", "🇮🇳  हिंदी (Hindi)", "🇮🇳  தமிழ் (Tamil)",
        "🇮🇳  తెలుగు (Telugu)", "🇮🇳  മലയാളം (Malayalam)",
        "🇮🇳  ગુજરાતી (Gujarati)", "🇮🇳  ਪੰਜਾਬੀ (Punjabi)", "🇮🇳  বাংলা (Bengali)"
    };
    private static final String[] LANG_CODES    = {"en","hi","ta","te","ml","gu","pa","bn"};
    private static final String[] LANG_DISPLAY  = {
        "English","Hindi","Tamil","Telugu","Malayalam","Gujarati","Punjabi","Bengali"
    };

    // Views
    private SharedPreferences prefs;
    private TextView  tvCurrentLang, tvSpeedLabel, tvMinAmount, tvDndTime;
    private TextView  btnSlow, btnNormal, btnFast;
    private SwitchCompat switchRepeat, switchCopyAmount, switchShare,
                          switchNote, switchDailySummary, switchDnd, switchLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_options);
        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        bindViews();
        loadValues();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Tell LockManager this activity is active so the grace window
        // stays open — prevents MainActivity from clearing the session
    }

    private void bindViews() {
        AppCompatImageButton btnBack = findViewById(R.id.btnMoreBack);
        btnBack.setOnClickListener(v -> finish());

        tvCurrentLang   = findViewById(R.id.tvCurrentLang);
        tvSpeedLabel    = findViewById(R.id.tvSpeedLabel);
        btnSlow         = findViewById(R.id.btnSpeedSlow);
        btnNormal       = findViewById(R.id.btnSpeedNormal);
        btnFast         = findViewById(R.id.btnSpeedFast);
        switchRepeat    = findViewById(R.id.switchRepeat);
        switchCopyAmount= findViewById(R.id.switchCopyAmount);
        switchShare     = findViewById(R.id.switchShare);
        switchNote      = findViewById(R.id.switchNote);
        switchDailySummary = findViewById(R.id.switchDailySummary);
        switchDnd       = findViewById(R.id.switchDnd);
        switchLock      = findViewById(R.id.switchLock);
        tvMinAmount     = findViewById(R.id.tvMinAmount);
        tvDndTime       = findViewById(R.id.tvDndTime);
    }

    private void loadValues() {
        tvCurrentLang.setText(displayForCode(prefs.getString(KEY_LANG, "en")));
        setSpeedChip(prefs.getString(KEY_SPEED, "normal"));
        switchRepeat.setChecked(prefs.getBoolean(KEY_REPEAT, false));
        switchCopyAmount.setChecked(prefs.getBoolean(KEY_COPY_AMOUNT, true));
        switchShare.setChecked(prefs.getBoolean(KEY_SHARE, true));
        switchNote.setChecked(prefs.getBoolean(KEY_NOTE, true));
        switchDailySummary.setChecked(prefs.getBoolean(KEY_DAILY_SUMMARY, true));
        switchDnd.setChecked(prefs.getBoolean(KEY_DND_ENABLED, false));
        switchLock.setChecked(LockManager.isLockEnabled(this));

        // Min amount label
        float min = prefs.getFloat(KEY_MIN_AMOUNT, 0f);
        tvMinAmount.setText(min == 0 ? "All amounts" : "Above \u20B9" + (int) min);

        // DND time label
        updateDndLabel();

        // Shortcut setup
        setupShortcutCard();
    }

    private void setupListeners() {
        // Language
        ((MaterialCardView) findViewById(R.id.cardLanguage))
            .setOnClickListener(v -> showLanguagePicker());

        // Speed chips
        btnSlow.setOnClickListener(v   -> saveSpeed("slow"));
        btnNormal.setOnClickListener(v -> saveSpeed("normal"));
        btnFast.setOnClickListener(v   -> saveSpeed("fast"));

        // Toggles
        switchRepeat.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean(KEY_REPEAT, on).apply());
        switchCopyAmount.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean(KEY_COPY_AMOUNT, on).apply());
        switchShare.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean(KEY_SHARE, on).apply());
        switchNote.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean(KEY_NOTE, on).apply());
        switchDailySummary.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_DAILY_SUMMARY, on).apply();
            if (on) DailySummaryWorker.schedule(this);
        });
        switchDnd.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_DND_ENABLED, on).apply();
            if (on && prefs.getInt(KEY_DND_START, -1) == -1) showDndPicker();
        });

        // Lock
        switchLock.setOnCheckedChangeListener((b, on) -> {
            if (on && !LockManager.isBiometricAvailable(this)) {
                Toast.makeText(this, "No biometric or screen lock set up on this device", Toast.LENGTH_LONG).show();
                switchLock.setChecked(false);
                return;
            }
            if (on) {
                LockManager.authenticate(this, new LockManager.AuthCallback() {
                    @Override public void onSuccess() {
                        LockManager.setLockEnabled(MoreOptionsActivity.this, true);
                        Toast.makeText(MoreOptionsActivity.this, "App lock enabled \uD83D\uDD12", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailed(String reason) {
                        switchLock.setChecked(false);
                        Toast.makeText(MoreOptionsActivity.this, "Auth failed: " + reason, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                LockManager.setLockEnabled(this, false);
                Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Min amount
        ((MaterialCardView) findViewById(R.id.cardMinAmount))
            .setOnClickListener(v -> showMinAmountPicker());

        // DND time picker
        ((MaterialCardView) findViewById(R.id.cardDnd))
            .setOnLongClickListener(v -> { showDndPicker(); return true; });

        // Monthly chart
        ((MaterialCardView) findViewById(R.id.cardMonthly))
            .setOnClickListener(v -> startActivity(new Intent(this, MonthlyChartActivity.class)));

        // Export CSV
        ((MaterialCardView) findViewById(R.id.cardExport))
            .setOnClickListener(v -> ExportManager.exportCsv(this));

        // Shortcut card
        ((MaterialCardView) findViewById(R.id.cardShortcut))
            .setOnClickListener(v -> Toast.makeText(this,
                "Long-press the AnnounceUPI icon on your home screen", Toast.LENGTH_LONG).show());
    }

    // ── Language picker ───────────────────────────────────────────────────
    private void showLanguagePicker() {
        int cur = indexOfCode(prefs.getString(KEY_LANG, "en"));
        final int[] chosen = {cur};
        new AlertDialog.Builder(this)
            .setTitle("Announcement Language")
            .setSingleChoiceItems(LANG_LABELS, cur, (d, w) -> chosen[0] = w)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit().putString(KEY_LANG, LANG_CODES[chosen[0]]).apply();
                tvCurrentLang.setText(LANG_DISPLAY[chosen[0]]);
                Toast.makeText(this, LANG_DISPLAY[chosen[0]] + " selected", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── Speed chips ───────────────────────────────────────────────────────
    private void saveSpeed(String speed) {
        prefs.edit().putString(KEY_SPEED, speed).apply();
        setSpeedChip(speed);
    }

    private void setSpeedChip(String speed) {
        btnSlow.setBackgroundResource(R.drawable.chip_unselected);
        btnNormal.setBackgroundResource(R.drawable.chip_unselected);
        btnFast.setBackgroundResource(R.drawable.chip_unselected);
        int grey  = Color.parseColor("#888888");
        int black = Color.parseColor("#0A0A0A");
        btnSlow.setTextColor(grey); btnNormal.setTextColor(grey); btnFast.setTextColor(grey);
        switch (speed) {
            case "slow":
                btnSlow.setBackgroundResource(R.drawable.chip_selected);
                btnSlow.setTextColor(black); tvSpeedLabel.setText("Slow"); break;
            case "fast":
                btnFast.setBackgroundResource(R.drawable.chip_selected);
                btnFast.setTextColor(black); tvSpeedLabel.setText("Fast"); break;
            default:
                btnNormal.setBackgroundResource(R.drawable.chip_selected);
                btnNormal.setTextColor(black); tvSpeedLabel.setText("Normal"); break;
        }
    }

    // ── Minimum amount picker ─────────────────────────────────────────────
    private void showMinAmountPicker() {
        String[] options = {"All amounts (no filter)", "Above ₹1", "Above ₹10",
                            "Above ₹50", "Above ₹100", "Above ₹500", "Above ₹1,000"};
        float[]  values  = {0, 1, 10, 50, 100, 500, 1000};
        float current = prefs.getFloat(KEY_MIN_AMOUNT, 0f);
        int cur = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == current) { cur = i; break; }
        final int[] chosen = {cur};
        new AlertDialog.Builder(this)
            .setTitle("Minimum Announcement Amount")
            .setSingleChoiceItems(options, cur, (d, w) -> chosen[0] = w)
            .setPositiveButton("Save", (d, w) -> {
                prefs.edit().putFloat(KEY_MIN_AMOUNT, values[chosen[0]]).apply();
                tvMinAmount.setText(chosen[0] == 0 ? "All amounts" : options[chosen[0]]);
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ── DND time picker ───────────────────────────────────────────────────
    private void showDndPicker() {
        String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            int h = i % 12 == 0 ? 12 : i % 12;
            hours[i] = h + ":00 " + (i < 12 ? "AM" : "PM");
        }
        int defStart = prefs.getInt(KEY_DND_START, 22);
        int defEnd   = prefs.getInt(KEY_DND_END,    7);
        final int[] s = {defStart}, e = {defEnd};

        new AlertDialog.Builder(this)
            .setTitle("Do Not Disturb — Start Hour")
            .setSingleChoiceItems(hours, defStart, (d, w) -> s[0] = w)
            .setPositiveButton("Next", (d, w) ->
                new AlertDialog.Builder(this)
                    .setTitle("Do Not Disturb — End Hour")
                    .setSingleChoiceItems(hours, defEnd, (d2, w2) -> e[0] = w2)
                    .setPositiveButton("Save", (d2, w2) -> {
                        prefs.edit().putInt(KEY_DND_START, s[0]).putInt(KEY_DND_END, e[0]).apply();
                        updateDndLabel();
                        Toast.makeText(this, "DND set: " + hours[s[0]] + " – " + hours[e[0]], Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null).show()
            )
            .setNegativeButton("Cancel", null).show();
    }

    private void updateDndLabel() {
        int start = prefs.getInt(KEY_DND_START, 22);
        int end   = prefs.getInt(KEY_DND_END,    7);
        String fmt = "%d:00 %s";
        String s   = String.format(java.util.Locale.US, fmt, start % 12 == 0 ? 12 : start % 12, start < 12 ? "AM" : "PM");
        String en  = String.format(java.util.Locale.US, fmt, end   % 12 == 0 ? 12 : end   % 12, end   < 12 ? "AM" : "PM");
        tvDndTime.setText(s + " – " + en);
    }

    // ── Shortcut card ─────────────────────────────────────────────────────
    private void setupShortcutCard() {
        TextView badge = findViewById(R.id.tvShortcutBadge);
        TextView desc  = findViewById(R.id.tvShortcutDesc);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            badge.setText("Active"); badge.setTextColor(Color.parseColor("#3DDC84"));
            desc.setText("Long-press icon → \"Today's Total\" or \"Options\"");
            createShortcuts();
        } else {
            badge.setText("API 25+"); badge.setTextColor(Color.parseColor("#888888"));
            desc.setText("Requires Android 7.1 or higher");
        }
    }

    private void createShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) return;
        android.content.pm.ShortcutManager sm = getSystemService(android.content.pm.ShortcutManager.class);
        if (sm == null || sm.isRateLimitingActive()) return;
        try {
            Intent todayIntent = new Intent(this, MainActivity.class);
            todayIntent.setAction(Intent.ACTION_VIEW);
            todayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            android.content.pm.ShortcutInfo today = new android.content.pm.ShortcutInfo.Builder(this, "today_total")
                .setShortLabel("Today's Total").setLongLabel("View today's payment total")
                .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(todayIntent).build();
            Intent optIntent = new Intent(this, MoreOptionsActivity.class);
            optIntent.setAction(Intent.ACTION_VIEW);
            optIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            android.content.pm.ShortcutInfo opts = new android.content.pm.ShortcutInfo.Builder(this, "open_options")
                .setShortLabel("Options").setLongLabel("Open app settings")
                .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(optIntent).build();
            sm.setDynamicShortcuts(java.util.Arrays.asList(today, opts));
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String displayForCode(String code) {
        for (int i = 0; i < LANG_CODES.length; i++)
            if (LANG_CODES[i].equals(code)) return LANG_DISPLAY[i];
        return "English";
    }
    private int indexOfCode(String code) {
        for (int i = 0; i < LANG_CODES.length; i++)
            if (LANG_CODES[i].equals(code)) return i;
        return 0;
    }

}
