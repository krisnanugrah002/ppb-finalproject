package com.example.finalprojectppb;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SenopatiApiService {
    // Endpoint baru sesuai dokumentasi Vercel
    @POST("api/v1/chat")
    Call<JsonObject> chat(@Body JsonObject requestBody);
}