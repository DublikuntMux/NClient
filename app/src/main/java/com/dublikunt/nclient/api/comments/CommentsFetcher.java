package com.dublikunt.nclient.api.comments;

import android.util.JsonReader;
import android.util.JsonToken;

import com.dublikunt.nclient.CommentActivity;
import com.dublikunt.nclient.adapters.CommentAdapter;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.utility.Utility;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CommentsFetcher extends Thread {
    private static final String COMMENT_API_URL = Utility.getBaseUrl() + "api/gallery/%d/comments";
    private final int id;
    private final CommentActivity commentActivity;
    private final List<Comment> comments = new ArrayList<>();

    public CommentsFetcher(CommentActivity commentActivity, int id) {
        this.id = id;
        this.commentActivity = commentActivity;
    }

    /**
     * Runs the task. Populates comments and posts results to #postResult (). Subclasses should override this
     */
    @Override
    public void run() {
        populateComments();
        postResult();
    }

    /**
     * Post the result of the comment. This is called when the user clicks on the result button or when the result is completed
     */
    private void postResult() {
        CommentAdapter commentAdapter = new CommentAdapter(commentActivity, comments, id);
        commentActivity.setAdapter(commentAdapter);
        commentActivity.runOnUiThread(() -> {
            commentActivity.getRecycler().setAdapter(commentAdapter);
            commentActivity.getRefresher().setRefreshing(false);
        });
    }

    /**
     * Populates the comments list with comments that belong to this comment. This is done by calling the comment API
     */
    private void populateComments() {
        String url = String.format(Locale.US, COMMENT_API_URL, id);
        try {
            Response response = Global.getClient().newCall(new Request.Builder().url(url).build()).execute();
            ResponseBody body = response.body();
            JsonReader reader = new JsonReader(new InputStreamReader(body.byteStream()));
            // Read a comment from the reader.
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                reader.beginArray();
                // Add a new comment to the comments list.
                while (reader.hasNext())
                    comments.add(new Comment(reader));
            }
            reader.close();
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
