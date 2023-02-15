package com.dublikunt.nclient.settings

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.dublikunt.nclient.R
import com.dublikunt.nclient.utility.LogUtility.download
import java.util.concurrent.CopyOnWriteArrayList

class NotificationSettings private constructor(private val notificationManager: NotificationManagerCompat) {
    companion object {
        private val notificationArray: MutableList<Int> = CopyOnWriteArrayList()
        private var notificationSettings: NotificationSettings? = null

        @JvmStatic
        var notificationId = 999
            get() = field++
            private set
        private var maximumNotification = 0
        fun initializeNotificationManager(context: Context) {
            notificationSettings =
                NotificationSettings(NotificationManagerCompat.from(context.applicationContext))
            maximumNotification = context.getSharedPreferences("Settings", 0)
                .getInt(context.getString(R.string.key_maximum_notification), 25)
            trimArray()
        }

        @JvmStatic
        fun notify(channel: String?, notificationId: Int, notification: Notification?) {
            if (maximumNotification == 0) return
            notificationArray.remove(Integer.valueOf(notificationId))
            notificationArray.add(notificationId)
            trimArray()
            download("Notification count: " + notificationArray.size)
            notificationSettings!!.notificationManager.notify(notificationId, notification!!)
        }

        @JvmStatic
        fun cancel(channel: String?, notificationId: Int) {
            notificationSettings!!.notificationManager.cancel(notificationId)
            notificationArray.remove(Integer.valueOf(notificationId))
        }

        private fun trimArray() {
            while (notificationArray.size > maximumNotification) {
                val first = notificationArray.removeAt(0)
                notificationSettings!!.notificationManager.cancel(first)
            }
        }
    }
}
