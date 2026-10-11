package dev.nezhar.lookabout

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** The Intent extra on a tap on the walk notice: MainActivity opens the walk, and the camera when a photo is due. */
const val EXTRA_FROM_WALK_NOTICE = "dev.nezhar.lookabout.FROM_WALK_NOTICE"

/** True when Android 13 or later still needs to ask for permission to post notifications. */
fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/**
 * Posts the walk notice: an ongoing, silent notification that shows on the lock screen, on its
 * own low-importance channel. No foreground service: it is a plain notification, posted only when
 * notifications are allowed.
 */
class WalkNotifier(context: Context) {
    private val context = context.applicationContext
    private val manager = NotificationManagerCompat.from(this.context)

    /** Posts or updates the notice, or removes it when [notice] is null. Posts nothing without permission. */
    @SuppressLint("MissingPermission") // Checked by needsNotificationPermission just below.
    fun show(notice: WalkNotice?) {
        if (notice == null) {
            manager.cancel(NOTICE_ID)
            return
        }
        if (needsNotificationPermission(context) || !manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Walk in progress")
                .setDescription("The picture to find next, on the lock screen during a walk")
                .setShowBadge(false)
                .build(),
        )
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_FROM_WALK_NOTICE, true)
            // One MainActivity: an open one gets the tap through onNewIntent.
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notice)
            // The position as the title; the picture text, which can be long, in full when expanded.
            .setContentTitle(notice.position)
            .setContentText(notice.picture)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.picture))
            .setContentIntent(tap)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            manager.notify(NOTICE_ID, notification)
        } catch (e: SecurityException) {
            // The permission was withdrawn between the check and the post.
            Log.w(Gemma.TAG, "Could not post the walk notice", e)
        }
    }

    private companion object {
        const val CHANNEL_ID = "walk"
        const val NOTICE_ID = 1
    }
}
