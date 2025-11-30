package com.example.finalprojectppb;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface SenopatiApiService {

    // Endpoint untuk Chat Text (jika masih dibutuhkan di tempat lain)
    @POST("api/chat")
    Call<JsonObject> chat(@Body JsonObject requestBody);

    // Endpoint BARU untuk Vision (Upload Gambar)
    // Sesuai dokumentasi Bruno: Chat Vision.bru -> multipartForm
    @Multipart
    @POST("api/vision")
    Call<JsonObject> chatVision(
            @Part MultipartBody.Part image,
            @Part("prompt") RequestBody prompt,
            @Part("systemPrompt") RequestBody systemPrompt,
            @Part("messages") RequestBody messages
    );
}