// FILE: app/src/main/java/com/announceupi/in/HomeFragment.java
package com.announceupi.in;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView         rvTransactions;
    private TransactionAdapter   adapter;
    private TextView             tvTotalAmount, tvTransactionCount;
    private TextView             tvAllTimeTotal, tvEmpty;
    private EditText             etSearch;
    private AppCompatImageButton btnClearSearch, btnClearAll;
    private SwitchCompat         switchTheme;

    private final List<JSONObject> allTransactions = new ArrayList<>();
    private final List<JSONObject> filteredList    = new ArrayList<>();
    private SharedPreferences      prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        prefs = requireContext().getSharedPreferences(MainActivity.PREF_FILE, 0);

        bindViews(root);
        setupThemeToggle();
        setupRecyclerView();
        setupSearch();
        setupClearAll();
        setVersion(root);
        loadTransactions();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void bindViews(View root) {
        rvTransactions     = root.findViewById(R.id.rvTransactions);
        tvTotalAmount      = root.findViewById(R.id.tvTotalAmount);
        tvTransactionCount = root.findViewById(R.id.tvTransactionCount);
        tvAllTimeTotal     = root.findViewById(R.id.tvAllTimeTotal);
        tvEmpty            = root.findViewById(R.id.tvEmpty);
        etSearch           = root.findViewById(R.id.etSearch);
        btnClearSearch     = root.findViewById(R.id.btnClearSearch);
        btnClearAll        = root.findViewById(R.id.btnClearAll);
        switchTheme        = root.findViewById(R.id.switchTheme);
    }

    private void setupThemeToggle() {
        boolean isDark = prefs.getBoolean("dark_mode", true);
        switchTheme.setChecked(isDark);
        switchTheme.setText(isDark ? "Dark" : "Light");
        switchTheme.setOnCheckedChangeListener((btn, checked) -> {
            switchTheme.setText(checked ? "Dark" : "Light");
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).applyTheme(checked);
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(filteredList);
        adapter.setOnDeleteListener(() -> {
            allTransactions.clear();
            allTransactions.addAll(filteredList);
            updateSummary();
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setNestedScrollingEnabled(false);
        rvTransactions.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int i,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int i,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                filterTransactions(q);
            }
        });
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
    }

    private void filterTransactions(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allTransactions);
        } else {
            String lower = query.toLowerCase();
            for (JSONObject t : allTransactions) {
                if (t.optString("amount","").toLowerCase().contains(lower)
                 || t.optString("source","").toLowerCase().contains(lower)
                 || t.optString("date",  "").toLowerCase().contains(lower)
                 || t.optString("time",  "").toLowerCase().contains(lower))
                    filteredList.add(t);
            }
        }
        adapter.notifyDataSetChanged();
        boolean empty = filteredList.isEmpty();
        rvTransactions.setVisibility(empty ? View.GONE    : View.VISIBLE);
        tvEmpty       .setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && !query.isEmpty())
            tvEmpty.setText("No results for \"" + query + "\"");
        else if (empty)
            tvEmpty.setText("No transactions yet.\nWaiting for a UPI payment\u2026");
    }

    private void setupClearAll() {
        btnClearAll.setOnClickListener(v ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear History")
                .setMessage("Delete all transaction history?")
                .setPositiveButton("Clear", (d, w) -> {
                    prefs.edit().remove(MainActivity.KEY_TRANSACTIONS).apply();
                    allTransactions.clear();
                    filteredList.clear();
                    adapter.notifyDataSetChanged();
                    updateSummary();
                    filterTransactions("");
                    Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null).show()
        );
    }

    public void loadTransactions() {
        allTransactions.clear();
        try {
            JSONArray arr = new JSONArray(
                prefs.getString(MainActivity.KEY_TRANSACTIONS, "[]"));
            for (int i = 0; i < arr.length(); i++)
                allTransactions.add(arr.getJSONObject(i));
        } catch (Exception ignored) {}
        String q = etSearch != null ? etSearch.getText().toString().trim() : "";
        filterTransactions(q);
        updateSummary();
    }

    private void updateSummary() {
        if (tvTotalAmount == null) return;
        String today = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        double todayTotal = 0, allTotal = 0;
        int todayCount = 0;
        for (JSONObject t : allTransactions) {
            double amt = t.optDouble("raw_amount", 0);
            allTotal  += amt;
            if (today.equalsIgnoreCase(t.optString("date", ""))) {
                todayTotal += amt; todayCount++;
            }
        }
        tvTotalAmount     .setText("\u20B9" + String.format(Locale.getDefault(), "%,.2f", todayTotal));
        tvTransactionCount.setText(todayCount + " transaction" + (todayCount == 1 ? "" : "s") + " today");
        tvAllTimeTotal    .setText("All time: \u20B9" + String.format(Locale.getDefault(), "%,.0f", allTotal));
    }

    private void setVersion(View root) {
        TextView tv = root.findViewById(R.id.tvAppVersion);
        if (tv == null) return;
        try {
            String v = requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            tv.setText("AnnounceUPI v" + v);
        } catch (Exception e) { tv.setText("AnnounceUPI"); }
    }
}
