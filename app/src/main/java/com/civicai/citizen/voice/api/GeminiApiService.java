package com.civicai.citizen.voice.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GeminiApiService {
    @POST("api/assistant/message")
    Call<NluResponse> processMessage(@Body NluRequest request);
}
