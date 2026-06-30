package com.cacl2.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
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
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.SharedScheduleCodec
import com.cacl2.schedule.model.SharedScheduleData
import com.cacl2.schedule.model.SharedSemester
import com.cacl2.schedule.model.ThemeMode
import com.cacl2.schedule.ui.share.ShareQrDialog
import com.cacl2.schedule.ui.schedule.components.ScheduleGrid
import com.cacl2.schedule.ui.schedule.components.WeekSelector
import com.cacl2.schedule.ui.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    courseRepository: CourseRepository,
    settingsRepository: SettingsRepository,
    semesterRepository: SemesterRepository,
    onEditCourse: (Long) -> Unit = {},
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
    val allSemesters by viewModel.allSemesters.collectAsState()

    var selectedCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasAutoJumped by remember { mutableStateOf(false) }
    var showShareQr by remember { mutableStateOf(false) }
    var shareQrData by remember { mutableStateOf<SharedScheduleData?>(null) }

    // allSemesters is null until Room emits; empty list means truly no semesters
    val totalWeeks = activeSemester?.totalWeeks ?: settings.totalWeeks
    val semesterStartDate = activeSemester?.startDate ?: settings.semesterStartDate
    val isDarkTheme = when (ThemeMode.fromValue(settings.themeMode)) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }

    LaunchedEffect(activeSemester) {
        val semester = activeSemester
        if (semester != null && semester.startDate.isNotBlank()) {
            viewModel.initCurrentWeek(semester.startDate, semester.totalWeeks)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = (currentWeek - 1).coerceAtLeast(0),
        pageCount = { totalWeeks }
    )

    LaunchedEffect(currentWeek, totalWeeks) {
        val targetPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0))
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
                    IconButton(onClick = {
                        val semester = activeSemester
                        val courses = weekCoursesByWeek.values.flatten().distinct()
                        if (semester != null) {
                            shareQrData = SharedScheduleData(
                                semester = SharedSemester(
                                    name = semester.name,
                                    startDate = semester.startDate,
                                    totalWeeks = semester.totalWeeks,
                                    periodsPerDay = semester.periodsPerDay
                                ),
                                courses = courses.map { SharedScheduleCodec.toSharedCourse(it) }
                            )
                            showShareQr = true
                        }
                    }) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = stringResource(R.string.share_qr_title)
                        )
                    }
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
        if (allSemesters != null && allSemesters!!.isEmpty()) {
            // No semesters at all — show guidance
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(30.dp)
                ) {
                    Text(
                        text = stringResource(R.string.schedule_no_semester_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.schedule_no_semester_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
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
                        totalWeeks = totalWeeks,
                        semesterStartDate = semesterStartDate,
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
                        beyondViewportPageCount = 0,
                        contentPadding = PaddingValues(0.dp),
                        key = { page -> page }
                    ) { page ->
                        val weekNumber = page + 1
                        val pageCourses = weekCoursesByWeek[weekNumber].orEmpty()

                        Box(modifier = Modifier.fillMaxSize()) {
                            ScheduleGrid(
                                courses = pageCourses,
                                periodsPerDay = activeSemester?.periodsPerDay ?: settings.periodsPerDay,
                                showWeekend = settings.showWeekend,
                                semesterStartDate = semesterStartDate,
                                currentWeek = weekNumber,
                                isDarkTheme = isDarkTheme,
                                showTeacher = settings.showTeacher,
                                showLocation = settings.showLocation,
                                onCourseClick = { selectedCourse = it }
                            )
                        }
                    }
                }
            }
        }
        } // end else (has semesters)
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

    if (showShareQr && shareQrData != null) {
        ShareQrDialog(
            data = shareQrData!!,
            onDismiss = { showShareQr = false }
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
