package com.civicai.citizen.voice.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class GeminiApiClient {
    // For emulators 10.0.2.2 is used. For physical devices with adb reverse, 127.0.0.1 is used.
    // Replace with your local machine's IP address (e.g., 192.168.1.100) if needed.
    private static final String BASE_URL = "http://10.21.204.179:3000/";
    
    private static Retrofit retrofit = null;

    public static GeminiApiService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(GeminiApiService.class);
    }
}
