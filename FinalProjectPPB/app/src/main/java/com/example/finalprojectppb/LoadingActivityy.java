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

import com.example.finalprojectppb.databinding.ActivityLoadingBinding;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

// Import GSON dan Retrofit
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit; // Penting untuk mengatur waktu tunggu
import okhttp3.OkHttpClient;          // Penting untuk klien HTTP

public class LoadingActivityy extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private ActivityLoadingBinding binding;
    private String imageUriString;

    // URL API SENOPATI (VERCEL)
    private static final String BASE_URL_SENOPATI = "https://senopati-api.vercel.app/";

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
            // MULAI TAHAP 1: Gemini Vision
            runGeminiVision(imageBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Gagal mengonversi URI ke Bitmap", e);
            showError("Gagal memuat gambar untuk analisis.");
        }
    }

    // --- TAHAP 1: Gemini Vision (Ambil Nama Objek) ---
    private void runGeminiVision(Bitmap imageBitmap) {
        binding.tvLoadingText.setText("Menganalisis gambar...");

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.4f;

        // Versi Gemini TETAP 2.5-flash sesuai permintaan
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build(),
                Collections.emptyList()
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String promptText = "Apa nama satu objek utama yang paling menonjol di gambar ini? Jawab hanya dengan nama bendanya dalam Bahasa Indonesia. Jangan ada kata lain.";

        Content content = new Content.Builder()
                .addImage(imageBitmap)
                .addText(promptText)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(@Nullable GenerateContentResponse result) {
                if (result != null) {
                    String detectedObjectName = result.getText().trim();
                    detectedObjectName = detectedObjectName.replace("```", "").trim();

                    Log.d(TAG, "Gemini Output: " + detectedObjectName);

                    // Lanjut ke TAHAP 2: Kirim nama objek ke Senopati
                    fetchDetailsFromSenopati(detectedObjectName);
                } else {
                    showError("Gemini tidak mengembalikan hasil.");
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "Gemini gagal", t);
                showError("Gagal mengenali gambar: " + t.getMessage());
            }
        }, mainExecutor);
    }

    // --- TAHAP 2: Senopati API Vercel (Ambil Fakta & Info) ---
    private void fetchDetailsFromSenopati(String objectName) {
        runOnUiThread(() -> binding.tvLoadingText.setText("Mencari fakta unik..."));

        // 1. GUNAKAN CLIENT UNSAFE (Bypass SSL Error)
        OkHttpClient client = getUnsafeOkHttpClient();

        // 2. PASANG CLIENT KE RETROFIT
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_SENOPATI)
                .client(client) // Menggunakan client yang sudah di-bypass SSL-nya
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SenopatiApiService service = retrofit.create(SenopatiApiService.class);

        // --- KONSTRUKSI REQUEST UNTUK API SENOPATI ---
        JsonObject payload = new JsonObject();

        String systemInstruction = "Kamu adalah ensiklopedia anak pintar. " +
                "Berikan detail objek dalam format JSON VALID tanpa markdown. " +
                "Format wajib: {\"inggris\": \"[Nama Inggris]\", \"kategori\": \"[Kategori]\", \"fakta\": \"[Fakta singkat]\"}";
        payload.addProperty("systemPrompt", systemInstruction);
        payload.addProperty("prompt", "Jelaskan tentang objek: " + objectName);
        payload.add("messages", new JsonArray());

        Log.d(TAG, "Mengirim ke Senopati: " + payload.toString());

        service.chat(payload).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String reply = response.body().get("reply").getAsString();
                        Log.d(TAG, "Senopati Reply: " + reply);

                        if (reply.contains("```json")) {
                            reply = reply.split("```json")[1].split("```")[0];
                        } else if (reply.contains("```")) {
                            reply = reply.split("```")[1].split("```")[0];
                        }

                        JsonObject jsonResult = JsonParser.parseString(reply.trim()).getAsJsonObject();

                        String inggris = jsonResult.has("inggris") ? jsonResult.get("inggris").getAsString() : "Unknown";
                        String kategori = jsonResult.has("kategori") ? jsonResult.get("kategori").getAsString() : "Umum";
                        String fakta = jsonResult.has("fakta") ? jsonResult.get("fakta").getAsString() : "Tidak ada fakta.";

                        startResultActivity(objectName, inggris, kategori, fakta);

                    } catch (Exception e) {
                        Log.e(TAG, "Gagal parsing JSON Senopati", e);
                        startResultActivity(objectName, "Nama Inggris", "Error", "Gagal memproses data.");
                    }
                } else {
                    Log.e(TAG, "Senopati Error: " + response.code());
                    showError("Server Senopati sibuk (Error " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Koneksi Senopati Error Detail: ", t);

                String pesanError;

                // Deteksi jenis error dengan pesan yang lebih spesifik
                if (t instanceof java.net.SocketTimeoutException) {
                    pesanError = "Waktu habis! Server terlalu lambat menjawab.";
                } else if (t instanceof java.security.cert.CertPathValidatorException ||
                        t instanceof javax.net.ssl.SSLHandshakeException) {
                    pesanError = "Masalah Keamanan SSL (Harusnya sudah fix).";
                } else if (t instanceof java.net.UnknownHostException) {
                    pesanError = "DNS Error: Tidak bisa menemukan server.";
                } else {
                    pesanError = "Error: " + t.getMessage();
                }

                showError(pesanError);
            }
        });
    }

    // --- METHOD TAMBAHAN UNTUK BYPASS SSL (CERTIFICATE ERROR) ---
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Buat TrustManager yang mempercayai semua sertifikat
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install TrustManager
            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Buat SocketFactory
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true); // Izinkan semua hostname

            // Tetap pasang Timeout 60 detik agar tidak RTO
            builder.connectTimeout(60, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            builder.writeTimeout(60, TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showError(String message) {
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