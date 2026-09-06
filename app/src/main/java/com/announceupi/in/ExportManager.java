// FILE: app/src/main/java/com/announceupi/in/ExportManager.java

package com.announceupi.in;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Exports transaction history as CSV to Downloads folder,
 * then opens the share sheet so user can send to WhatsApp, Gmail, Drive etc.
 */
public class ExportManager {

    public static void exportCsv(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("upi_prefs", Context.MODE_PRIVATE);

        try {
            JSONArray arr = new JSONArray(prefs.getString("transactions", "[]"));
            if (arr.length() == 0) {
                Toast.makeText(ctx, "No transactions to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // Build CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Time,Amount,Raw Amount,Source,Note\n");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject txn = arr.getJSONObject(i);
                csv.append(escape(txn.optString("date",       "")))
                   .append(",")
                   .append(escape(txn.optString("time",       "")))
                   .append(",")
                   .append(escape(txn.optString("amount",     "")))
                   .append(",")
                   .append(txn.optDouble("raw_amount", 0))
                   .append(",")
                   .append(escape(txn.optString("source",     "")))
                   .append(",")
                   .append(escape(txn.optString("note",       "")))
                   .append("\n");
            }

            // Write to app cache dir (FileProvider accessible)
            String ts   = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            String name = "AnnounceUPI_" + ts + ".csv";
            File   file = new File(ctx.getCacheDir(), name);
            FileWriter fw = new FileWriter(file);
            fw.write(csv.toString());
            fw.close();

            // Share via intent
            Uri uri = FileProvider.getUriForFile(ctx,
                ctx.getPackageName() + ".provider", file);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "AnnounceUPI Transaction History");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(share, "Export via"));

        } catch (Exception e) {
            Toast.makeText(ctx, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Wraps a value in quotes and escapes internal quotes for CSV safety */
    private static String escape(String val) {
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
