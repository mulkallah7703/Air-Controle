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
import com.mulkallah.aircontrole.core.model.GestureAction
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import com.mulkallah.aircontrole.core.model.PipelineStatus
import com.mulkallah.aircontrole.core.permissions.EnabledAccessibilityServices
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
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
import kotlinx.coroutines.withTimeoutOrNull

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
        isStarted = true
        AirControleLog.i("service onCreate pid=${android.os.Process.myPid()}")
        prefs = AirControlePreferences(applicationContext)
        overlay = CursorOverlayController(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            AirControleConstants.ACTION_STOP, AirControleConstants.ACTION_KILL_SWITCH -> {
                AirControleLog.i("service stop action=${intent.action}")
                scope.launch { prefs.setAirControlEnabled(false) }
                shutdownAndStop()
                return START_NOT_STICKY
            }
        }
        if (!startInForeground()) {
            AirControlBridge.updateStatus(PipelineStatus.SERVICE_START_FAILED)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        val accessibilitySetting = PermissionChecker.enabledAccessibilityServicesSetting(this)
        AirControleLog.i(
            "service onStartCommand action=${intent?.action} " +
                "accessibilitySetting=${accessibilitySetting ?: "null"} " +
                "accessibilityReady=${EnabledAccessibilityServices.isAirControleEnabled(accessibilitySetting)} " +
                "accessibilityConnected=${AirControlBridge.accessibilityConnected}",
        )
        startJob?.cancel()
        startJob = scope.launch { startPipeline() }
        return START_STICKY
    }

    override fun onDestroy() {
        AirControleLog.i("service onDestroy")
        isStarted = false
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
        val size = withTimeoutOrNull(400L) { prefs.cursorSize.first() } ?: 1f
        val pulse = withTimeoutOrNull(400L) { prefs.cursorPulse.first() } ?: true
        AirControleLog.i("pipeline starting overlay=$overlay cursorSize=$size")
        val overlayShown = overlay?.show(size, pulse) == true
        if (!overlayShown) {
            AirControleLog.e("overlay failed to attach")
            AirControlBridge.updateStatus(PipelineStatus.OVERLAY_FAILED)
        }
        AirControlBridge.setRunning(true)
        if (overlayShown) {
            AirControlBridge.updateStatus(PipelineStatus.RUNNING)
        }
        val camera = createTracker()
        tracker = camera
        var lastError: Throwable? = null
        repeat(START_ATTEMPTS) { attempt ->
            try {
                AirControleLog.i("camera.start attempt=${attempt + 1}/$START_ATTEMPTS")
                camera.start()
                if (camera.isBound) {
                    watchForFrames(camera)
                    return
                }
                lastError = IllegalStateException(PipelineStatus.CAMERA_START_FAILED)
                AirControleLog.e("camera.start attempt=${attempt + 1} finished unbound")
            } catch (error: Throwable) {
                lastError = error
                AirControleLog.e(
                    "camera.start attempt=${attempt + 1} threw ${error.javaClass.name}: ${error.message}",
                    error,
                )
            }
            if (attempt < START_ATTEMPTS - 1) {
                val backoffMs = 400L * (attempt + 1)
                AirControleLog.i("camera.start retry in ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        val token = PipelineStatus.fromThrowable(
            lastError ?: IllegalStateException(PipelineStatus.CAMERA_START_FAILED),
        )
        AirControleLog.e("camera pipeline gave up token=$token message=${lastError?.message}")
        AirControlBridge.updateStatus(token)
    }

    private fun createTracker(): CameraHandTracker {
        return CameraHandTracker(
            context = applicationContext,
            onHand = { frame ->
                markPipelineHealthy()
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
                markPipelineHealthy()
                val result = machine.onLostHand(timestamp)
                publishFrame(result.cursor, result.state.name, result.pose.name)
            },
            onError = { error ->
                val token = PipelineStatus.fromThrowable(error)
                AirControleLog.e(
                    "pipeline error token=$token ${error.javaClass.name}: ${error.message}",
                    error,
                )
                AirControlBridge.updateStatus(token)
            },
            onCameraBound = {
                if (!PipelineStatus.isError(AirControlBridge.statusMessage.value)) {
                    AirControlBridge.updateStatus(PipelineStatus.CAMERA_BOUND)
                }
                AirControleLog.i("pipeline camera bound landmarker ready")
            },
        )
    }

    private fun markPipelineHealthy() {
        val status = AirControlBridge.statusMessage.value
        if (status == PipelineStatus.OVERLAY_FAILED) return
        if (PipelineStatus.isError(status) || status.isEmpty() || status == PipelineStatus.RUNNING) {
            if (PipelineStatus.isError(status)) {
                AirControleLog.i("pipeline recovered from $status — frames flowing")
            }
            AirControlBridge.updateStatus(PipelineStatus.CAMERA_BOUND)
        }
    }

    private suspend fun watchForFrames(camera: CameraHandTracker) {
        delay(2_000L)
        if (!AirControlBridge.running.value) return
        if (camera.deliveredFrameCount > 0) {
            AirControleLog.i(
                "watchdog early ok frames=${camera.deliveredFrameCount} hands=${camera.deliveredHandCount}",
            )
            return
        }
        AirControleLog.w(
            "watchdog: no MediaPipe results after 2s analyzed=${camera.analyzedFrameCount} bound=${camera.isBound}",
        )
        if (camera.analyzedFrameCount == 0 || !camera.isBound) {
            try {
                AirControleLog.i("watchdog rebinding camera")
                camera.rebind()
            } catch (error: Throwable) {
                AirControleLog.e("watchdog rebind threw ${error.javaClass.name}: ${error.message}", error)
            }
        }
        delay(3_000L)
        if (!AirControlBridge.running.value) return
        if (camera.deliveredFrameCount > 0) {
            AirControleLog.i(
                "watchdog ok frames=${camera.deliveredFrameCount} hands=${camera.deliveredHandCount} " +
                    "analyzed=${camera.analyzedFrameCount} state=${AirControlBridge.machineState.value}",
            )
            return
        }
        if (PipelineStatus.isError(AirControlBridge.statusMessage.value)) return
        AirControleLog.e(
            "no camera frames after retry analyzed=${camera.analyzedFrameCount} bound=${camera.isBound} " +
                "— Samsung battery / camera FGS likely blocked",
        )
        AirControlBridge.updateStatus(PipelineStatus.CAMERA_NO_FRAMES)
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
        val settingOn = PermissionChecker.hasAccessibility(this)
        val needsAccessibility = action != GestureAction.PAUSE &&
            action != GestureAction.MOVE_CURSOR &&
            action != GestureAction.NONE
        if (needsAccessibility && (!settingOn || sink == null)) {
            AirControleLog.w(
                "action blocked gesture=${gesture.name} action=${action.name} " +
                    "accessibilityReady=$settingOn connected=${sink?.connected == true} — " +
                    "enable AirControleAccessibilityService in system settings",
            )
            return
        }
        val cursor = AirControlBridge.cursor.value
        val ok = GestureActionRouter.dispatch(
            action = action,
            sink = sink,
            cursor = cursor,
            onPause = { AirControlBridge.togglePaused() },
        )
        AirControleLog.i(
            "action dispatched gesture=${gesture.name} action=${action.name} " +
                "accessibilityReady=$settingOn connected=${sink?.connected == true} " +
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

    private fun startInForeground(): Boolean {
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
        return try {
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
            true
        } catch (error: Throwable) {
            AirControleLog.e("startForeground CAMERA failed", error)
            false
        }
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
        private const val START_ATTEMPTS = 2

        @Volatile
        var isStarted: Boolean = false
            private set

        fun start(context: Context): Boolean = AirControlStarter.start(context)

        fun stop(context: Context) {
            AirControleLog.i("service stop requested")
            val intent = Intent(context, AirControlForegroundService::class.java)
                .setAction(AirControleConstants.ACTION_STOP)
            try {
                context.startService(intent)
            } catch (error: Throwable) {
                AirControleLog.e("service stop dispatch failed", error)
            }
        }
    }
}


