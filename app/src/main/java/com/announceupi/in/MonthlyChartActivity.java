// FILE: app/src/main/java/com/announceupi/in/MonthlyChartActivity.java

package com.announceupi.in;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class MonthlyChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_chart);

        AppCompatImageButton btnBack = findViewById(R.id.btnChartBack);
        btnBack.setOnClickListener(v -> finish());

        loadChart();
    }

    private void loadChart() {
        SharedPreferences prefs = getSharedPreferences("upi_prefs", MODE_PRIVATE);
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());

        TextView tvTitle = findViewById(R.id.tvChartMonth);
        tvTitle.setText(monthLabel);

        // Build day → total map for current month
        TreeMap<Integer, Double> dayTotals = new TreeMap<>();
        Calendar now = Calendar.getInstance();
        int curMonth = now.get(Calendar.MONTH);
        int curYear  = now.get(Calendar.YEAR);
        int daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= daysInMonth; d++) dayTotals.put(d, 0.0);

        double monthTotal = 0;
        int txnCount = 0;

        try {
            JSONArray arr = new JSONArray(prefs.getString("transactions", "[]"));
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject txn = arr.getJSONObject(i);
                String dateStr = txn.optString("date", "");
                try {
                    Date d = sdf.parse(dateStr);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    if (cal.get(Calendar.MONTH) == curMonth &&
                        cal.get(Calendar.YEAR)  == curYear) {
                        int day = cal.get(Calendar.DAY_OF_MONTH);
                        double amt = txn.optDouble("raw_amount", 0);
                        dayTotals.put(day, dayTotals.getOrDefault(day, 0.0) + amt);
                        monthTotal += amt;
                        txnCount++;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // Summary
        TextView tvTotal = findViewById(R.id.tvChartTotal);
        TextView tvCount = findViewById(R.id.tvChartCount);
        tvTotal.setText("\u20B9" + String.format(Locale.getDefault(), "%,.2f", monthTotal));
        tvCount.setText(txnCount + " transactions this month");

        // Build bar entries
        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int index = 0;
        for (int day : dayTotals.keySet()) {
            entries.add(new BarEntry(index, dayTotals.get(day).floatValue()));
            labels.add(String.valueOf(day));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Total (₹)");
        dataSet.setColor(Color.parseColor("#3DDC84"));
        dataSet.setValueTextColor(Color.parseColor("#888888"));
        dataSet.setValueTextSize(8f);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        BarChart chart = findViewById(R.id.barChart);
        chart.setData(barData);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.setExtraBottomOffset(8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#888888"));
        xAxis.setTextSize(9f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#888888"));
        leftAxis.setTextSize(9f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#1AFFFFFF"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                if (value >= 1000) return "\u20B9" + (int)(value/1000) + "k";
                return "\u20B9" + (int) value;
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.animateY(800);
        chart.invalidate();
    }
}
