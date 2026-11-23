package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.HerbariumApp
import com.example.MainActivity
import com.example.R
import com.example.data.repository.BotanicalLibraryScanSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BotanicalScanStatus(
  val isScanning: Boolean = false,
  val lastSummary: BotanicalLibraryScanSummary? = null,
)

/**
 * Background service that scans accessible device/SAF storage for audio files longer
 * than 90 seconds (> 90,000 ms), imports metadata in parallel batches, and automatically
 * categorizes qualifying tracks into the "Botanical Library" collection.
 */
class BotanicalLibraryScanService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var activeScanJob: Job? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val asForeground = intent?.getBooleanExtra(EXTRA_AS_FOREGROUND, false) == true
    val treeUri = intent?.getStringExtra(EXTRA_TREE_URI)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    val needPickerFallback = intent?.getBooleanExtra(EXTRA_TRIGGERED_MANUALLY, false) == true

    if (asForeground) {
      promoteToForeground()
    }

    if (activeScanJob?.isActive == true) {
      return START_NOT_STICKY
    }

    activeScanJob = serviceScope.launch {
      _scanStatus.value = _scanStatus.value.copy(isScanning = true)
      try {
        val app = application as? HerbariumApp ?: return@launch
        val summary = app.musicRepository.scanAndCategorizeIntoBotanicalLibrary(
          extraTreeUri = treeUri,
          triggeredManually = needPickerFallback,
        )
        _scanStatus.value = BotanicalScanStatus(
          isScanning = false,
          lastSummary = summary,
        )
        _scanCompletedEvents.tryEmit(summary)
      } catch (_: Exception) {
        _scanStatus.value = _scanStatus.value.copy(isScanning = false)
      } finally {
        if (asForeground) {
          runCatching {
            ServiceCompat.stopForeground(this@BotanicalLibraryScanService, ServiceCompat.STOP_FOREGROUND_REMOVE)
          }
        }
        stopSelf(startId)
      }
    }

    return START_NOT_STICKY
  }

  private fun promoteToForeground() {
    ensureNotificationChannel()
    val openIntent = PendingIntent.getActivity(
      this,
      102,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.app_name))
      .setContentText(getString(R.string.scan_service_notification_text))
      .setOngoing(true)
      .setSilent(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(openIntent)
      .build()

    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    }
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(NotificationManager::class.java) ?: return
      if (nm.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          getString(R.string.scan_channel_name),
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
      }
    }
  }

  override fun onDestroy() {
    activeScanJob?.cancel()
    serviceScope.cancel()
    _scanStatus.value = _scanStatus.value.copy(isScanning = false)
    super.onDestroy()
  }

  companion object {
    const val ACTION_SCAN_STORAGE = "com.example.action.SCAN_BOTANICAL_LIBRARY"
    const val EXTRA_TREE_URI = "extra_tree_uri"
    const val EXTRA_TRIGGERED_MANUALLY = "extra_triggered_manually"
    const val EXTRA_AS_FOREGROUND = "extra_as_foreground"

    private const val CHANNEL_ID = "botanical_library_scan"
    private const val NOTIFICATION_ID = 4207

    private val _scanStatus = MutableStateFlow(BotanicalScanStatus())
    val scanStatus: StateFlow<BotanicalScanStatus> = _scanStatus.asStateFlow()

    private val _scanCompletedEvents = MutableSharedFlow<BotanicalLibraryScanSummary>(extraBufferCapacity = 8)
    val scanCompletedEvents: SharedFlow<BotanicalLibraryScanSummary> = _scanCompletedEvents.asSharedFlow()

    fun startScan(
      context: Context,
      treeUri: Uri? = null,
      triggeredManually: Boolean = false,
      asForeground: Boolean = false,
    ) {
      val intent = Intent(context, BotanicalLibraryScanService::class.java).apply {
        action = ACTION_SCAN_STORAGE
        putExtra(EXTRA_TREE_URI, treeUri?.toString())
        putExtra(EXTRA_TRIGGERED_MANUALLY, triggeredManually)
        putExtra(EXTRA_AS_FOREGROUND, asForeground)
      }
      runCatching {
        if (asForeground) {
          androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } else {
          context.startService(intent)
        }
      }
    }
  }
}
