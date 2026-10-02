package id.biojelan.mobile

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import id.biojelan.app.data.local.SystemNotifier

/**
 * Menampilkan notifikasi di bar sistem. Hanya tampil bila izin POST_NOTIFICATIONS diberikan (Android 13+)
 * dan app sedang tidak di depan. Ini notifikasi lokal: muncul selama proses app masih hidup saat polling
 * menemukan data baru. Untuk notifikasi saat app benar-benar mati dibutuhkan push (FCM) dari backend.
 */
class AndroidSystemNotifier(private val app: Application) : SystemNotifier {
    private var nextId = 1000

    override fun show(text: String) {
        if (AppForeground.visible) return
        if (Build.VERSION.SDK_INT >= 33 &&
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Notifikasi transaksi", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        val open = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(app, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(app)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(app.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(nextId++, notification)
    }

    private companion object {
        const val CHANNEL_ID = "transaksi"
    }
}
