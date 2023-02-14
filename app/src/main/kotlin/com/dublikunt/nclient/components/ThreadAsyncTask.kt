package com.dublikunt.nclient.components

import androidx.appcompat.app.AppCompatActivity
import com.dublikunt.nclient.settings.Global

abstract class ThreadAsyncTask<Params, Progress, Result>(private val activity: AppCompatActivity) {
    @SafeVarargs
    fun execute(vararg params: Params) {
        val thread: Thread = AsyncThread(params as Array<Params>)
        thread.start()
    }

    protected fun onPreExecute() {}
    protected open fun onPostExecute(result: Result) {}
    protected open fun onProgressUpdate(vararg values: Progress) {}
    protected abstract fun doInBackground(vararg params: Params): Result
    @SafeVarargs
    protected fun publishProgress(vararg values: Progress) {
        if (!Global.isDestroyed(activity)) activity.runOnUiThread { onProgressUpdate(*values) }
    }

    internal inner class AsyncThread(var params: Array<Params>) : Thread() {
        override fun run() {
            if (!Global.isDestroyed(activity)) activity.runOnUiThread { onPreExecute() }
            val result = doInBackground(*params)
            if (!Global.isDestroyed(activity)) activity.runOnUiThread { onPostExecute(result) }
        }
    }
}
