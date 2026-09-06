// FILE: app/src/main/java/com/announceupi/in/TransactionAdapter.java
package com.announceupi.in;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TxnViewHolder> {

    private static final String PREF_FILE = "upi_prefs";

    private final List<JSONObject> transactions;

    public interface OnDeleteListener { void onDeleted(); }
    private OnDeleteListener deleteListener;
    public void setOnDeleteListener(OnDeleteListener l) { this.deleteListener = l; }

    public TransactionAdapter(List<JSONObject> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TxnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_transaction, parent, false);
        return new TxnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TxnViewHolder holder, int position) {
        JSONObject txn = transactions.get(position);
        Context ctx    = holder.itemView.getContext();

        String amount = txn.optString("amount", "\u20B90");
        String source = txn.optString("source", "Unknown");
        String time   = txn.optString("time",   "");
        String date   = txn.optString("date",   "");
        String note   = txn.optString("note",   "");

        holder.tvAmount.setText(amount);
        holder.tvSource.setText(source);
        holder.tvTime.setText(time);
        holder.tvDate.setText(note.isEmpty() ? date : date + "  \uD83D\uDCDD " + note);
        holder.tvAvatar.setText(
            source.isEmpty() ? "?" : String.valueOf(source.charAt(0)).toUpperCase());

        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        // Tap: copy amount
        holder.itemView.setOnClickListener(v -> {
            if (!prefs.getBoolean(MoreOptionsActivity.KEY_COPY_AMOUNT, true)) return;
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("amount", amount));
                Toast.makeText(ctx, "Copied " + amount, Toast.LENGTH_SHORT).show();
            }
        });

        // Long press: share
        holder.itemView.setOnLongClickListener(v -> {
            if (!prefs.getBoolean(MoreOptionsActivity.KEY_SHARE, true)) return false;
            String text = "Received " + amount + " from " + source
                        + " on " + date + " at " + time;
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            ctx.startActivity(Intent.createChooser(share, "Share via"));
            return true;
        });

        // Double tap: note
        GestureDetector gd = new GestureDetector(ctx,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (!prefs.getBoolean(MoreOptionsActivity.KEY_NOTE, true)) return false;
                    showNoteDialog(ctx, txn, holder.tvDate, date);
                    return true;
                }
            });
        holder.itemView.setOnTouchListener((v, event) -> {
            gd.onTouchEvent(event);
            return false;
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(ctx)
                .setTitle("Delete Transaction")
                .setMessage("Remove " + amount + " from " + source + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_ID || pos >= transactions.size()) return;
                    transactions.remove(pos);
                    notifyItemRemoved(pos);
                    new Thread(() -> persistTransactions(ctx)).start();
                    if (deleteListener != null) deleteListener.onDeleted();
                    Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void showNoteDialog(Context ctx, JSONObject txn, TextView tvDate, String date) {
        EditText input = new EditText(ctx);
        input.setHint("Add a note");
        input.setText(txn.optString("note", ""));
        input.setSingleLine(true);
        int pad = (int)(16 * ctx.getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(ctx)
            .setTitle("Transaction Note")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String note = input.getText().toString().trim();
                try { txn.put("note", note); } catch (Exception ignored) {}
                tvDate.setText(note.isEmpty() ? date : date + "  \uD83D\uDCDD " + note);
                new Thread(() -> persistTransactions(ctx)).start();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", (d, w) -> {
                txn.remove("note");
                tvDate.setText(date);
                new Thread(() -> persistTransactions(ctx)).start();
            })
            .show();
    }

    private void persistTransactions(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        for (JSONObject t : transactions) arr.put(t);
        prefs.edit().putString(MainActivity.KEY_TRANSACTIONS, arr.toString()).apply();
    }

    @Override
    public int getItemCount() { return transactions.size(); }

    static class TxnViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvSource, tvDate, tvAmount, tvTime;
        AppCompatImageButton btnDelete;

        TxnViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar  = itemView.findViewById(R.id.tvTxnAvatar);
            tvSource  = itemView.findViewById(R.id.tvTxnSource);
            tvDate    = itemView.findViewById(R.id.tvTxnDate);
            tvAmount  = itemView.findViewById(R.id.tvTxnAmount);
            tvTime    = itemView.findViewById(R.id.tvTxnTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteTxn);
        }
    }
}
