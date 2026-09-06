// FILE: app/src/main/java/com/announceupi/in/SplashActivity.java
package com.announceupi.in;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Route to LoginActivity — it will skip itself if already authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }

    @Override
    public void onBackPressed() {
        // Disabled on splash
    }
}
