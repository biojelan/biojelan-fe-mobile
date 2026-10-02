package id.biojelan.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import id.biojelan.app.App

class MainActivity : ComponentActivity() {
    // Hasilnya tidak perlu ditangani: kalau ditolak, notifikasi sistem tidak tampil dan app tetap jalan normal.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        AppForeground.visible = true
    }

    override fun onStop() {
        AppForeground.visible = false
        super.onStop()
    }

    /** Android 13+ mewajibkan izin runtime; di bawahnya notifikasi otomatis diizinkan. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
