package com.dublikunt.nclient.components.classes.integration;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.dublikunt.nclient.settings.CustomInterceptor;

import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    private final Call.Factory client;

    public OkHttpUrlLoader(@NonNull Call.Factory client) {
        this.client = client;
    }

    @Override
    public boolean handles(@NonNull GlideUrl url) {
        return true;
    }

    @Override
    public LoadData<InputStream> buildLoadData(
        @NonNull GlideUrl model, int width, int height, @NonNull Options options) {
        return new LoadData<>(model, new OkHttpStreamFetcher(client, model));
    }

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private static volatile Call.Factory internalClient;
        private final Call.Factory client;

        public Factory() {
            this(getInternalClient());
        }

        public Factory(@NonNull Call.Factory client) {
            this.client = client;
        }

        private static Call.Factory getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        OkHttpClient.Builder builder = new OkHttpClient.Builder();
                        builder.addInterceptor(new CustomInterceptor(null, false));
                        OkHttpClient client = builder.build();
                        client.dispatcher().setMaxRequests(25);
                        client.dispatcher().setMaxRequestsPerHost(25);
                        internalClient = (Call.Factory) client;
                    }
                }
            }
            return internalClient;
        }

        @NonNull
        @Override
        public ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new OkHttpUrlLoader(client);
        }

        @Override
        public void teardown() {
        }
    }
}
