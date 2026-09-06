// FILE: app/src/main/java/com/announceupi/in/MainActivity.java
package com.announceupi.in;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    static final String PREF_FILE        = "upi_prefs";
    static final String KEY_TRANSACTIONS = "transactions";

    private BottomNavigationView bottomNav;
    private HomeFragment         homeFragment;
    private DashboardFragment    dashboardFragment;
    private SettingsFragment     settingsFragment;
    private Fragment             activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences p = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(
            p.getBoolean("dark_mode", true)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            homeFragment      = new HomeFragment();
            dashboardFragment = new DashboardFragment();
            settingsFragment  = new SettingsFragment();

            fm.beginTransaction()
                .add(R.id.fragmentContainer, dashboardFragment, "dashboard").hide(dashboardFragment)
                .add(R.id.fragmentContainer, settingsFragment,  "settings" ).hide(settingsFragment)
                .add(R.id.fragmentContainer, homeFragment,      "home"     )
                .commit();

            activeFragment = homeFragment;
        } else {
            homeFragment      = (HomeFragment)      fm.findFragmentByTag("home");
            dashboardFragment = (DashboardFragment) fm.findFragmentByTag("dashboard");
            settingsFragment  = (SettingsFragment)  fm.findFragmentByTag("settings");

            int selected = bottomNav.getSelectedItemId();
            if      (selected == R.id.nav_dashboard) activeFragment = dashboardFragment;
            else if (selected == R.id.nav_settings)  activeFragment = settingsFragment;
            else                                     activeFragment = homeFragment;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target;
            int id = item.getItemId();
            if      (id == R.id.nav_home)      target = homeFragment;
            else if (id == R.id.nav_dashboard) target = dashboardFragment;
            else                               target = settingsFragment;

            if (target == activeFragment) return true;

            fm.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
            activeFragment = target;
            return true;
        });

        DailySummaryWorker.schedule(this);
        AnnounceUPIWidget.scheduleAutoRefresh(this);
        AnnounceUPIWidget.scheduleDailyWorker(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LockManager.isLockEnabled(this) && !LockManager.isSessionValid()) {
            LockManager.authenticate(this, new LockManager.AuthCallback() {
                @Override public void onSuccess() {
                    if (homeFragment != null) homeFragment.loadTransactions();
                }
                @Override public void onFailed(String reason) {
                    finishAffinity();
                }
            });
        }
    }

    public void applyTheme(boolean darkMode) {
        getSharedPreferences(PREF_FILE, MODE_PRIVATE)
            .edit().putBoolean("dark_mode", darkMode).apply();
        AppCompatDelegate.setDefaultNightMode(
            darkMode ? AppCompatDelegate.MODE_NIGHT_YES
                     : AppCompatDelegate.MODE_NIGHT_NO);
        recreate();
    }

    public static void saveTransaction(android.content.Context ctx,
                                       double amount, String source) {
        SharedPreferences p = ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(p.getString(KEY_TRANSACTIONS, "[]"));
            JSONObject txn = new JSONObject();
            txn.put("amount", "\u20B9" + (amount % 1 == 0
                ? String.format(java.util.Locale.getDefault(), "%.0f", amount)
                : String.format(java.util.Locale.getDefault(), "%.2f", amount)));
            txn.put("raw_amount", amount);
            txn.put("source", source);
            java.text.SimpleDateFormat tf =
                new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
            java.text.SimpleDateFormat df =
                new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
            txn.put("time", tf.format(new java.util.Date()));
            txn.put("date", df.format(new java.util.Date()));

            JSONArray updated = new JSONArray();
            updated.put(txn);
            for (int i = 0; i < Math.min(arr.length(), 499); i++) updated.put(arr.get(i));
            p.edit().putString(KEY_TRANSACTIONS, updated.toString()).apply();

            AnnounceUPIWidget.updateAllWidgets(ctx);
        } catch (Exception ignored) {}
    }
}
