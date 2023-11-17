package com.dublikunt.nclient.components;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ThreadAsyncTask<Params, Progress, Result> {
    private final AppCompatActivity activity;
    private Thread thread;

    public ThreadAsyncTask(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void execute(Params... params) {
        thread = new AsyncThread(params);
        thread.start();
    }

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected abstract Result doInBackground(Params... params);

    protected final void publishProgress(Progress... values) {
        if (!activity.isDestroyed())
            activity.runOnUiThread(() -> onProgressUpdate(values));
    }

    class AsyncThread extends Thread {
        Params[] params;

        AsyncThread(Params[] params) {
            this.params = params;
        }

        @Override
        public void run() {
            if (!activity.isDestroyed())
                activity.runOnUiThread(ThreadAsyncTask.this::onPreExecute);
            Result result = doInBackground(params);
            if (!activity.isDestroyed())
                activity.runOnUiThread(() -> onPostExecute(result));
        }
    }
}
