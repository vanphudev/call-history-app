package com.antimobile.callhs.data.messaging.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.antimobile.callhs.MainActivity
import com.antimobile.callhs.R
import com.antimobile.callhs.data.messaging.MessagingIntentParser
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.receiver.messaging.MarkConversationReadReceiver
import com.antimobile.callhs.receiver.messaging.NotificationReplyReceiver

object MessageNotifier {
    const val CHANNEL_ID = "sms_messages_v1"
    const val REMOTE_INPUT_KEY = "sms_reply_text"
    const val EXTRA_THREAD_ID = "thread_id"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_SUB_ID = "subscription_id"

    fun ensureChannel(context: Context) {
        val s = LanguageSettings.stringsFor(context).messaging
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, s.notificationChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.notificationChannelDescription
                enableVibration(true)
                enableLights(true)
                lightColor = Color.rgb(52, 168, 83)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
            }
        )
    }

    fun notifyIncoming(
        context: Context,
        threadId: Long,
        address: String,
        title: String,
        body: String,
        timestamp: Long,
        subscriptionId: Int?,
    ) {
        if (MessagingForegroundState.visibleThreadId == threadId) return
        ensureChannel(context)
        if (!canPost(context)) return
        val s = LanguageSettings.stringsFor(context).messaging
        val sender = Person.Builder().setName(title).setKey(address).build()
        val user = Person.Builder().setName("CallHS").setKey("callhs-self").build()
        val openIntent = PendingIntent.getActivity(
            context,
            notificationId(threadId),
            Intent(context, MainActivity::class.java).apply {
                action = MessagingIntentParser.ACTION_OPEN_CONVERSATION
                putExtra(MessagingIntentParser.EXTRA_THREAD_ID, threadId)
                putExtra(MessagingIntentParser.EXTRA_ADDRESS, address)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val replyIntent = PendingIntent.getBroadcast(
            context,
            notificationId(threadId) xor 0x20000000,
            Intent(context, NotificationReplyReceiver::class.java).apply {
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(EXTRA_ADDRESS, address)
                subscriptionId?.let { putExtra(EXTRA_SUB_ID, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val replyInput = RemoteInput.Builder(REMOTE_INPUT_KEY).setLabel(s.notificationReply).build()
        val markReadIntent = PendingIntent.getBroadcast(
            context,
            notificationId(threadId) xor 0x10000000,
            Intent(context, MarkConversationReadReceiver::class.java).putExtra(EXTRA_THREAD_ID, threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(s.notificationNewMessage)
            .setContentText(title)
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setColor(Color.rgb(52, 168, 83))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.MessagingStyle(user).addMessage(body, timestamp, sender))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setContentIntent(openIntent)
            .addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_notification_message, s.notificationReply, replyIntent)
                    .addRemoteInput(replyInput)
                    .setAllowGeneratedReplies(true)
                    .build()
            )
            .addAction(R.drawable.ic_notification_message, s.notificationMarkRead, markReadIntent)
            .build()
        post(context, notificationId(threadId), notification)
    }

    fun notifyMmsUnsupported(context: Context) {
        ensureChannel(context)
        if (!canPost(context)) return
        val s = LanguageSettings.stringsFor(context).messaging
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setColor(Color.rgb(232, 113, 10))
            .setContentTitle(s.mmsUnsupportedTitle)
            .setContentText(s.mmsUnsupportedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(s.mmsUnsupportedBody))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        post(context, MMS_WARNING_ID, notification)
    }

    fun notifyReplyFailed(context: Context, threadId: Long, address: String) {
        ensureChannel(context)
        if (!canPost(context)) return
        val s = LanguageSettings.stringsFor(context).messaging
        val openIntent = PendingIntent.getActivity(
            context,
            notificationId(threadId),
            Intent(context, MainActivity::class.java).apply {
                action = MessagingIntentParser.ACTION_OPEN_CONVERSATION
                putExtra(MessagingIntentParser.EXTRA_THREAD_ID, threadId)
                putExtra(MessagingIntentParser.EXTRA_ADDRESS, address)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setColor(Color.rgb(211, 47, 47))
            .setContentTitle(s.directReplyFailed)
            .setContentText(address)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        post(context, notificationId(threadId), notification)
    }

    fun cancelThread(context: Context, threadId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(threadId))
    }

    private fun canPost(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        // Quyền có thể bị thu hồi giữa check và notify; notification không được làm crash receiver SMS.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun notificationId(threadId: Long): Int = (threadId xor (threadId ushr 32)).toInt() and 0x3fffffff

    private const val MMS_WARNING_ID = 0x4348534D
}
