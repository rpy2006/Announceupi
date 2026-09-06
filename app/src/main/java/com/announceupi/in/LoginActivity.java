// FILE: app/src/main/java/com/announceupi/in/LoginActivity.java
package com.announceupi.in;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private ProgressBar        progressLogin;
    private View               btnGoogleSignIn;

    private final ActivityResultLauncher<Intent> signInLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleSignInResult(task);
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already authenticated, skip login screen
        if (GoogleAuthManager.isAuthenticated(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        googleSignInClient = GoogleAuthManager.getClient(this);
        progressLogin      = findViewById(R.id.progressLogin);
        btnGoogleSignIn    = findViewById(R.id.btnGoogleSignIn);

        // Google Sign-In button
        btnGoogleSignIn.setOnClickListener(v -> {
            showLoading(true);
            signInLauncher.launch(googleSignInClient.getSignInIntent());
        });

        // Skip login
        TextView tvSkip = findViewById(R.id.tvSkipLogin);
        tvSkip.setOnClickListener(v -> {
            GoogleAuthManager.setSkipLogin(this, true);
            goToMain();
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            GoogleAuthManager.saveUser(this, account);
            Toast.makeText(this,
                "Welcome, " + account.getDisplayName() + "!",
                Toast.LENGTH_SHORT).show();
            goToMain();
        } catch (ApiException e) {
            showLoading(false);
            String msg;
            switch (e.getStatusCode()) {
                case 12501: msg = "Sign-in cancelled"; break;
                case 12502: msg = "Sign-in in progress"; break;
                case 10:    msg = "Developer error — check SHA-1 in Google Console"; break;
                default:    msg = "Sign-in failed (code " + e.getStatusCode() + ")"; break;
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean show) {
        progressLogin.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGoogleSignIn.setEnabled(!show);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Don't allow going back from login screen
        finishAffinity();
    }
}
