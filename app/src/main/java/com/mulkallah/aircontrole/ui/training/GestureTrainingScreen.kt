package com.mulkallah.aircontrole.ui.training

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mulkallah.aircontrole.R
import com.mulkallah.aircontrole.camera.CameraHandTracker
import com.mulkallah.aircontrole.core.bridge.AirControlBridge
import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import com.mulkallah.aircontrole.core.model.GestureTraining
import com.mulkallah.aircontrole.core.permissions.AccessibilitySettingsLauncher
import com.mulkallah.aircontrole.core.permissions.PermissionChecker
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import com.mulkallah.aircontrole.gestures.GestureState
import com.mulkallah.aircontrole.gestures.GestureStateMachine
import com.mulkallah.aircontrole.gestures.GestureType
import com.mulkallah.aircontrole.gestures.HandPose
import com.mulkallah.aircontrole.ui.gestures.gestureHowRes
import com.mulkallah.aircontrole.ui.gestures.gestureTitleRes
import com.mulkallah.aircontrole.ui.gestures.machineStateRes
import com.mulkallah.aircontrole.ui.theme.AirCyan
import com.mulkallah.aircontrole.ui.theme.AirNavy
import com.mulkallah.aircontrole.ui.theme.AirOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureTrainingScreen(
    preferences: AirControlePreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trained by preferences.trainedGestures.collectAsState(initial = emptySet())
    val running by AirControlBridge.running.collectAsState()
    val recognitionSeq by AirControlBridge.recognitionSeq.collectAsState()
    val lastGesture by AirControlBridge.lastGesture.collectAsState()
    val machineState by AirControlBridge.machineState.collectAsState()
    var permissions by remember { mutableStateOf(PermissionChecker.snapshot(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = PermissionChecker.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val counts = remember { mutableStateMapOf<String, Int>() }
    var activeKey by remember {
        mutableStateOf(GestureTraining.nextIncomplete(emptySet()) ?: GestureMappingCatalog.CLICK)
    }
    var localState by remember { mutableStateOf(GestureState.IDLE.name) }
    var lastFlash by remember { mutableStateOf<String?>(null) }
    val trainedNow by rememberUpdatedState(trained)
    val startedSeq = remember { AirControlBridge.recognitionSeq.value }

    LaunchedEffect(trained) {
        val next = GestureTraining.nextIncomplete(trained)
        if (next != null && activeKey in trained) {
            activeKey = next
        }
    }

    DisposableEffect(Unit) {
        AirControlBridge.setSuppressActions(true)
        onDispose { AirControlBridge.setSuppressActions(false) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissions = PermissionChecker.snapshot(context) }

    fun onDetected(gestureKey: String) {
        if (gestureKey !in GestureMappingCatalog.gestureKeys) return
        scope.launch(Dispatchers.Main.immediate) {
            if (gestureKey in trainedNow) return@launch
            val nextCount = (counts[gestureKey] ?: 0) + 1
            counts[gestureKey] = nextCount
            lastFlash = gestureKey
            if (GestureTraining.isComplete(nextCount)) {
                preferences.markGestureTrained(gestureKey)
            }
        }
    }

    LaunchedEffect(recognitionSeq, lastGesture, running) {
        if (!running) return@LaunchedEffect
        val gesture = lastGesture ?: return@LaunchedEffect
        if (recognitionSeq > startedSeq) onDetected(gesture)
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var holdStarted = 0L
        while (true) {
            val now = System.currentTimeMillis()
            val tracking = AirControlBridge.machineState.value == GestureState.TRACKING.name
            val pointing = AirControlBridge.pose.value == HandPose.POINT.name
            if (tracking && pointing) {
                if (holdStarted == 0L) holdStarted = now
                if (now - holdStarted >= GestureTraining.POINT_HOLD_MS) {
                    onDetected(GestureMappingCatalog.POINT_MOVE)
                    holdStarted = now + GestureTraining.POINT_REARM_MS
                }
            } else {
                holdStarted = 0L
            }
            delay(80L)
        }
    }

    LaunchedEffect(permissions.camera, running) {
        if (!permissions.camera || running) {
            localState = GestureState.IDLE.name
            return@LaunchedEffect
        }
        val machine = GestureStateMachine()
        var localPointHold = 0L
        val tracker = CameraHandTracker(
            context = context.applicationContext,
            onHand = { frame ->
                val result = machine.onFrame(frame)
                scope.launch(Dispatchers.Main.immediate) { localState = result.state.name }
                if (result.shouldDispatch && result.recognized != GestureType.NONE) {
                    onDetected(result.recognized.name)
                }
                if (result.state == GestureState.TRACKING && result.pose == HandPose.POINT) {
                    if (localPointHold == 0L) localPointHold = frame.timestampMs
                    if (frame.timestampMs - localPointHold >= GestureTraining.POINT_HOLD_MS) {
                        onDetected(GestureMappingCatalog.POINT_MOVE)
                        localPointHold = frame.timestampMs + GestureTraining.POINT_REARM_MS
                    }
                } else if (result.pose != HandPose.POINT) {
                    localPointHold = 0L
                }
            },
            onEmpty = { timestamp ->
                val result = machine.onLostHand(timestamp)
                scope.launch(Dispatchers.Main.immediate) { localState = result.state.name }
            },
            onError = {
                localState = "IDLE"
            },
        )
        try {
            tracker.start()
            awaitCancellation()
        } finally {
            tracker.stop()
        }
    }

    val total = GestureMappingCatalog.gestureKeys.size
    val done = trained.size
    val watching = permissions.camera
    val statusLabel = if (running) machineState else localState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirNavy),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.training_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AirNavy),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.training_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.training_progress, done, total),
                style = MaterialTheme.typography.titleMedium,
                color = AirCyan,
            )
            LinearProgressIndicator(
                progress = if (total == 0) 0f else done / total.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!permissions.camera) {
                CtaCard(
                    title = stringResource(R.string.training_need_camera_title),
                    body = stringResource(R.string.training_need_camera_body),
                    action = stringResource(R.string.training_allow_camera),
                    onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                )
            }
            if (!permissions.accessibility) {
                CtaCard(
                    title = stringResource(R.string.training_need_accessibility_title),
                    body = stringResource(R.string.training_need_accessibility_body),
                    action = stringResource(R.string.training_open_accessibility),
                    onClick = { AccessibilitySettingsLauncher.open(context) },
                )
            }
            if (permissions.camera) {
                Text(
                    text = stringResource(R.string.training_air_control_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val complete = done == total
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (complete) {
                        Text(stringResource(R.string.training_done_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.training_done_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(stringResource(gestureTitleRes(activeKey)), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(gestureHowRes(activeKey)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (watching) {
                            Text(
                                text = stringResource(R.string.training_watching),
                                color = AirCyan,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${stringResource(R.string.home_status)} · ${stringResource(machineStateRes(statusLabel))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val count = if (activeKey in trained) {
                            GestureTraining.REQUIRED_DETECTIONS
                        } else {
                            counts[activeKey] ?: 0
                        }
                        Text(
                            text = stringResource(
                                R.string.training_detections,
                                count.coerceAtMost(GestureTraining.REQUIRED_DETECTIONS),
                                GestureTraining.REQUIRED_DETECTIONS,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (lastFlash == activeKey) {
                            Text(
                                text = stringResource(R.string.training_detected),
                                color = AirOk,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            Text(stringResource(R.string.training_checklist), style = MaterialTheme.typography.titleLarge)
            GestureMappingCatalog.gestureKeys.forEach { key ->
                val practiced = key in trained
                val count = if (practiced) GestureTraining.REQUIRED_DETECTIONS else (counts[key] ?: 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeKey = key }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (practiced) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (practiced) AirOk else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(gestureTitleRes(key)), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(
                                R.string.training_detections,
                                count.coerceAtMost(GestureTraining.REQUIRED_DETECTIONS),
                                GestureTraining.REQUIRED_DETECTIONS,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (trained.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        counts.clear()
                        lastFlash = null
                        scope.launch {
                            preferences.resetTraining()
                            activeKey = GestureMappingCatalog.CLICK
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.training_reset))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CtaCard(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(action)
            }
        }
    }
}
