package com.catchlearn.app;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SenopatiApiService {

    // Endpoint untuk Chat Text
    @POST("api/chat")
    Call<JsonObject> chat(@Body JsonObject requestBody);

    // [DIPERBAIKI] Endpoint Vision sekarang menggunakan JSON Body (Base64), bukan Multipart
    @POST("api/vision")
    Call<JsonObject> chatVision(@Body JsonObject requestBody);
}