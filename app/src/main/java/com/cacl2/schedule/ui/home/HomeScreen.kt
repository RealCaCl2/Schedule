package com.cacl2.schedule.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.ui.schedule.ScheduleViewModel
import com.cacl2.schedule.ui.theme.AppDimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    courseRepository: CourseRepository,
    settingsRepository: SettingsRepository,
    semesterRepository: SemesterRepository,
    viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(
            courseRepository, settingsRepository, semesterRepository,
            application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val settings by viewModel.settings.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekCoursesByWeek by viewModel.weekCoursesByWeek.collectAsState()
    val activeSemester by viewModel.activeSemester.collectAsState()

    LaunchedEffect(activeSemester) {
        val semester = activeSemester
        if (semester != null && semester.startDate.isNotBlank()) {
            viewModel.initCurrentWeek(semester.startDate, semester.totalWeeks)
        }
    }

    val today = LocalDate.now()
    val todayDay = today.dayOfWeek.value
    val todayCourses = weekCoursesByWeek[currentWeek]
        .orEmpty()
        .filter { it.dayOfWeek == todayDay }
        .sortedWith(compareBy<CourseEntity> { it.startPeriod }.thenBy { it.endPeriod })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppDimens.ItemGap),
                contentPadding = PaddingValues(AppDimens.ScreenHorizontal)
            ) {
                item {
                    TodayHeader(today = today, currentWeek = currentWeek)
                }

                if (todayCourses.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.home_empty_today),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.home_course_count, todayCourses.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(todayCourses, key = { _, course -> course.id }) { index, course ->
                        TodayCourseTimelineCard(
                            course = course,
                            isLast = index == todayCourses.lastIndex,
                            showTeacher = settings.showTeacher,
                            showLocation = settings.showLocation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(today: LocalDate, currentWeek: Int) {
    val dateLabel = today.format(DateTimeFormatter.ofPattern("MM/dd"))
    val dayLabel = dayName(today.dayOfWeek.value)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusLarge),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.HeroPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.home_date_with_day, dateLabel, dayLabel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.home_week_label, currentWeek),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun TodayCourseTimelineCard(
    course: CourseEntity,
    isLast: Boolean,
    showTeacher: Boolean = true,
    showLocation: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.width(36.dp).padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${course.startPeriod}", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (course.endPeriod > course.startPeriod) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("${course.endPeriod}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (!isLast) Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(course.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val info = listOfNotNull(
                    if (showLocation) course.location.ifBlank { null } else null,
                    if (showTeacher) course.teacher.ifBlank { null } else null
                )
                if (info.isNotEmpty()) Text(info.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.home_weeks_label, course.startWeek, course.endWeek, weekTypeLabel(course.weekType)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun dayName(day: Int): String = when (day) {
    1 -> stringResource(R.string.schedule_day_monday)
    2 -> stringResource(R.string.schedule_day_tuesday)
    3 -> stringResource(R.string.schedule_day_wednesday)
    4 -> stringResource(R.string.schedule_day_thursday)
    5 -> stringResource(R.string.schedule_day_friday)
    6 -> stringResource(R.string.schedule_day_saturday)
    7 -> stringResource(R.string.schedule_day_sunday)
    else -> day.toString()
}

@Composable
private fun weekTypeLabel(type: Int): String = when (type) {
    1 -> stringResource(R.string.schedule_week_type_odd)
    2 -> stringResource(R.string.schedule_week_type_even)
    else -> ""
}
