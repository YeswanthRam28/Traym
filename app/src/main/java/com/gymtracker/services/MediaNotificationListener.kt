package com.gymtracker.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * An empty NotificationListenerService.
 * By binding to this service and having the user grant Notification Access,
 * we unlock the ability to use MediaSessionManager globally.
 */
class MediaNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaNotification", "NotificationListenerService connected")
        // We can trigger the manager here, but it's easier to just use MediaSessionManager directly from the manager
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("MediaNotification", "NotificationListenerService disconnected")
    }
}
