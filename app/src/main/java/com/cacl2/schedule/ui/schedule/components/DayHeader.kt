package com.cacl2.schedule.ui.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacl2.schedule.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DayHeader(
    showWeekend: Boolean,
    semesterStartDate: String,
    currentWeek: Int,
    dayWidth: Dp,
    modifier: Modifier = Modifier
) {
    val dayNames = if (showWeekend) {
        listOf(
            stringResource(R.string.schedule_day_monday),
            stringResource(R.string.schedule_day_tuesday),
            stringResource(R.string.schedule_day_wednesday),
            stringResource(R.string.schedule_day_thursday),
            stringResource(R.string.schedule_day_friday),
            stringResource(R.string.schedule_day_saturday),
            stringResource(R.string.schedule_day_sunday)
        )
    } else {
        listOf(
            stringResource(R.string.schedule_day_monday),
            stringResource(R.string.schedule_day_tuesday),
            stringResource(R.string.schedule_day_wednesday),
            stringResource(R.string.schedule_day_thursday),
            stringResource(R.string.schedule_day_friday)
        )
    }

    val dates = getWeekDates(semesterStartDate, currentWeek, showWeekend)
    val today = LocalDate.now()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        dayNames.forEachIndexed { index, name ->
            if (index > 0) {
                VerticalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.height(44.dp)
                )
            }

            val date = dates.getOrNull(index)
            val isToday = date == today
            val dateText = date?.format(DateTimeFormatter.ofPattern("d")).orEmpty()
            val dayBgColor = animateColorAsState(
                targetValue = if (isToday) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "dayHeaderBg"
            ).value

            Box(
                modifier = Modifier
                    .width(dayWidth)
                    .height(44.dp)
                    .background(dayBgColor)
                    .padding(horizontal = 2.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.schedule_day_prefix, name),
                        fontSize = 12.sp,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun getWeekDates(semesterStartDate: String, week: Int, showWeekend: Boolean): List<LocalDate> {
    if (semesterStartDate.isBlank()) return emptyList()
    return try {
        val start = LocalDate.parse(semesterStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val weekStart = start.plusDays(((week - 1) * 7).toLong())
        val dayCount = if (showWeekend) 7 else 5
        (0 until dayCount).map { weekStart.plusDays(it.toLong()) }
    } catch (_: Exception) {
        emptyList()
    }
}

