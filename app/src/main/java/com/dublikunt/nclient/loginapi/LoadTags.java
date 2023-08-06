package com.dublikunt.nclient.loginapi;

import android.util.JsonReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dublikunt.nclient.adapters.TagsAdapter;
import com.dublikunt.nclient.api.components.Tag;
import com.dublikunt.nclient.api.enums.TagType;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.settings.Login;
import com.dublikunt.nclient.utility.LogUtility;
import com.dublikunt.nclient.utility.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class LoadTags extends Thread {
    @Nullable
    private final TagsAdapter adapter;

    public LoadTags(@Nullable TagsAdapter adapter) {
        this.adapter = adapter;
    }

    @NonNull
    private Elements getScripts(String url) throws IOException {

        Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
        Elements x = Jsoup.parse(response.body().byteStream(), null, Utility.getBaseUrl()).getElementsByTag("script");
        response.close();
        return x;
    }

    @NonNull
    private String extractArray(@NonNull Element e) throws StringIndexOutOfBoundsException {
        String t = e.toString();
        return t.substring(t.indexOf('['), t.indexOf(';'));
    }

    private void readTags(@NonNull JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            Tag tt = new Tag(reader);
            if (tt.getType() != TagType.LANGUAGE && tt.getType() != TagType.CATEGORY) {
                Login.addOnlineTag(tt);
                if (adapter != null) adapter.addItem();
            }
        }
    }

    @Override
    public void run() {
        super.run();
        if (Login.getUser() == null) return;
        String url = String.format(Locale.US, Utility.getBaseUrl() + "users/%s/%s/blacklist",
            Login.getUser().getId(), Login.getUser().getCodename()
        );
        LogUtility.d(url);
        try {
            Elements scripts = getScripts(url);
            analyzeScripts(scripts);
        } catch (IOException | StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

    }

    private void analyzeScripts(@NonNull Elements scripts) throws IOException, StringIndexOutOfBoundsException {
        if (scripts.size() > 0) {
            Login.clearOnlineTags();
            String array = Utility.unescapeUnicodeString(extractArray(scripts.last()));
            JsonReader reader = new JsonReader(new StringReader(array));
            readTags(reader);
            reader.close();
        }
    }
}
