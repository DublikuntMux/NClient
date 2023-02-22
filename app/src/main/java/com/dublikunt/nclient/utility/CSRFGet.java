package com.dublikunt.nclient.utility;

import androidx.annotation.Nullable;

import com.dublikunt.nclient.settings.Global;

import java.io.IOException;

import okhttp3.Request;

public class CSRFGet extends Thread {
    @Nullable
    private final Response response;
    private final String url;

    public CSRFGet(@Nullable Response response, String url) {
        this.response = response;
        this.url = url;
    }

    /**
     * Executes the request and returns the token. This is called by the Thread#interrupt () method when the thread is done
     */
    @Override
    public void run() {
        try {
            okhttp3.Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
            response.body();
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
        /**
         * Called when we have a response. This is the place to do stuff that needs to be done in order to get the user's access to the API
         *
         * @param token - The token that was
         */
        void onResponse(String token) throws IOException;

        /**
         * Called when an error occurs. This is a no - op for this type of event. The default implementation prints the stack trace to System. err
         *
         * @param e - The exception that occurred
         */
        default void onError(Exception e) {
            e.printStackTrace();
        }
    }
}
