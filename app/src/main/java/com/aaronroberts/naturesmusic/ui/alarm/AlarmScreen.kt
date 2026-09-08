package com.aaronroberts.naturesmusic.ui.alarm

import android.Manifest
import android.os.Build
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.alarm.AlarmPhase
import com.aaronroberts.naturesmusic.alarm.AlarmStore
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.components.ScreenScaffold
import com.aaronroberts.naturesmusic.ui.help.AlarmPageHelp
import com.aaronroberts.naturesmusic.ui.help.HelpQuestionButton
import com.aaronroberts.naturesmusic.ui.theme.AlarmBlue
import com.aaronroberts.naturesmusic.ui.theme.AlarmGreen
import com.aaronroberts.naturesmusic.ui.theme.AlarmRed
import com.aaronroberts.naturesmusic.ui.theme.Ink
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = NaturesMusicApplication.instance
    val settings by app.alarmStore.settings.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    var emptyMixMessage by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(Calendar.getInstance()) }
    var currentTimeText by remember { mutableStateOf(AlarmStore.formatDateTime(Date())) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* alarm still schedules; notification may be limited if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeText = AlarmStore.formatDateTime(Date())
            delay(1_000)
        }
    }

    val (pillTitle, pillColor) = when (settings.phase) {
        AlarmPhase.Idle -> "Alarm" to Ink
        AlarmPhase.Armed -> settings.alarmTimeLabel to AlarmBlue
        AlarmPhase.Ringing -> "Alarm Active" to AlarmRed
        AlarmPhase.Snoozed -> "Snoozed..." to AlarmGreen
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenScaffold(title = "Alarm", modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HelpQuestionButton(onClick = { showHelp = true })
                PillButton(
                    label = "Main Menu",
                    onClick = onBack,
                    fillMaxWidth = false,
                    height = 44.dp,
                    modifier = Modifier.width(180.dp),
                )
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (emptyMixMessage) {
                    Text(
                        text = "Start the sounds you want to wake to, then Set Alarm.",
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }
                SectionLabel("Current Time")
                PillButton(
                    label = currentTimeText,
                    onClick = {},
                    labelColor = AlarmBlue,
                    fontSize = 16.sp,
                    height = 48.dp,
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    PillButton(
                        label = "Set Alarm",
                        onClick = {
                            val armed = app.alarmController.setAlarm(picker)
                            emptyMixMessage = !armed
                        },
                        fillMaxWidth = false,
                        height = 44.dp,
                        modifier = Modifier.width(140.dp),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PillButton(
                            label = "Snooze",
                            onClick = { app.alarmController.snooze() },
                            fillMaxWidth = false,
                            height = 44.dp,
                            modifier = Modifier.width(140.dp),
                        )
                        Text(
                            text = "${settings.snoozeMinutes} mins.",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            PillButton(
                                label = "+",
                                onClick = { app.alarmController.bumpSnooze(plus = true) },
                                fillMaxWidth = false,
                                height = 36.dp,
                                modifier = Modifier.width(50.dp),
                                maxLines = 1,
                            )
                            PillButton(
                                label = "−",
                                onClick = { app.alarmController.bumpSnooze(plus = false) },
                                fillMaxWidth = false,
                                height = 36.dp,
                                modifier = Modifier.width(50.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                SectionLabel("Alarm Time")
                PillButton(
                    label = pillTitle,
                    onClick = {
                        if (settings.phase == AlarmPhase.Idle) {
                            val armed = app.alarmController.setAlarm(picker)
                            emptyMixMessage = !armed
                        } else {
                            app.alarmController.alarmStop()
                            emptyMixMessage = false
                        }
                    },
                    labelColor = pillColor,
                    fontSize = 16.sp,
                    height = 48.dp,
                )
                Spacer(Modifier.height(20.dp))
                DateTimeWheel(
                    value = picker,
                    onValueChange = { picker = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
        AlarmPageHelp(visible = showHelp, onDismiss = { showHelp = false })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

/**
 * iOS UIDatePicker wheels: date | hour | minute | AM-PM (AM-PM omitted in 24-hour locales).
 */
@Composable
fun DateTimeWheel(
    value: Calendar,
    onValueChange: (Calendar) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val is24 = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val dates = remember {
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        (0 until 400).map { offset ->
            (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        }
    }
    val dateLabels = remember(dates) { dates.map { dateWheelLabel(it) }.toTypedArray() }
    val change = remember { object { var current: (Calendar) -> Unit = onValueChange } }
    change.current = onValueChange

    AndroidView(
        modifier = modifier.height(216.dp),
        factory = { ctx ->
            LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                descendantFocusability = LinearLayout.FOCUS_BLOCK_DESCENDANTS

                fun lp(weight: Float) = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    weight,
                )

                val datePicker = NumberPicker(ctx).apply {
                    minValue = 0
                    maxValue = dates.lastIndex
                    displayedValues = dateLabels
                    wrapSelectorWheel = false
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                }
                val hourPicker = NumberPicker(ctx).apply {
                    if (is24) {
                        minValue = 0
                        maxValue = 23
                    } else {
                        minValue = 1
                        maxValue = 12
                    }
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                }
                val minutePicker = NumberPicker(ctx).apply {
                    minValue = 0
                    maxValue = 59
                    displayedValues = Array(60) { String.format(Locale.US, "%02d", it) }
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                }
                val amPmPicker = if (is24) {
                    null
                } else {
                    NumberPicker(ctx).apply {
                        minValue = 0
                        maxValue = 1
                        displayedValues = arrayOf("AM", "PM")
                        wrapSelectorWheel = false
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    }
                }

                fun emit() {
                    val next = Calendar.getInstance()
                    val day = dates.getOrElse(datePicker.value) { Calendar.getInstance() }
                    next.set(Calendar.YEAR, day.get(Calendar.YEAR))
                    next.set(Calendar.MONTH, day.get(Calendar.MONTH))
                    next.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH))
                    if (is24) {
                        next.set(Calendar.HOUR_OF_DAY, hourPicker.value)
                    } else {
                        val hour12 = hourPicker.value
                        val amPm = amPmPicker?.value ?: 0
                        next.set(Calendar.AM_PM, amPm)
                        next.set(Calendar.HOUR, if (hour12 == 12) 0 else hour12)
                    }
                    next.set(Calendar.MINUTE, minutePicker.value)
                    next.set(Calendar.SECOND, 0)
                    next.set(Calendar.MILLISECOND, 0)
                    change.current(next)
                }

                datePicker.setOnValueChangedListener { _, _, _ -> emit() }
                hourPicker.setOnValueChangedListener { _, _, _ -> emit() }
                minutePicker.setOnValueChangedListener { _, _, _ -> emit() }
                amPmPicker?.setOnValueChangedListener { _, _, _ -> emit() }

                addView(datePicker, lp(1.6f))
                addView(hourPicker, lp(0.7f))
                addView(minutePicker, lp(0.7f))
                if (amPmPicker != null) addView(amPmPicker, lp(0.7f))

                tag = arrayOf(datePicker, hourPicker, minutePicker, amPmPicker)
            }
        },
        update = { layout ->
            @Suppress("UNCHECKED_CAST")
            val pickers = layout.tag as Array<*>
            val datePicker = pickers[0] as NumberPicker
            val hourPicker = pickers[1] as NumberPicker
            val minutePicker = pickers[2] as NumberPicker
            val amPmPicker = pickers[3] as NumberPicker?
            val dateIndex = dates.indexOfFirst { sameDay(it, value) }.coerceAtLeast(0)
            if (datePicker.value != dateIndex) datePicker.value = dateIndex
            if (minutePicker.value != value.get(Calendar.MINUTE)) {
                minutePicker.value = value.get(Calendar.MINUTE)
            }
            if (is24) {
                val hour = value.get(Calendar.HOUR_OF_DAY)
                if (hourPicker.value != hour) hourPicker.value = hour
            } else {
                val hour12 = value.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                val amPm = value.get(Calendar.AM_PM)
                if (hourPicker.value != hour12) hourPicker.value = hour12
                if (amPmPicker != null && amPmPicker.value != amPm) amPmPicker.value = amPm
            }
        },
    )
}

private fun sameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun dateWheelLabel(day: Calendar): String {
    val today = Calendar.getInstance()
    if (sameDay(day, today)) return "Today"
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    if (sameDay(day, tomorrow)) return "Tomorrow"
    val fmt = if (day.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
        SimpleDateFormat("EEE MMM d", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }
    return fmt.format(day.time)
}
