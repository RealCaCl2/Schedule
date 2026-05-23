package com.cacl2.schedule.ui.schedule

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.ui.schedule.components.ScheduleGrid
import com.cacl2.schedule.ui.schedule.components.WeekSelector
import com.cacl2.schedule.ui.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    courseRepository: CourseRepository,
    settingsRepository: SettingsRepository,
    onEditCourse: (Long) -> Unit = {},
    viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(courseRepository, settingsRepository)
    )
) {
    val settings by viewModel.settings.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekCoursesByWeek by viewModel.weekCoursesByWeek.collectAsState()

    var selectedCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasAutoJumped by remember { mutableStateOf(false) }

    LaunchedEffect(settings.semesterStartDate, settings.totalWeeks) {
        if (settings.semesterStartDate.isNotBlank()) {
            viewModel.initCurrentWeek(settings.semesterStartDate, settings.totalWeeks)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = (currentWeek - 1).coerceAtLeast(0),
        pageCount = { settings.totalWeeks }
    )

    LaunchedEffect(currentWeek, settings.totalWeeks) {
        val targetPage = (currentWeek - 1).coerceIn(0, (settings.totalWeeks - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetPage) {
            if (!hasAutoJumped) {
                pagerState.scrollToPage(targetPage)
                hasAutoJumped = true
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page -> viewModel.setCurrentWeek(page + 1) }
    }

    val currentWeekCourses by remember {
        derivedStateOf { weekCoursesByWeek[currentWeek].orEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.schedule_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.schedule_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEditCourse(0L) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.edit_add_course)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.ScreenHorizontal, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(AppDimens.ItemGap)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.RadiusMedium),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    WeekSelector(
                        currentWeek = currentWeek,
                        totalWeeks = settings.totalWeeks,
                        semesterStartDate = settings.semesterStartDate,
                        onWeekChange = { viewModel.setCurrentWeek(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(AppDimens.RadiusLarge),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        contentPadding = PaddingValues(0.dp)
                    ) { page ->
                        val weekNumber = page + 1
                        val pageCourses = weekCoursesByWeek[weekNumber].orEmpty()

                        Box(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = currentWeekCourses.isEmpty() && weekNumber == currentWeek,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(30.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.schedule_empty_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.schedule_empty_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            ScheduleGrid(
                                courses = pageCourses,
                                periodsPerDay = settings.periodsPerDay,
                                showWeekend = settings.showWeekend,
                                semesterStartDate = settings.semesterStartDate,
                                currentWeek = weekNumber,
                                onCourseClick = { selectedCourse = it }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { selectedCourse = null },
            title = { Text(course.courseName) },
            text = {
                Column {
                    Text(stringResource(R.string.schedule_dialog_teacher, course.teacher))
                    Text(stringResource(R.string.schedule_dialog_location, course.location))
                    Text(
                        stringResource(
                            R.string.schedule_dialog_time,
                            dayOfWeekName(course.dayOfWeek),
                            course.startPeriod,
                            course.endPeriod
                        )
                    )
                    Text(
                        stringResource(
                            R.string.schedule_dialog_weeks,
                            course.startWeek,
                            course.endWeek,
                            weekTypeName(course.weekType)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            stringResource(R.string.edit_delete),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = course.id
                    selectedCourse = null
                    onEditCourse(id)
                }) {
                    Text(stringResource(R.string.edit_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCourse = null }) {
                    Text(stringResource(R.string.schedule_dialog_close))
                }
            }
        )
    }

    if (showDeleteConfirm && selectedCourse != null) {
        val courseToDelete = selectedCourse!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.edit_delete_confirm_title)) },
            text = { Text(stringResource(R.string.edit_delete_confirm_message, courseToDelete.courseName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(courseToDelete)
                    showDeleteConfirm = false
                    selectedCourse = null
                }) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun dayOfWeekName(day: Int): String = when (day) {
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
private fun weekTypeName(type: Int): String = when (type) {
    1 -> stringResource(R.string.schedule_week_type_odd)
    2 -> stringResource(R.string.schedule_week_type_even)
    else -> ""
}


