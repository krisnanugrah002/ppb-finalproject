package com.example.finalprojectppb; // Ganti dengan nama paket Anda

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Waktu tampil splash screen (dalam milidetik)
    private static final int SPLASH_TIME_OUT = 3000; // 3 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Handler untuk menunda perpindahan ke MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Buat Intent untuk pindah ke MainActivity
                // (MainActivity adalah Activity yang akan memuat activity_main_camera.xml)
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(i);

                // Tutup activity ini agar tidak bisa kembali ke splash screen
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
