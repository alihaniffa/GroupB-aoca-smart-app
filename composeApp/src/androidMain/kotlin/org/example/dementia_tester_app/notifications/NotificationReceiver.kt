package org.example.dementia_tester_app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.example.dementia_tester_app.R

/**
 * BroadcastReceiver for handling notification alarms.
 * This receiver is triggered by AlarmManager to display notifications.
 */
class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_KEY = "notification_id"
        const val NOTIFICATION_MESSAGE_KEY = "notification_message"
        const val EXPECTED_TIME_KEY = "notification_expected_time"
        const val CHANNEL_ID = "health"
        const val CHANNEL_NAME = "Health"
        const val CHANNEL_DESCRIPTION = "Health reminders and alerts"
        const val ACTION_SHOW_NOTIFICATION =
            "com.aoca.dementiatester.SHOW_NOTIFICATION"

        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "receiver fired action=${intent.action}")

        if (intent.action != ACTION_SHOW_NOTIFICATION) {
            Log.e(TAG, "unexpected action")
            return
        }

        val notificationId =
            intent.getStringExtra(NOTIFICATION_ID_KEY) ?: run {
                Log.e(TAG, "missing id")
                return
            }

        val message =
            intent.getStringExtra(NOTIFICATION_MESSAGE_KEY) ?: run {
                Log.e(TAG, "missing message")
                return
            }

        val expectedTime =
            intent.getLongExtra(EXPECTED_TIME_KEY, -1L)

        val now = System.currentTimeMillis()

        Log.i(
            TAG,
            "receiver id=$notificationId at=$now expected=$expectedTime"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            if (
                notificationManager.getNotificationChannel(CHANNEL_ID) == null
            ) {
                createNotificationChannel(context)
                Log.i(TAG, "channel created (fallback)")
            }
        }

        try {
            val launchIntent =
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)

            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    notificationId.hashCode(),
                    launchIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )

            val defaultSoundUri =
                RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION
                )

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            val builder =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Dementia Tester")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                    )
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(
                        longArrayOf(
                            0,
                            500,
                            250,
                            500
                        )
                    )
                    .setLights(
                        0xFF0000FF.toInt(),
                        1000,
                        500
                    )

            notificationManager.notify(
                notificationId.hashCode(),
                builder.build()
            )

        } catch (_: Exception) {
            Log.e(TAG, "notify error")
        }
    }

    private fun createNotificationChannel(
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val importance =
                    NotificationManager.IMPORTANCE_HIGH

                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        importance
                    ).apply {
                        description =
                            CHANNEL_DESCRIPTION

                        lockscreenVisibility =
                            NotificationCompat.VISIBILITY_PUBLIC

                        enableLights(true)

                        lightColor =
                            0xFF0000FF.toInt()

                        enableVibration(true)

                        vibrationPattern =
                            longArrayOf(
                                0,
                                500,
                                250,
                                500
                            )

                        val soundUri =
                            RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_NOTIFICATION
                            )

                        setSound(
                            soundUri,
                            android.media.AudioAttributes
                                .Builder()
                                .setUsage(
                                    android.media.AudioAttributes
                                        .USAGE_NOTIFICATION
                                )
                                .setContentType(
                                    android.media.AudioAttributes
                                        .CONTENT_TYPE_SONIFICATION
                                )
                                .build()
                        )

                        setShowBadge(true)

                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
                        ) {
                            setAllowBubbles(true)
                        }
                    }

                val notificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                notificationManager
                    .createNotificationChannel(channel)

            } catch (_: Exception) {
                Log.e(TAG, "channel create error")
            }
        }
    }
}