// FILE: app/src/main/java/com/announceupi/in/DashboardFragment.java
package com.announceupi.in;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvDashToday, tvDashTodayCount, tvDashMonth, tvDashMonthCount;
    private TextView tvDashAllTime, tvDashAllTimeCount, tvDashAvg, tvExportCount;
    private BarChart  barChartMonthly;
    private LineChart lineChartWeekly;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        prefs = requireContext().getSharedPreferences(MainActivity.PREF_FILE, 0);

        tvDashToday       = root.findViewById(R.id.tvDashToday);
        tvDashTodayCount  = root.findViewById(R.id.tvDashTodayCount);
        tvDashMonth       = root.findViewById(R.id.tvDashMonth);
        tvDashMonthCount  = root.findViewById(R.id.tvDashMonthCount);
        tvDashAllTime     = root.findViewById(R.id.tvDashAllTime);
        tvDashAllTimeCount= root.findViewById(R.id.tvDashAllTimeCount);
        tvDashAvg         = root.findViewById(R.id.tvDashAvg);
        tvExportCount     = root.findViewById(R.id.tvExportCount);
        barChartMonthly   = root.findViewById(R.id.barChartMonthly);
        lineChartWeekly   = root.findViewById(R.id.lineChartWeekly);

        root.findViewById(R.id.btnExportCsv).setOnClickListener(v ->
            ExportManager.exportCsv(requireContext())
        );

        loadData();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        JSONArray arr;
        try {
            arr = new JSONArray(prefs.getString(MainActivity.KEY_TRANSACTIONS, "[]"));
        } catch (Exception e) { arr = new JSONArray(); }

        String today    = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        String thisMonth= new SimpleDateFormat("MMM yyyy",    Locale.getDefault()).format(new Date());

        double todayAmt = 0, monthAmt = 0, allAmt = 0;
        int    todayCnt = 0, monthCnt = 0, allCnt = arr.length();

        // daily totals for this month (day 1..31)
        float[] dayTotals  = new float[32];
        // last 7 days totals
        float[] weekTotals = new float[7];
        String[] weekLabels= new String[7];

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            weekLabels[6 - i] = new SimpleDateFormat("EEE", Locale.getDefault()).format(cal.getTime());
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.optJSONObject(i);
            if (t == null) continue;
            double amt  = t.optDouble("raw_amount", 0);
            String date = t.optString("date", "");
            allAmt += amt;

            if (today.equalsIgnoreCase(date))     { todayAmt += amt; todayCnt++; }
            if (date.contains(new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date()))) {
                monthAmt += amt; monthCnt++;
                // day-of-month
                try {
                    Date d = sdf.parse(date);
                    Calendar dc = Calendar.getInstance();
                    if (d != null) { dc.setTime(d); dayTotals[dc.get(Calendar.DAY_OF_MONTH)] += (float) amt; }
                } catch (Exception ignored) {}
            }
            // last 7 days
            for (int w = 6; w >= 0; w--) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -(6 - w));
                if (sdf.format(cal.getTime()).equalsIgnoreCase(date)) {
                    weekTotals[w] += (float) amt;
                }
            }
        }

        // Update stat cards
        tvDashToday.setText("\u20B9" + fmtAmt(todayAmt));
        tvDashTodayCount.setText(todayCnt + " payments");
        tvDashMonth.setText("\u20B9" + fmtAmt(monthAmt));
        tvDashMonthCount.setText(monthCnt + " payments");
        tvDashAllTime.setText("\u20B9" + fmtAmt(allAmt));
        tvDashAllTimeCount.setText(allCnt + " total transactions");
        double avg = allCnt > 0 ? allAmt / allCnt : 0;
        tvDashAvg.setText("Avg\n\u20B9" + fmtAmt(avg));
        tvExportCount.setText(allCnt + " records");

        setupBarChart(dayTotals);
        setupLineChart(weekTotals, weekLabels);
    }

    private void setupBarChart(float[] dayTotals) {
        List<BarEntry> entries = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= daysInMonth; d++) {
            if (dayTotals[d] > 0) entries.add(new BarEntry(d, dayTotals[d]));
        }
        if (entries.isEmpty()) entries.add(new BarEntry(1, 0));

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(0xFF3DDC84);
        ds.setDrawValues(false);

        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);

        barChartMonthly.setData(data);
        barChartMonthly.getDescription().setEnabled(false);
        barChartMonthly.getLegend().setEnabled(false);
        barChartMonthly.setDrawGridBackground(false);
        barChartMonthly.setDrawBorders(false);
        barChartMonthly.setPinchZoom(false);
        barChartMonthly.setDoubleTapToZoomEnabled(false);

        XAxis x = barChartMonthly.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(0xFF888888);
        x.setTextSize(10f);
        x.setDrawGridLines(false);
        x.setGranularity(1f);

        YAxis left = barChartMonthly.getAxisLeft();
        left.setTextColor(0xFF888888);
        left.setTextSize(10f);
        left.setDrawGridLines(true);
        left.setGridColor(0x22888888);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) {
                return v >= 1000 ? "\u20B9" + (int)(v/1000) + "k" : "\u20B9" + (int) v;
            }
        });
        barChartMonthly.getAxisRight().setEnabled(false);
        barChartMonthly.animateY(600);
        barChartMonthly.invalidate();
    }

    private void setupLineChart(float[] weekTotals, String[] labels) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) entries.add(new Entry(i, weekTotals[i]));

        LineDataSet ds = new LineDataSet(entries, "");
        ds.setColor(0xFF5B9CF6);
        ds.setCircleColor(0xFF5B9CF6);
        ds.setLineWidth(2f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(0xFF5B9CF6);
        ds.setFillAlpha(30);

        lineChartWeekly.setData(new LineData(ds));
        lineChartWeekly.getDescription().setEnabled(false);
        lineChartWeekly.getLegend().setEnabled(false);
        lineChartWeekly.setDrawGridBackground(false);
        lineChartWeekly.setDrawBorders(false);
        lineChartWeekly.setPinchZoom(false);
        lineChartWeekly.setDoubleTapToZoomEnabled(false);

        XAxis x = lineChartWeekly.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(0xFF888888);
        x.setTextSize(10f);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) {
                int idx = (int) v;
                return (idx >= 0 && idx < 7) ? labels[idx] : "";
            }
        });

        YAxis left = lineChartWeekly.getAxisLeft();
        left.setTextColor(0xFF888888);
        left.setTextSize(10f);
        left.setDrawGridLines(true);
        left.setGridColor(0x22888888);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) {
                return v >= 1000 ? "\u20B9" + (int)(v/1000) + "k" : "\u20B9" + (int) v;
            }
        });
        lineChartWeekly.getAxisRight().setEnabled(false);
        lineChartWeekly.animateX(600);
        lineChartWeekly.invalidate();
    }

    private String fmtAmt(double v) {
        if (v >= 100000) return String.format(Locale.getDefault(), "%.1fL", v / 100000);
        if (v >= 1000)   return String.format(Locale.getDefault(), "%.1fk", v / 1000);
        return String.format(Locale.getDefault(), "%.0f", v);
    }
}
