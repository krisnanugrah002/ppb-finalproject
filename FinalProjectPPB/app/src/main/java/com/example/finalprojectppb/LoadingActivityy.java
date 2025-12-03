package com.example.finalprojectppb;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectppb.databinding.ActivityLoadingBinding;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoadingActivityy extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private ActivityLoadingBinding binding;
    private String imageUriString;

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
        uploadImageToSenopati(imageUri);
    }

    private void uploadImageToSenopati(Uri imageUri) {
        binding.tvLoadingText.setText("Processing by Senopati Vision");

        // 1. Siapkan File dari URI dengan KOMPRESI
        File file = getCompressedFileFromUri(imageUri);

        if (file == null) {
            showError("Gagal memproses file gambar.");
            return;
        }

        Log.d(TAG, "File siap upload. Ukuran: " + (file.length() / 1024) + " KB");

        // 2. Siapkan Request Body (Multipart)
        // Image (Pastikan tipe MIME adalah image/jpeg karena kita sudah kompres ke JPG)
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part bodyImage = MultipartBody.Part.createFormData("image", "upload.jpg", requestFile);

        // --- UPDATE PENTING: PROMPT BARU DENGAN PERSONA CURATOR + FIX KATEGORI ---
        // Saya menambahkan field "kategori" ke dalam JSON Schema agar AI mengisinya.
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


        RequestBody bodyPrompt = RequestBody.create(MediaType.parse("text/plain"), singlePrompt);

        // System Prompt
        String systemInstruction = "Kamu adalah asisten JSON. Outputmu harus JSON valid saja sesuai schema yang diminta. Jangan menyapa, jangan memberi penjelasan lain.";
        RequestBody bodySystemPrompt = RequestBody.create(MediaType.parse("text/plain"), systemInstruction);

        // Messages
        RequestBody bodyMessages = RequestBody.create(MediaType.parse("text/plain"), "[]");

        // 3. Setup Retrofit
        OkHttpClient client = getUnsafeOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_SENOPATI)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SenopatiApiService service = retrofit.create(SenopatiApiService.class);

        // 4. Kirim Request
        Log.d(TAG, "Mengirim request multipart ke Senopati Elysia...");
        service.chatVision(bodyImage, bodyPrompt, bodySystemPrompt, bodyMessages).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        String reply = null;

                        // --- LOGIKA EKSTRAKSI RESPON ---
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
                                reply = body.toString();
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
                        showError("Gagal membaca struktur data server.");
                    }
                } else {
                    Log.e(TAG, "Server Error: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {}
                    showError("Gagal: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Koneksi Gagal", t);
                showError("Error Koneksi: " + t.getMessage());
            }
        });
    }

    private void parseAndShowResult(String jsonString) {
        try {
            String cleanJson = jsonString;

            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.split("```json")[1].split("```")[0];
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.split("```")[1].split("```")[0];
            }

            int firstBrace = cleanJson.indexOf("{");
            int lastBrace = cleanJson.lastIndexOf("}");

            if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
                cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
                Log.d(TAG, "Final JSON to Parse: " + cleanJson);

                JsonObject jsonResult = JsonParser.parseString(cleanJson.trim()).getAsJsonObject();

                String nama = jsonResult.has("nama_indonesia") ? jsonResult.get("nama_indonesia").getAsString() :
                        (jsonResult.has("nama") ? jsonResult.get("nama").getAsString() : "Benda Misterius");

                String inggris = jsonResult.has("nama_inggris") ? jsonResult.get("nama_inggris").getAsString() :
                        (jsonResult.has("inggris") ? jsonResult.get("inggris").getAsString() : "Unknown");

                // Kategori sekarang akan diambil dari JSON jika AI memberikannya
                String kategori = jsonResult.has("kategori") ? jsonResult.get("kategori").getAsString() : "Umum";

                String fakta = jsonResult.has("fakta_menarik") ? jsonResult.get("fakta_menarik").getAsString() :
                        (jsonResult.has("fakta") ? jsonResult.get("fakta").getAsString() : "Tidak ada fakta.");

                startResultActivity(nama, inggris, kategori, fakta);
            } else {
                Log.w(TAG, "Respon bukan JSON, menggunakan fallback manual: " + cleanJson);
                String namaObjek = cleanJson.trim().replace("\"", "");
                if (namaObjek.length() > 50) namaObjek = namaObjek.substring(0, 50);
                startResultActivity(namaObjek, "Translate Me", "Umum", "AI hanya memberikan nama, tidak ada fakta detail.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error Parsing Final", e);
            startResultActivity("Gagal Proses", "-", "-", "Format data tidak dikenali.");
        }
    }

    // --- PERBAIKAN UTAMA: KOMPRESI GAMBAR ---
    // Metode ini mengubah ukuran gambar agar tidak terlalu besar (Max 1024px)
    // dan mengompresnya menjadi JPG agar diterima server Vercel (Limit 4.5MB).
    private File getCompressedFileFromUri(Uri uri) {
        try {
            // 1. Decode dimensi gambar terlebih dahulu (tanpa memuat seluruh gambar ke memori)
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();

            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }

            // 2. Hitung skala reduksi agar gambar tidak lebih besar dari 1024px
            // Ini sangat penting untuk mengurangi penggunaan memori dan ukuran file
            int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
            double ratio = (originalSize > 1024) ? (originalSize / 1024.0) : 1.0;

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);

            // 3. Decode gambar dengan skala yang sudah dihitung
            input = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();

            if (bitmap == null) return null;

            // 4. Simpan gambar yang sudah di-resize ke file cache sebagai JPG
            File tempFile = new File(getCacheDir(), "upload_compressed.jpg");
            FileOutputStream out = new FileOutputStream(tempFile);

            // Kompresi ke JPEG dengan kualitas 70% (Cukup bagus, ukuran kecil)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);

            out.flush();
            out.close();

            // Bersihkan memori bitmap
            bitmap.recycle();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Gagal mengompres gambar", e);
            return null;
        }
    }

    // Helper untuk menghitung sample size (harus pangkat 2: 1, 2, 4, 8...)
    private int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        return k;
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
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        binding.ivCapturedImage.postDelayed(this::finish, 2000);
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
            builder.connectTimeout(60, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}