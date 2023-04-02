package com.dublikunt.nclient.utility;

import androidx.annotation.Nullable;

import com.dublikunt.nclient.settings.Global;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Request;

public class CSRFGet extends Thread {
    @Nullable
    private final Response response;
    private final String url;

    public CSRFGet(@Nullable Response response, String url) {
        this.response = response;
        this.url = url;
    }

    @Override
    public void run() {
        try {
            okhttp3.Response response = Objects.requireNonNull(Global.getClient()).newCall(new Request.Builder().url(url).build()).execute();
            String token = response.body().string();
            token = token.substring(token.lastIndexOf("csrf_token"));
            token = token.substring(token.indexOf('"') + 1);
            token = token.substring(0, token.indexOf('"'));
            // Called when the response is received.
            if (this.response != null) this.response.onResponse(token);
        } catch (Exception e) {
            // Called when an error occurs.
            if (response != null) response.onError(e);
        }
    }

    public interface Response {
        void onResponse(String token) throws IOException;

        default void onError(Exception e) {
            e.printStackTrace();
        }
    }
}
