package com.example.audio

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.example.HerbariumApp
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
import java.util.concurrent.atomic.AtomicBoolean

data class BotanicalScanStatus(
  val isScanning: Boolean = false,
  val lastSummary: BotanicalLibraryScanSummary? = null,
)

/**
 * Coordinates MediaStore and SAF folder scanning for audio files exceeding the configured
 * duration threshold, importing metadata in parallel batches and categorizing qualifying
 * tracks into the "Botanical Library" collection without requiring FOREGROUND_SERVICE_DATA_SYNC.
 */
class BotanicalLibraryScanService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var activeScanJob: Job? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val treeUri = intent?.getStringExtra(EXTRA_TREE_URI)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    val triggeredManually = intent?.getBooleanExtra(EXTRA_TRIGGERED_MANUALLY, false) == true

    if (activeScanJob?.isActive == true || isScanRunning.get()) {
      return START_NOT_STICKY
    }

    activeScanJob = serviceScope.launch {
      try {
        val app = application as? HerbariumApp ?: return@launch
        executeScan(app, treeUri, triggeredManually)
      } finally {
        stopSelf(startId)
      }
    }

    return START_NOT_STICKY
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

    private val isScanRunning = AtomicBoolean(false)

    private val _scanStatus = MutableStateFlow(BotanicalScanStatus())
    val scanStatus: StateFlow<BotanicalScanStatus> = _scanStatus.asStateFlow()

    private val _scanCompletedEvents = MutableSharedFlow<BotanicalLibraryScanSummary>(extraBufferCapacity = 8)
    val scanCompletedEvents: SharedFlow<BotanicalLibraryScanSummary> = _scanCompletedEvents.asSharedFlow()

    fun startScan(
      context: Context,
      treeUri: Uri? = null,
      triggeredManually: Boolean = false,
      @Suppress("UNUSED_PARAMETER") asForeground: Boolean = false,
    ) {
      val app = context.applicationContext as? HerbariumApp
      if (app != null) {
        if (isScanRunning.get()) return
        app.applicationScope.launch {
          executeScan(app, treeUri, triggeredManually)
        }
        return
      }
      val intent = Intent(context, BotanicalLibraryScanService::class.java).apply {
        action = ACTION_SCAN_STORAGE
        putExtra(EXTRA_TREE_URI, treeUri?.toString())
        putExtra(EXTRA_TRIGGERED_MANUALLY, triggeredManually)
      }
      runCatching {
        context.startService(intent)
      }
    }

    internal suspend fun executeScan(
      app: HerbariumApp,
      treeUri: Uri? = null,
      triggeredManually: Boolean = false,
    ) {
      if (!isScanRunning.compareAndSet(false, true)) return
      _scanStatus.value = _scanStatus.value.copy(isScanning = true)
      try {
        val minDurationMs = app.preferencesRepository.getMinScanDurationMs()
        val summary = app.musicRepository.scanAndCategorizeIntoBotanicalLibrary(
          extraTreeUri = treeUri,
          triggeredManually = triggeredManually,
          minDurationMs = minDurationMs,
        )
        app.preferencesRepository.recordAutoScanCompleted((minDurationMs / 1000L).toInt())
        _scanStatus.value = BotanicalScanStatus(
          isScanning = false,
          lastSummary = summary,
        )
        _scanCompletedEvents.tryEmit(summary)
      } catch (_: Exception) {
        _scanStatus.value = _scanStatus.value.copy(isScanning = false)
      } finally {
        isScanRunning.set(false)
      }
    }
  }
}
