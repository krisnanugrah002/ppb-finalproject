package com.example.finalprojectppb;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.finalprojectppb.BuildConfig;
import com.example.finalprojectppb.databinding.ActivityLoadingBinding;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;

public class LoadingActivityy extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private ActivityLoadingBinding binding;
    private String imageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoadingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUriString = getIntent().getStringExtra("IMAGE_URI");
        if (imageUriString == null) {
            Log.e(TAG, "Tidak ada URI gambar yang diterima.");
            finish();
            return;
        }

        Uri imageUri = Uri.parse(imageUriString);
        binding.ivCapturedImage.setImageURI(imageUri);

        Animation scanAnimation = AnimationUtils.loadAnimation(this, R.anim.scanner_animation);
        binding.ivScannerLine.startAnimation(scanAnimation);

        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            runGemini(imageBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Gagal mengonversi URI ke Bitmap", e);
            showError("Gagal memuat gambar untuk analisis.");
        }
    }

    private void runGemini(Bitmap imageBitmap) {
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();


        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-pro",
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build(),
                Collections.emptyList()
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String promptText = "Analisis gambar ini. Identifikasi objek utama. " +
                "Kembalikan HANYA string JSON yang valid, tanpa teks tambahan, penjelasan, atau markdown. " +
                "Format JSON harus persis seperti ini: {\"nama\": \"[Nama objek dalam Bahasa Indonesia]\", \"inggris\": \"[Nama objek dalam Bahasa Inggris]\", \"kategori\": \"[Kategori objek, misal: Buah, Perabotan, Hewan]\", \"fakta\": \"[Satu fakta unik atau deskripsi singkat tentang objek ini]\"}";

        Content content = new Content.Builder()
                .addImage(imageBitmap)
                .addText(promptText)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(@Nullable GenerateContentResponse result) {
                try {
                    String jsonText = result.getText().trim();
                    Log.d(TAG, "Gemini Response: " + jsonText);

                    if (jsonText.startsWith("```json")) {
                        jsonText = jsonText.substring(7);
                    }
                    if (jsonText.endsWith("```")) {
                        jsonText = jsonText.substring(0, jsonText.length() - 3);
                    }
                    jsonText = jsonText.trim();


                    if (!jsonText.startsWith("{") || !jsonText.endsWith("}")) {
                        throw new JSONException("Respons bukan JSON valid: " + jsonText);
                    }

                    JSONObject json = new JSONObject(jsonText);
                    String nama = json.optString("nama", "Tidak ditemukan");
                    String inggris = json.optString("inggris", "Not found");
                    String kategori = json.optString("kategori", "Tidak diketahui");
                    String fakta = json.optString("fakta", "Tidak ada fakta");

                    startResultActivity(nama, inggris, kategori, fakta);

                } catch (JSONException e) {
                    Log.e(TAG, "Gagal parsing JSON", e);
                    showError("Respons AI tidak valid. Coba lagi.");
                } catch (Exception e) {
                    Log.e(TAG, "Error tak terduga", e);
                    showError("Terjadi error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "Panggilan Gemini gagal", t);
                showError("Gagal menghubungi AI: " + t.getMessage());
            }
        }, mainExecutor);
    }

    private void showError(String message) {
        binding.ivScannerLine.clearAnimation();
        binding.ivScannerLine.setVisibility(View.GONE);
        binding.tvLoadingText.setText(message);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
    }

    private void startResultActivity(String nama, String inggris, String kategori, String fakta) {
        Intent intent = new Intent(LoadingActivityy.this, ResultActivity.class);
        intent.putExtra("IMAGE_URI", imageUriString);
        intent.putExtra("NAMA_OBJEK", nama);
        intent.putExtra("INGGRIS_OBJEK", inggris);
        intent.putExtra("KATEGORI_OBJEK", kategori);
        intent.putExtra("FAKTA_OBJEK", fakta);
        startActivity(intent);
        finish();
    }
}