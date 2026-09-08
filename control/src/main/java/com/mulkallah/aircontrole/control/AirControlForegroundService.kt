package com.mulkallah.aircontrole.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.mulkallah.aircontrole.camera.CameraHandTracker
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.gestures.GestureActionRouter
import com.mulkallah.aircontrole.core.model.CursorPosition
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.gestures.GestureState
import com.mulkallah.aircontrole.gestures.GestureStateMachine
import com.mulkallah.aircontrole.gestures.GestureType
import com.mulkallah.aircontrole.overlay.CursorOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AirControlForegroundService : LifecycleService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val machine = GestureStateMachine()
    private lateinit var prefs: AirControlePreferences
    private var overlay: CursorOverlayController? = null
    private var tracker: CameraHandTracker? = null
    private var startJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        AirControleLog.i("service onCreate")
        prefs = AirControlePreferences(applicationContext)
        overlay = CursorOverlayController(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        AirControleLog.i("service start action=${intent?.action} accessibility=${AirControlBridge.accessibilityConnected}")
        when (intent?.action) {
            AirControleConstants.ACTION_STOP, AirControleConstants.ACTION_KILL_SWITCH -> {
                scope.launch { prefs.setAirControlEnabled(false) }
                shutdownAndStop()
                return START_NOT_STICKY
            }
        }
        startInForeground()
        acquireWakeLock()
        startJob?.cancel()
        startJob = scope.launch { startPipeline() }
        return START_STICKY
    }

    override fun onDestroy() {
        AirControleLog.i("service onDestroy")
        shutdownPipeline()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun startPipeline() {
        tracker?.stop()
        tracker = null
        machine.reset()
        AirControlBridge.resetSessionFlags()
        if (overlay == null) {
            overlay = CursorOverlayController(applicationContext)
        }
        val size = prefs.cursorSize.first()
        val pulse = prefs.cursorPulse.first()
        val overlayShown = overlay?.show(size, pulse) == true
        if (!overlayShown) {
            AirControleLog.e("overlay failed to attach")
            AirControlBridge.updateStatus(PipelineStatus.OVERLAY_FAILED)
        }
        AirControlBridge.setRunning(true)
        if (overlayShown) {
            AirControlBridge.updateStatus(PipelineStatus.RUNNING)
        }
        val camera = CameraHandTracker(
            context = applicationContext,
            lifecycleOwner = this,
            onHand = { frame ->
                if (AirControlBridge.paused.value && machine.state != GestureState.COOLDOWN) {
                    val result = machine.onFrame(frame)
                    publishFrame(result.cursor, result.state.name, result.pose.name)
                    if (result.shouldDispatch && result.recognized == GestureType.PALM_PAUSE) {
                        AirControlBridge.updateLastGesture(result.recognized.name)
                        if (!AirControlBridge.suppressActions.value) {
                            AirControlBridge.togglePaused()
                            overlay?.pulse("pause")
                            AirControleLog.i("pause toggled paused=${AirControlBridge.paused.value}")
                        }
                    }
                    return@CameraHandTracker
                }
                val result = machine.onFrame(frame)
                publishFrame(result.cursor, result.state.name, result.pose.name)
                if (result.sessionStart) {
                    overlay?.pulse(getString(R.string.overlay_tracking))
                    AirControlBridge.updateLastGesture(GestureMappingCatalog.HAND_START)
                    AirControleLog.i("session start state=${result.state} cursorVisible=${result.cursor.visible}")
                } else if (result.pulse) {
                    overlay?.pulse(result.recognized.name.lowercase())
                    if (result.recognized != GestureType.NONE) {
                        AirControlBridge.updateLastGesture(result.recognized.name)
                    }
                }
                if (result.shouldDispatch) {
                    dispatch(result.recognized)
                }
            },
            onEmpty = { timestamp ->
                val result = machine.onLostHand(timestamp)
                publishFrame(result.cursor, result.state.name, result.pose.name)
            },
            onError = { error ->
                val token = pipelineErrorToken(error)
                AirControleLog.e("pipeline error token=$token message=${error.message}", error)
                AirControlBridge.updateStatus(token)
            },
            onCameraBound = {
                if (!PipelineStatus.isError(AirControlBridge.statusMessage.value)) {
                    AirControlBridge.updateStatus(PipelineStatus.CAMERA_BOUND)
                }
                AirControleLog.i("pipeline camera bound landmarker ready")
            },
        )
        tracker = camera
        try {
            camera.start()
            watchForFrames(camera)
        } catch (error: Throwable) {
            AirControleLog.e("camera.start threw", error)
            AirControlBridge.updateStatus(pipelineErrorToken(error))
        }
    }

    private suspend fun watchForFrames(camera: CameraHandTracker) {
        delay(4_000L)
        if (!AirControlBridge.running.value) return
        if (PipelineStatus.isError(AirControlBridge.statusMessage.value)) return
        if (camera.deliveredFrameCount == 0) {
            AirControleLog.e("no camera frames after 4s — Samsung battery / camera FGS likely blocked")
            AirControlBridge.updateStatus(PipelineStatus.CAMERA_NO_FRAMES)
        } else {
            AirControleLog.i(
                "watchdog ok frames=${camera.deliveredFrameCount} hands=${camera.deliveredHandCount} " +
                    "state=${AirControlBridge.machineState.value}",
            )
        }
    }

    private fun publishFrame(
        cursor: CursorPosition,
        state: String,
        pose: String,
    ) {
        AirControlBridge.updateCursor(cursor)
        if (AirControlBridge.machineState.value != state) {
            AirControleLog.i("state ${AirControlBridge.machineState.value} -> $state pose=$pose cursorVisible=${cursor.visible}")
        }
        AirControlBridge.updateMachineState(state)
        AirControlBridge.updatePose(pose)
        overlay?.update(cursor)
    }

    private fun dispatch(gesture: GestureType) {
        if (AirControlBridge.suppressActions.value) {
            AirControleLog.w("action suppressed gesture=${gesture.name}")
            return
        }
        val action = GestureMappingCatalog.actionFor(gesture.name)
        val sink = AirControlBridge.actionSink
        val cursor = AirControlBridge.cursor.value
        val ok = GestureActionRouter.dispatch(
            action = action,
            sink = sink,
            cursor = cursor,
            onPause = { AirControlBridge.togglePaused() },
        )
        AirControleLog.i(
            "action dispatched gesture=${gesture.name} action=${action.name} " +
                "accessibility=${sink != null} connected=${sink?.connected == true} " +
                "result=$ok cursor=${"%.2f".format(cursor.x)},${"%.2f".format(cursor.y)}",
        )
    }

    private fun shutdownPipeline() {
        startJob?.cancel()
        startJob = null
        tracker?.stop()
        tracker = null
        overlay?.hide()
        overlay = null
        machine.reset()
        releaseWakeLock()
        AirControlBridge.setRunning(false)
        AirControlBridge.updateStatus(PipelineStatus.STOPPED)
        AirControleLog.i("pipeline stopped")
    }

    private fun shutdownAndStop() {
        shutdownPipeline()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AirControle:camera").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 60 * 1000L)
        }
        AirControleLog.i("wake lock acquired")
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                AirControleLog.i("wake lock released")
            }
        } catch (error: Throwable) {
            AirControleLog.w("wake lock release failed", error)
        }
        wakeLock = null
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
        AirControleLog.i("foreground started type=camera")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            AirControleConstants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.air_control_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.air_control_notification_text)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        fun start(context: Context) {
            AirControleLog.i("service start requested")
            val intent = Intent(context, AirControlForegroundService::class.java)
                .setAction(AirControleConstants.ACTION_START)
            ContextCompatStart.start(context, intent)
        }

        fun stop(context: Context) {
            AirControleLog.i("service stop requested")
            val intent = Intent(context, AirControlForegroundService::class.java)
                .setAction(AirControleConstants.ACTION_STOP)
            context.startService(intent)
        }
    }
}

private fun pipelineErrorToken(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        message == PipelineStatus.MODEL_MISSING || message.contains("hand_landmarker", ignoreCase = true) ->
            PipelineStatus.MODEL_MISSING
        message == PipelineStatus.CAMERA_PERMISSION -> PipelineStatus.CAMERA_PERMISSION
        message == PipelineStatus.LANDMARKER_FAILED -> PipelineStatus.LANDMARKER_FAILED
        message.contains("landmarker", ignoreCase = true) -> PipelineStatus.LANDMARKER_FAILED
        message.contains("camera", ignoreCase = true) -> PipelineStatus.CAMERA_START_FAILED
        else -> PipelineStatus.CAMERA_ERROR
    }
}

private object ContextCompatStart {
    fun start(context: Context, intent: Intent) {
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }
}
