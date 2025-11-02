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

// Import BuildConfig jika diperlukan
import com.example.finalprojectppb.BuildConfig;
import com.example.finalprojectppb.databinding.ActivityLoadingBinding;

// Import Gemini
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;

// Import Guava
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
    private String imageUriString; // Simpan URI untuk dikirim ke Result

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

        // Konversi URI ke Bitmap dan panggil Gemini
        try {
            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            runGemini(imageBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Gagal mengonversi URI ke Bitmap", e);
            showError("Gagal memuat gambar untuk analisis.");
        }
    }

    /**
     * Menjalankan panggilan API ke Gemini untuk menganalisis gambar dan meminta JSON via prompt (tanpa setResponseMimeType).
     *
     * @param imageBitmap Bitmap dari gambar yang akan dianalisis.
     */
    private void runGemini(Bitmap imageBitmap) {
        // Config minimal tanpa setResponseMimeType
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();


        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-pro",  // Model stabil
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build(),
                Collections.emptyList()
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // "Hack" prompt: Paksa Gemini mengembalikan HANYA JSON tanpa teks lain
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

                    // --- TAMBAHKAN INI UNTUK MEMBERSIHKAN MARKDOWN ---
                    if (jsonText.startsWith("```json")) {
                        jsonText = jsonText.substring(7); // Hapus ```json
                    }
                    if (jsonText.endsWith("```")) {
                        jsonText = jsonText.substring(0, jsonText.length() - 3); // Hapus ```
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

                    // Kirim data ke ResultActivity
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

    /**
     * Menampilkan pesan error dan kembali setelah 3 detik.
     *
     * @param message Pesan error yang akan ditampilkan.
     */
    private void showError(String message) {
        binding.ivScannerLine.clearAnimation();
        binding.ivScannerLine.setVisibility(View.GONE);
        binding.tvLoadingText.setText(message);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
    }

    /**
     * Memulai ResultActivity dan mengirim data.
     */
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
