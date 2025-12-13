package com.catchlearn.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.catchlearn.app.databinding.ActivityLoadingBinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoadingActivityy extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private ActivityLoadingBinding binding;
    private String imageUriString;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // URL API SENOPATI BARU
    private static final String BASE_URL_SENOPATI = "https://senopati-elysia.vercel.app/";

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

        // Langsung jalankan proses Vision ke Server
        processAndUploadImage(imageUri);
    }

    private void processAndUploadImage(Uri imageUri) {
        binding.tvLoadingText.setText("Mengompresi Gambar...");

        // 1. Jalankan proses berat (Kompresi & Base64) di Background Thread
        executorService.execute(() -> {
            try {
                // Gunakan mode agresif agar data kecil & cepat diupload
                String base64Image = getBase64FromUri(imageUri);

                // Kembali ke Main Thread untuk update UI & Kirim Request
                mainHandler.post(() -> {
                    if (base64Image == null) {
                        showError("Gagal memproses gambar.");
                        return;
                    }
                    Log.d(TAG, "Base64 siap. Panjang karakter: " + base64Image.length());
                    sendRequestToSenopati(base64Image);
                });

            } catch (Exception e) {
                mainHandler.post(() -> showError("Error pemrosesan gambar: " + e.getMessage()));
            }
        });
    }

    private void sendRequestToSenopati(String base64Image) {
        binding.tvLoadingText.setText("Processing by Senopati Vision");

        // 2. Siapkan Prompt
        String singlePrompt =
                "You are an AI that identifies the main object in the image and responds ONLY in valid JSON.\n" +
                        "\n" +
                        "Your tasks:\n" +
                        "1. Identify the object.\n" +
                        "2. Provide the English name.\n" +
                        "3. Provide ONE fun fact for young children (TK–SD).\n" +
                        "\n" +
                        "JSON schema:\n" +
                        "{\n" +
                        "  \"nama_indonesia\": \"string\",\n" +
                        "  \"nama_inggris\": \"string\",\n" +
                        "  \"kategori\": \"string\",\n" +
                        "  \"fakta_menarik\": \"string\"\n" +
                        "}\n" +
                        "\n" +
                        "STRICT rules for \"fakta_menarik\":\n" +
                        "• MUST be short (minimal 3 sentence and max 5 sentences).\n" +
                        "• MUST be simple enough for children.\n" +
                        "• MUST be a fun, unique, or surprising fact about the object.\n" +
                        "• MUST NOT AND DO NOT describe what the object is used for.\n" +
                        "• MUST NOT AND DO NOT describe what the object looks like.\n" +
                        "• MUST NOT be long or scientific.\n" +
                        "\n" +
                        "Fun facts may include:\n" +
                        "• A simple historical origin.\n" +
                        "• A kid-friendly trivia.\n" +
                        "• A surprising discovery.\n" +
                        "\n" +
                        "Examples of GOOD fun facts:\n" +
                        "• \"Sofa pertama dulu hanya dipakai bangsawan Mesir.\" \n" +
                        "• \"Pensil dulu dibuat dari timah, bukan grafit seperti sekarang.\" \n" +
                        "\n" +
                        "Examples of BAD WORSE fun facts:\n" +
                        "• \"Pintu di rumah anda bisa membantu anda masuk dan keluar rumah.\" \n" +
                        "• \"Pintu ini bisa dibuka dan ditutup dengan mudah.\" \n" +
                        "\n" +
                        "Language: Bahasa Indonesia only.\n" +
                        "Output: Valid JSON ONLY. No explanation, no markdown.";

        String systemInstruction = "Kamu adalah asisten JSON. Outputmu harus JSON valid saja sesuai schema yang diminta. Jangan menyapa, jangan memberi penjelasan lain.";

        // 3. Susun JSON Body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("prompt", singlePrompt);
        requestBody.addProperty("systemPrompt", systemInstruction);
        requestBody.add("messages", new JsonArray());

        // --- DATA URI SCHEME ---
        // Format ini Wajib untuk Senopati Vision API
        requestBody.addProperty("image", "data:image/jpeg;base64," + base64Image);

        // 4. Setup Retrofit
        OkHttpClient client = getUnsafeOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_SENOPATI)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SenopatiApiService service = retrofit.create(SenopatiApiService.class);

        // 5. Kirim Request
        Log.d(TAG, "Mengirim request JSON ke Senopati Elysia...");
        service.chatVision(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();

                        // --- CEK ERROR DARI API SERVER ---
                        if (body.has("success") && !body.get("success").getAsBoolean()) {
                            String errorMsg = "Server Error";
                            if (body.has("error")) {
                                if (body.get("error").isJsonObject()) {
                                    errorMsg = body.getAsJsonObject("error").get("message").getAsString();
                                } else {
                                    errorMsg = body.get("error").getAsString();
                                }
                            }
                            Log.e(TAG, "API Returned Error: " + errorMsg);
                            showError("Gagal: " + errorMsg);
                            return;
                        }

                        // --- LOGIKA EKSTRAKSI RESPON SUKSES ---
                        String reply = null;
                        if (body.has("data") && body.get("data").isJsonObject()) {
                            JsonObject dataObj = body.getAsJsonObject("data");
                            if (dataObj.has("reply") && !dataObj.get("reply").isJsonNull()) {
                                reply = dataObj.get("reply").getAsString();
                            }
                        }

                        if (reply == null) {
                            if (body.has("content") && !body.get("content").isJsonNull()) {
                                reply = body.get("content").getAsString();
                            } else if (body.has("reply") && !body.get("reply").isJsonNull()) {
                                reply = body.get("reply").getAsString();
                            } else {
                                if (!body.has("error")) {
                                    reply = body.toString();
                                }
                            }
                        }

                        Log.d(TAG, "Senopati Vision Raw Output: " + reply);

                        if (reply == null || reply.isEmpty()) {
                            showError("Respon server kosong.");
                            return;
                        }

                        parseAndShowResult(reply);

                    } catch (Exception e) {
                        Log.e(TAG, "Gagal ekstraksi JSON Response", e);
                        showError("Gagal membaca respon server.");
                    }
                } else {
                    Log.e(TAG, "Server HTTP Error: " + response.code());
                    showError("HTTP Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Koneksi Gagal", t);
                showError("Koneksi Error: " + t.getMessage());
            }
        });
    }

    private String getBase64FromUri(Uri uri) {
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }

            // --- OPTIMASI SPEED: Turunkan Max Size ke 512px (Cukup untuk AI) ---
            // Resolusi 512px jauh lebih ringan untuk diproses & diupload daripada 1024px/600px
            int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
            double ratio = (originalSize > 512) ? (originalSize / 512.0) : 1.0;

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);

            input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();

            if (bitmap == null) return null;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // --- OPTIMASI SPEED: Kualitas 50% ---
            // Kualitas 50% menghasilkan file sangat kecil (cepat upload) tapi masih terbaca AI
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            bitmap.recycle();

            return Base64.encodeToString(byteArray, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Gagal konversi gambar ke Base64", e);
            return null;
        }
    }

    private int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        return k;
    }

    private void parseAndShowResult(String jsonString) {
        try {
            String cleanJson = jsonString;

            // Bersihkan markdown block jika ada
            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.split("```json")[1].split("```")[0];
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.split("```")[1].split("```")[0];
            }

            int firstBrace = cleanJson.indexOf("{");
            int lastBrace = cleanJson.lastIndexOf("}");

            if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
                cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);

                JsonObject jsonResult = JsonParser.parseString(cleanJson.trim()).getAsJsonObject();

                if (jsonResult.has("success") && !jsonResult.get("success").getAsBoolean()) {
                    showError("API Error: " + (jsonResult.has("error") ? jsonResult.get("error").toString() : "Unknown"));
                    return;
                }

                String nama = jsonResult.has("nama_indonesia") ? jsonResult.get("nama_indonesia").getAsString() :
                        (jsonResult.has("nama") ? jsonResult.get("nama").getAsString() : "Benda Misterius");

                String inggris = jsonResult.has("nama_inggris") ? jsonResult.get("nama_inggris").getAsString() :
                        (jsonResult.has("inggris") ? jsonResult.get("inggris").getAsString() : "Unknown");

                String kategori = jsonResult.has("kategori") ? jsonResult.get("kategori").getAsString() : "Umum";

                String fakta = jsonResult.has("fakta_menarik") ? jsonResult.get("fakta_menarik").getAsString() :
                        (jsonResult.has("fakta") ? jsonResult.get("fakta").getAsString() : "Tidak ada fakta.");

                startResultActivity(nama, inggris, kategori, fakta);
            } else {
                startResultActivity("Gagal Baca", "Error", "Error", "AI memberikan respons yang tidak bisa dibaca.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error Parsing Final", e);
            startResultActivity("Gagal Proses", "-", "-", "Format data server tidak sesuai.");
        }
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        binding.ivCapturedImage.postDelayed(this::finish, 3000);
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };
            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            // Timeout 120s untuk jaga-jaga server cold start
            builder.connectTimeout(120, TimeUnit.SECONDS);
            builder.readTimeout(120, TimeUnit.SECONDS);
            builder.writeTimeout(120, TimeUnit.SECONDS);

            builder.addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                okhttp3.Request request = original.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:91.0) Gecko/91.0 Firefox/91.0")
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}