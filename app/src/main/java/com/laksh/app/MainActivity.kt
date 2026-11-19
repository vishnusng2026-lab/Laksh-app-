@file:OptIn(ExperimentalMaterial3Api::class)

package com.laksh.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LakshTheme {
                LakshApp(refresh = tick.intValue, bump = { tick.intValue++ })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

/* ---------- Theme: soft lavender-grey surfaces, indigo accent, big rounded shapes ---------- */

@Composable
fun LakshTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFB7C4FF),
            onPrimary = Color(0xFF0F2072),
            primaryContainer = Color(0xFF2B3A7C),
            onPrimaryContainer = Color(0xFFDDE1FF),
            background = Color(0xFF121318),
            onBackground = Color(0xFFE4E1E9),
            surface = Color(0xFF121318),
            onSurface = Color(0xFFE4E1E9),
            surfaceVariant = Color(0xFF2A2B33),
            onSurfaceVariant = Color(0xFFC5C6D0),
            surfaceContainer = Color(0xFF1F1F25),
            surfaceContainerHigh = Color(0xFF292A30),
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF3F51A5),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDE1FF),
            onPrimaryContainer = Color(0xFF0B1B5E),
            background = Color(0xFFFBF8FF),
            onBackground = Color(0xFF1B1B21),
            surface = Color(0xFFFBF8FF),
            onSurface = Color(0xFF1B1B21),
            surfaceVariant = Color(0xFFE3E1EC),
            onSurfaceVariant = Color(0xFF46464F),
            surfaceContainer = Color(0xFFEFEDF4),
            surfaceContainerHigh = Color(0xFFE9E7EF),
            error = Color(0xFFBA1A1A),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** A rounded 8-lobe "cookie" outline used for the round buttons and the app badge. */
private val CookieShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy)
        val steps = 160
        for (i in 0..steps) {
            val t = (i * 2.0 * PI / steps).toFloat()
            val rr = r * (0.93f + 0.07f * cos(8f * t))
            val x = cx + rr * cos(t)
            val y = cy + rr * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
private fun CookieButton(
    size: Int = 48,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var m = Modifier
        .size(size.dp)
        .clip(CookieShape)
        .background(container)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PhoneGlyph(size: Int = 22, tint: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Image(
        painterResource(R.drawable.ic_call),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(size.dp),
    )
}

private fun permissionsOk(ctx: Context): Boolean {
    val notif = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val full = Build.VERSION.SDK_INT < 34 ||
        ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    return notif && full
}

/* ---------- Main screen ---------- */

@Composable
fun LakshApp(refresh: Int, bump: () -> Unit) {
    val ctx = LocalContext.current
    var saved by remember { mutableStateOf(Reminders.all(ctx)) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("next") }
    var filter by remember { mutableStateOf("all") }
    var showNew by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var showSetup by remember { mutableStateOf(false) }
    var pendingDeleteAudio by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) { saved = Reminders.all(ctx) }
    LaunchedEffect(Unit) {
        Reminders.cleanupOrphanedAudio(ctx)
        while (true) {
            delay(15_000)
            clock = System.currentTimeMillis()
            saved = Reminders.all(ctx)
        }
    }

    val reload = { saved = Reminders.all(ctx) }
    val ready = remember(refresh) { permissionsOk(ctx) }
    val sortLabel = when (sort) {
        "name" -> "Name A to Z"
        "new" -> "Recently added"
        else -> "Nearest first"
    }

    val q = query.trim().lowercase()
    val rows = saved
        .filter { r ->
            when (filter) {
                "pending" -> !r.completed
                "completed" -> r.completed
                else -> true
            }
        }
        .filter { q.isEmpty() || "${it.caller} ${it.task}".lowercase().contains(q) }
        .let { list ->
            when (sort) {
                "name" -> list.sortedWith(compareBy({ it.caller.lowercase() }, { it.task.lowercase() }))
                "new" -> list.sortedByDescending { it.created }
                else -> list.sortedWith(compareBy({ it.completed }, { it.dueAt }))
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                item {
                    // Search pill with the app badge
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CookieButton(size = 52, container = MaterialTheme.colorScheme.primary) {
                            PhoneGlyph(24, MaterialTheme.colorScheme.onPrimary)
                        }
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search Laksh") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CookieButton(size = 48, onClick = {
                            sort = when (sort) {
                                "next" -> "new"
                                "new" -> "name"
                                else -> "next"
                            }
                        }) {
                            Text("A↕", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        listOf("all" to "All", "pending" to "Pending", "completed" to "Completed").forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { Text(label, fontSize = 15.sp) },
                                shape = RoundedCornerShape(50),
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.height(48.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your calls", fontSize = 34.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${rows.size} calls · $sortLabel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                            )
                        }
                        TextButton(onClick = {
                            editingReminder = null
                            showNew = true
                        }) {
                            Text("+  New call", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (!ready) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSetup = true },
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("Finish setup", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Text(
                                    "Tap here to allow calls to ring over the lock screen.",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (rows.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (saved.isEmpty()) "No calls yet. Tap New call to schedule your first one."
                                else "No calls match this search or filter.",
                                Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                itemsIndexed(rows, key = { _, r -> r.id }) { index, r ->
                    val big = 28.dp
                    val small = 6.dp
                    val top = if (index == 0) big else small
                    val bottom = if (index == rows.lastIndex) big else small
                    ReminderRow(
                        r = r,
                        now = clock,
                        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                        onTest = {
                            Reminders.testIn5s(ctx, r.id)
                            reload()
                            scope.launch { snack.showSnackbar("Test call in 5 seconds. Lock your phone to see it.") }
                        },
                        onEdit = {
                            editingReminder = r
                            showNew = true
                        },
                        onReactivate = {
                            var u = r.copy(
                                completed = false,
                                tries = 0,
                                snoozeUntil = 0L,
                                testOnly = false,
                                last = "",
                                dateMillis = System.currentTimeMillis(),
                            )
                            u = u.copy(nextAt = Reminders.computeNext(u, System.currentTimeMillis()))
                            if (u.nextAt <= System.currentTimeMillis()) {
                                u = u.copy(dateMillis = System.currentTimeMillis() + 86_400_000L)
                                u = u.copy(nextAt = Reminders.computeNext(u, System.currentTimeMillis()))
                            }
                            Reminders.add(ctx, u)
                            reload()
                        },
                        onDelete = {
                            pendingDeleteAudio?.let { runCatching { File(it).delete() } }
                            pendingDeleteAudio = r.audioPath
                            Reminders.delete(ctx, r.id, removeVoiceFile = false)
                            reload()
                            scope.launch {
                                val res = snack.showSnackbar("Deleted.", actionLabel = "Undo")
                                if (res == SnackbarResult.ActionPerformed) {
                                    if (pendingDeleteAudio == r.audioPath) pendingDeleteAudio = null
                                    Reminders.add(ctx, r)
                                    reload()
                                } else if (pendingDeleteAudio == r.audioPath) {
                                    pendingDeleteAudio?.let { runCatching { File(it).delete() } }
                                    pendingDeleteAudio = null
                                }
                            }
                        },
                    )
                }
            }

            // Floating pill menu
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem("Calls", selected = true) { }
                    NavItem("New call", selected = false) {
                        editingReminder = null
                        showNew = true
                    }
                    NavItem("Setup", selected = false) { showSetup = true }
                }
            }
        }
    }

    if (showNew) {
        ModalBottomSheet(
            onDismissRequest = {
                showNew = false
                editingReminder = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            ScheduleForm(
                editing = editingReminder,
                onAdded = {
                    val wasEdit = editingReminder != null
                    showNew = false
                    editingReminder = null
                    reload()
                    scope.launch {
                        val prefix = if (wasEdit) "Updated." else "Scheduled."
                        snack.showSnackbar("$prefix ${Fmt.eta(it, System.currentTimeMillis())}.")
                    }
                },
            )
        }
    }

    if (showSetup) {
        ModalBottomSheet(
            onDismissRequest = { showSetup = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            SetupPanel(refresh, bump)
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReminderRow(
    r: Reminder,
    now: Long,
    shape: Shape,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menu = true },
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Fmt.initials(r.caller),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PhoneGlyph(14, MaterialTheme.colorScheme.primary)
                        Text(Fmt.freq(r), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(r.task, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(
                        "${r.caller} · ${Fmt.clock(r.hour, r.minute)}" + if (r.audioPath != null) " · voice note" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                    )
                    Text(
                        if (r.completed) "Completed" else Fmt.eta(r, now),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    if (r.last.isNotEmpty()) {
                        Text(r.last, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
                CookieButton(size = 52, onClick = { if (r.completed) onReactivate() else onTest() }) {
                    if (r.completed) {
                        Text("✓", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        PhoneGlyph(22)
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (!r.completed) {
                DropdownMenuItem(text = { Text("Test call in 5 seconds") }, onClick = { menu = false; onTest() })
            } else {
                DropdownMenuItem(text = { Text("Reactivate") }, onClick = { menu = false; onReactivate() })
            }
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}

/* ---------- Setup sheet ---------- */

@Composable
private fun SetupPanel(refresh: Int, bump: () -> Unit) {
    val ctx = LocalContext.current
    val notifOk = remember(refresh) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val fsOk = remember(refresh) {
        Build.VERSION.SDK_INT < 34 ||
            ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { bump() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Setup", fontSize = 28.sp, fontWeight = FontWeight.Medium)
        if (notifOk && fsOk) {
            Text("Ready", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(
                "Calls will ring over the lock screen.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!notifOk) {
            Text("Laksh needs notification permission to show the incoming call.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Allow notifications") }
        }
        if (!fsOk) {
            Text("Android needs your OK to show a call over the lock screen.")
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 34) {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${ctx.packageName}"),
                        ),
                    )
                }
            }) { Text("Allow full-screen calls") }
        }
        Text(
            "Tip: in your phone's battery settings, set Laksh to Unrestricted so calls ring on time.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ---------- New / Edit call sheet ---------- */

@Composable
private fun ScheduleForm(editing: Reminder? = null, onAdded: (Reminder) -> Unit) {
    val ctx = LocalContext.current
    val now = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 2) } }
    var hour by remember { mutableIntStateOf(editing?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(editing?.minute ?: now.get(Calendar.MINUTE)) }
    var caller by remember { mutableStateOf(editing?.caller ?: "") }
    var task by remember { mutableStateOf(editing?.task ?: "") }
    var freq by remember { mutableStateOf(editing?.freq ?: Freq.ONCE) }
    var dateMillis by remember { mutableLongStateOf(editing?.dateMillis ?: System.currentTimeMillis()) }
    var dow by remember { mutableIntStateOf(editing?.dow ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    var speak by remember { mutableStateOf(editing?.speak ?: true) }
    val originalAudioPath = editing?.audioPath
    var voicePath by remember { mutableStateOf(editing?.audioPath) }
    var recording by remember { mutableStateOf(false) }
    var formSaved by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }

    fun stopRecording() {
        if (recording) {
            recording = false
            val p = recorder.stop()
            if (p != null) voicePath = p else Toast.makeText(ctx, "Recording failed. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    fun beginRecording() {
        if (voicePath != null && voicePath != originalAudioPath) {
            voicePath?.let { runCatching { File(it).delete() } }
        }
        voicePath = null
        if (recorder.start()) recording = true
        else Toast.makeText(ctx, "Could not start the microphone.", Toast.LENGTH_SHORT).show()
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) beginRecording() else Toast.makeText(ctx, "Microphone permission is needed for voice notes.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(recording) {
        if (recording) {
            delay(60_000)
            stopRecording()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recording) {
                recorder.cancel()
            }
            if (!formSaved && voicePath != null && voicePath != originalAudioPath) {
                voicePath?.let { runCatching { File(it).delete() } }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (editing == null) "New reminder call" else "Edit reminder call",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("I want a reminder call at")
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
            }) { Text(Fmt.clock(hour, minute)) }
        }

        Text("where")
        OutlinedTextField(
            value = caller,
            onValueChange = { caller = it.take(40) },
            label = { Text("Caller name, like Mom or Doctor") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("will call me")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(freq == Freq.ONCE, { freq = Freq.ONCE }, { Text("Once") }, shape = RoundedCornerShape(50))
            FilterChip(freq == Freq.DAILY, { freq = Freq.DAILY }, { Text("Every day") }, shape = RoundedCornerShape(50))
            FilterChip(freq == Freq.WEEKLY, { freq = Freq.WEEKLY }, { Text("Every week") }, shape = RoundedCornerShape(50))
        }

        if (freq == Freq.ONCE) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("on")
                OutlinedButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    DatePickerDialog(ctx, { _, y, mo, d ->
                        dateMillis = Calendar.getInstance().apply { set(y, mo, d, 12, 0, 0) }.timeInMillis
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text(Fmt.day(dateMillis)) }
            }
        }
        if (freq == Freq.WEEKLY) {
            val days = listOf(
                "Mon" to Calendar.MONDAY, "Tue" to Calendar.TUESDAY, "Wed" to Calendar.WEDNESDAY,
                "Thu" to Calendar.THURSDAY, "Fri" to Calendar.FRIDAY, "Sat" to Calendar.SATURDAY,
                "Sun" to Calendar.SUNDAY,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                days.forEach { (label, value) ->
                    FilterChip(dow == value, { dow = value }, { Text(label) }, shape = RoundedCornerShape(50))
                }
            }
        }

        Text("to remind me about")
        OutlinedTextField(
            value = task,
            onValueChange = { task = it.take(200) },
            label = { Text("Call Mom for her medication") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recording) {
                Button(onClick = { stopRecording() }) { Text("Stop recording") }
                Text("Recording… up to 60 seconds", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                OutlinedButton(onClick = {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) beginRecording() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text(if (voicePath == null) "Record voice note" else "Record again") }
                if (voicePath != null) {
                    TextButton(onClick = {
                        if (voicePath != originalAudioPath) {
                            voicePath?.let { runCatching { File(it).delete() } }
                        }
                        voicePath = null
                    }) { Text("Remove note") }
                }
            }
        }
        if (voicePath != null && !recording) {
            Text("Voice note attached. It plays when you answer.", color = MaterialTheme.colorScheme.primary)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(speak, { speak = it })
            Text("Read the task aloud when I answer (if no voice note)")
        }

        Button(
            onClick = {
                stopRecording()
                if (task.isBlank()) {
                    Toast.makeText(ctx, "Write what the call should remind you about.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val r = if (editing == null) {
                    Reminders.newReminder(caller, task.trim(), hour, minute, freq, dateMillis, dow, speak, voicePath)
                } else {
                    val base = editing.copy(
                        caller = caller.ifBlank { "Reminder" },
                        task = task.trim(),
                        hour = hour,
                        minute = minute,
                        freq = freq,
                        dateMillis = dateMillis,
                        dow = dow,
                        speak = speak,
                        audioPath = voicePath,
                        completed = false,
                        snoozeUntil = 0L,
                        tries = 0,
                        testOnly = false,
                    )
                    base.copy(nextAt = Reminders.computeNext(base, System.currentTimeMillis()))
                }
                if (r.freq == Freq.ONCE && r.nextAt <= System.currentTimeMillis()) {
                    Toast.makeText(ctx, "That time has passed. Pick a later time or date.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (originalAudioPath != null && originalAudioPath != voicePath) {
                    runCatching { File(originalAudioPath).delete() }
                }
                formSaved = true
                Reminders.add(ctx, r)
                onAdded(r)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text(if (editing == null) "Schedule the call" else "Save changes", fontSize = 16.sp) }
    }
}
