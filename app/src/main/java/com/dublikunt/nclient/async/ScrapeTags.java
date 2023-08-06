package com.dublikunt.nclient.async;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.JsonReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.dublikunt.nclient.api.components.Tag;
import com.dublikunt.nclient.api.enums.TagStatus;
import com.dublikunt.nclient.api.enums.TagType;
import com.dublikunt.nclient.async.database.Queries;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.utility.LogUtility;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ScrapeTags extends JobIntentService {
    private static final int DAYS_UNTIL_SCRAPE = 7;
    private static final String DATA_FOLDER = "https://raw.githubusercontent.com/DublikuntMux/NClient/main/data/";
    private static final String TAGS = DATA_FOLDER + "tags.json";
    private static final String VERSION = DATA_FOLDER + "tagsVersion";

    public ScrapeTags() {
    }

    public static void startWork(Context context) {
        enqueueWork(context, ScrapeTags.class, 2000, new Intent());
    }

    private int getNewVersionCode() throws IOException {
        Response x = Global.getClient(this).newCall(new Request.Builder().url(VERSION).build()).execute();
        ResponseBody body = x.body();
        try {
            int k = Integer.parseInt(body.string().trim());
            LogUtility.d("Found version: " + k);
            x.close();
            return k;
        } catch (NumberFormatException e) {
            LogUtility.e("Unable to convert", e);
        }
        return -1;
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("Settings", 0);
        Date nowTime = new Date();
        Date lastTime = new Date(preferences.getLong("lastSync", nowTime.getTime()));
        int lastVersion = preferences.getInt("lastTagsVersion", -1), newVersion = -1;
        if (!enoughDayPassed(nowTime, lastTime)) return;

        LogUtility.d("Scraping tags");
        try {
            newVersion = getNewVersionCode();
            if (lastVersion > -1 && lastVersion >= newVersion) return;
            List<Tag> tags = Queries.TagTable.getAllFiltered();
            fetchTags();
            for (Tag t : tags) Queries.TagTable.updateStatus(t.getId(), t.getStatus());
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtility.d("End scraping");
        preferences.edit()
            .putLong("lastSync", nowTime.getTime())
            .putInt("lastTagsVersion", newVersion)
            .apply();
    }

    private void fetchTags() throws IOException {
        Response x = Global.getClient(this).newCall(new Request.Builder().url(TAGS).build()).execute();
        ResponseBody body = x.body();
        JsonReader reader = new JsonReader(body.charStream());
        reader.beginArray();
        while (reader.hasNext()) {
            Tag tag = readTag(reader);
            Queries.TagTable.insertScrape(tag, true);
        }
        reader.close();
        x.close();
    }

    @NonNull
    @Contract("_ -> new")
    private Tag readTag(@NonNull JsonReader reader) throws IOException {
        reader.beginArray();
        int id = reader.nextInt();
        String name = reader.nextString();
        int count = reader.nextInt();
        TagType type = TagType.values[reader.nextInt()];
        reader.endArray();
        return new Tag(name, count, id, type, TagStatus.DEFAULT);
    }

    private boolean enoughDayPassed(@NonNull Date nowTime, @NonNull Date lastTime) {
        if (nowTime.getTime() == lastTime.getTime()) return true;
        int daysBetween = 0;
        Calendar now = Calendar.getInstance(), last = Calendar.getInstance();
        now.setTime(nowTime);
        last.setTime(lastTime);
        while (last.before(now)) {
            last.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
            if (daysBetween > DAYS_UNTIL_SCRAPE)
                return true;
        }
        LogUtility.d("Passed " + daysBetween + " days since last scrape");
        return false;
    }
}
