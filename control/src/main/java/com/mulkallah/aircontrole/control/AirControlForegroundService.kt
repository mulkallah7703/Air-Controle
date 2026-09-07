package com.mulkallah.aircontrole.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mulkallah.aircontrole.camera.CameraHandTracker
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.gestures.GestureStateMachine
import com.mulkallah.aircontrole.gestures.GestureType
import com.mulkallah.aircontrole.overlay.CursorOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AirControlForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val machine = GestureStateMachine()
    private lateinit var prefs: AirControlePreferences
    private var overlay: CursorOverlayController? = null
    private var tracker: CameraHandTracker? = null
    private var startJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AirControlePreferences(applicationContext)
        overlay = CursorOverlayController(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AirControleConstants.ACTION_STOP, AirControleConstants.ACTION_KILL_SWITCH -> {
                scope.launch { prefs.setAirControlEnabled(false) }
                shutdownAndStop()
                return START_NOT_STICKY
            }
        }
        startInForeground()
        startJob?.cancel()
        startJob = scope.launch { startPipeline() }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        shutdownPipeline()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun startPipeline() {
        tracker?.stop()
        tracker = null
        if (overlay == null) {
            overlay = CursorOverlayController(applicationContext)
        }
        val size = prefs.cursorSize.first()
        val pulse = prefs.cursorPulse.first()
        overlay?.show(size, pulse)
        AirControlBridge.setRunning(true)
        AirControlBridge.updateStatus("running")
        machine.reset()
        val camera = CameraHandTracker(
            context = applicationContext,
            onHand = { frame ->
                if (AirControlBridge.paused.value && machine.state != com.mulkallah.aircontrole.gestures.GestureState.COOLDOWN) {
                    val result = machine.onFrame(frame)
                    AirControlBridge.updateCursor(result.cursor)
                    AirControlBridge.updateMachineState(result.state.name)
                    if (result.shouldDispatch && result.recognized == GestureType.PALM_PAUSE) {
                        AirControlBridge.togglePaused()
                        overlay?.pulse("pause")
                    }
                    return@CameraHandTracker
                }
                val result = machine.onFrame(frame)
                AirControlBridge.updateCursor(result.cursor)
                AirControlBridge.updateMachineState(result.state.name)
                overlay?.update(result.cursor)
                if (result.pulse) {
                    overlay?.pulse(result.recognized.name.lowercase())
                    AirControlBridge.updateLastGesture(result.recognized.name)
                }
                if (result.shouldDispatch) {
                    dispatch(result.recognized)
                }
            },
            onEmpty = { timestamp ->
                val result = machine.onLostHand(timestamp)
                AirControlBridge.updateMachineState(result.state.name)
                AirControlBridge.updateCursor(result.cursor)
                overlay?.update(result.cursor)
            },
            onError = { error ->
                AirControlBridge.updateStatus(error.message ?: "camera_error")
            },
        )
        tracker = camera
        try {
            camera.start()
        } catch (error: Throwable) {
            AirControlBridge.updateStatus(error.message ?: "camera_start_failed")
        }
    }

    private fun dispatch(gesture: GestureType) {
        val sink = AirControlBridge.actionSink
        val cursor = AirControlBridge.cursor.value
        when (gesture) {
            GestureType.CLICK -> sink?.performClick(cursor.x, cursor.y)
            GestureType.SCROLL_UP -> sink?.performScroll(AirControlBridge.ScrollDirection.UP)
            GestureType.SCROLL_DOWN -> sink?.performScroll(AirControlBridge.ScrollDirection.DOWN)
            GestureType.SWIPE_LEFT -> sink?.performBack()
            GestureType.SWIPE_RIGHT -> sink?.performHome()
            GestureType.FIST_BACK -> sink?.performBack()
            GestureType.PEACE_HOME -> sink?.performHome()
            GestureType.PALM_PAUSE -> AirControlBridge.togglePaused()
            GestureType.POINT_MOVE, GestureType.NONE -> Unit
        }
    }

    private fun shutdownPipeline() {
        tracker?.stop()
        tracker = null
        overlay?.hide()
        overlay = null
        machine.reset()
        AirControlBridge.setRunning(false)
        AirControlBridge.updateStatus("stopped")
    }

    private fun shutdownAndStop() {
        shutdownPipeline()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground() {
        ensureChannel()
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val content = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AirControlForegroundService::class.java).setAction(AirControleConstants.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, AirControleConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(getString(R.string.air_control_notification_title))
            .setContentText(getString(R.string.air_control_notification_text))
            .setContentIntent(content)
            .setOngoing(true)
            .addAction(0, getString(R.string.air_control_notification_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AirControleConstants.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
            )
        } else {
            startForeground(AirControleConstants.NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            AirControleConstants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.air_control_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AirControlForegroundService::class.java)
                .setAction(AirControleConstants.ACTION_START)
            ContextCompatStart.start(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AirControlForegroundService::class.java)
                .setAction(AirControleConstants.ACTION_STOP)
            context.startService(intent)
        }
    }
}

private object ContextCompatStart {
    fun start(context: Context, intent: Intent) {
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }
}
