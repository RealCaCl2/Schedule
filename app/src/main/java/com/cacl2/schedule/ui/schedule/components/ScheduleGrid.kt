package com.cacl2.schedule.ui.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.CourseEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleGrid(
    courses: List<CourseEntity>,
    periodsPerDay: Int,
    showWeekend: Boolean,
    semesterStartDate: String,
    currentWeek: Int,
    onCourseClick: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellHeight: Dp = 58.dp
    val timeColumnWidth: Dp = 30.dp
    val dayCount = if (showWeekend) 7 else 5
    val isDarkTheme = isSystemInDarkTheme()
    val coursesByDay = remember(courses) { courses.groupBy { it.dayOfWeek } }

    val weekDates = remember(semesterStartDate, currentWeek, showWeekend) {
        getWeekDates(semesterStartDate, currentWeek, showWeekend)
    }
    val today = LocalDate.now()
    val todayColumn: Int? = remember(weekDates) {
        val idx = weekDates.indexOf(today)
        if (idx >= 0) idx + 1 else null
    }
    val monthLabel = remember(weekDates) {
        if (weekDates.isEmpty()) ""
        else {
            val months = weekDates.map { it.monthValue }.distinct()
            if (months.size == 1) {
                "single:${months.first()}"
            } else {
                "cross:${months.first()}:${months.last()}"
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dayWidth = (maxWidth - timeColumnWidth) / dayCount

        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(timeColumnWidth)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    val monthText = when {
                        monthLabel.startsWith("single:") -> {
                            val month = monthLabel.removePrefix("single:").toIntOrNull() ?: 0
                            if (month > 0) stringResource(R.string.schedule_month_single, month) else ""
                        }

                        monthLabel.startsWith("cross:") -> {
                            val parts = monthLabel.removePrefix("cross:").split(":")
                            val m1 = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val m2 = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            if (m1 > 0 && m2 > 0) {
                                stringResource(R.string.schedule_month_cross, m1, m2)
                            } else {
                                ""
                            }
                        }

                        else -> ""
                    }

                    Text(
                        text = monthText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VerticalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.height(44.dp)
                )

                DayHeader(
                    showWeekend = showWeekend,
                    semesterStartDate = semesterStartDate,
                    currentWeek = currentWeek,
                    dayWidth = dayWidth
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            val gridScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(gridScrollState)
            ) {
                TimeColumn(
                    periodsPerDay = periodsPerDay,
                    cellHeight = cellHeight
                )

                VerticalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.height(cellHeight * periodsPerDay)
                )

                for (day in 1..dayCount) {
                    val highlightTodayColumn = todayColumn == day
                    val columnColor = animateColorAsState(
                        targetValue = if (highlightTodayColumn) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        label = "todayColumnColor"
                    ).value

                    Box(
                        modifier = Modifier
                            .width(dayWidth)
                            .height(cellHeight * periodsPerDay)
                            .background(columnColor)
                    ) {
                        for (period in 1..periodsPerDay) {
                            HorizontalDivider(
                                thickness = 0.25.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                modifier = Modifier.offset(y = cellHeight * period)
                            )
                        }

                        val dayCourses = coursesByDay[day].orEmpty()
                        dayCourses.forEach { course ->
                            val yOffset = cellHeight * (course.startPeriod - 1)
                            CourseCard(
                                course = course,
                                cellHeight = cellHeight,
                                dayWidth = dayWidth,
                                isDarkTheme = isDarkTheme,
                                onClick = { onCourseClick(course) },
                                modifier = Modifier.offset(y = yOffset)
                            )
                        }
                    }

                    if (day < dayCount) {
                        VerticalDivider(
                            thickness = 0.25.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.height(cellHeight * periodsPerDay)
                        )
                    }
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
