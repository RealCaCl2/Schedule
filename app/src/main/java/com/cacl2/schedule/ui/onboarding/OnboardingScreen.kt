package com.cacl2.schedule.ui.onboarding

import android.app.DatePickerDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.local.entity.SemesterEntity
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.SharedScheduleCodec
import com.cacl2.schedule.ui.scan.ScanActivity
import com.cacl2.schedule.util.CourseColorMapper
import com.cacl2.schedule.util.QiangZhiUrlNormalizer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    semesterRepository: SemesterRepository,
    onComplete: () -> Unit,
    onScanComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var totalWeeksDraft by remember { mutableStateOf("20") }
    var periodsPerDayDraft by remember { mutableStateOf("12") }
    var semesterStartDate by remember { mutableStateOf("") }
    var qiangzhiUrlDraft by remember { mutableStateOf("https://portal.hyit.edu.cn") }

    data class UrlPreset(val label: String, val url: String)
    val customLabel = stringResource(R.string.settings_url_custom)
    val urlPresets = remember {
        listOf(
            UrlPreset("淮安大学", "https://portal.hyit.edu.cn"),
            UrlPreset(customLabel, "")
        )
    }
    var selectedUrlPreset by remember { mutableStateOf(urlPresets.first()) }
    var urlExpanded by remember { mutableStateOf(false) }

    var totalWeeksError by remember { mutableStateOf<String?>(null) }
    var periodsError by remember { mutableStateOf<String?>(null) }
    var semesterDateError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Scan launcher
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanned = result.data?.getStringExtra("SCAN_RESULT") ?: return@rememberLauncherForActivityResult
            val data = SharedScheduleCodec.decode(scanned)
            if (data != null) {
                scope.launch {
                    // Complete onboarding
                    settingsRepository.completeOnboarding(
                        totalWeeks = data.semester.totalWeeks,
                        periodsPerDay = data.semester.periodsPerDay,
                        semesterStartDate = data.semester.startDate,
                        qiangzhiUrl = ""
                    )
                    // Create semester from scanned data
                    val semesterId = data.semester.startDate.replace("-", "").take(6) + "-" + data.semester.totalWeeks.toString()
                    val semester = SemesterEntity(
                        id = semesterId,
                        name = data.semester.name,
                        startDate = data.semester.startDate,
                        totalWeeks = data.semester.totalWeeks,
                        periodsPerDay = data.semester.periodsPerDay
                    )
                    semesterRepository.insert(semester)
                    settingsRepository.setActiveSemester(semester.id)
                    // Import courses
                    val courses = data.courses.map {
                        CourseEntity(
                            courseName = it.courseName,
                            teacher = it.teacher,
                            location = it.location,
                            dayOfWeek = it.dayOfWeek,
                            startPeriod = it.startPeriod,
                            endPeriod = it.endPeriod,
                            startWeek = it.startWeek,
                            endWeek = it.endWeek,
                            weekType = it.weekType,
                            colorIndex = CourseColorMapper.getColorIndex(it.courseName),
                            semesterId = semester.id
                        )
                    }
                    if (courses.isNotEmpty()) {
                        // Need a CourseRepository here — use the app-level one
                        val app = context.applicationContext as com.cacl2.schedule.ScheduleApplication
                        val courseRepo = com.cacl2.schedule.data.repository.CourseRepository(app.database.courseDao())
                        courseRepo.insertAll(courses)
                    }
                    onScanComplete()
                }
            } else {
                scanError = context.getString(R.string.import_error_invalid)
            }
        }
    }

    val totalWeeksErrorText = stringResource(R.string.settings_total_weeks_error)
    val periodsPerDayErrorText = stringResource(R.string.settings_periods_per_day_error)
    val urlErrorText = stringResource(R.string.settings_system_url_error)
    val semesterDateRequiredText = stringResource(R.string.onboarding_semester_date_required)

    fun submit() {
        val totalWeeks = totalWeeksDraft.trim().toIntOrNull()
        val periods = periodsPerDayDraft.trim().toIntOrNull()
        val normalizedUrl = QiangZhiUrlNormalizer.normalizeOrNull(qiangzhiUrlDraft)

        totalWeeksError = if (totalWeeks == null || totalWeeks !in 1..30) totalWeeksErrorText else null
        periodsError = if (periods == null || periods !in 1..16) periodsPerDayErrorText else null
        semesterDateError = if (semesterStartDate.isBlank()) semesterDateRequiredText else null
        urlError = if (normalizedUrl == null) urlErrorText else null

        if (totalWeeksError != null || periodsError != null || semesterDateError != null || urlError != null) {
            return
        }

        isSaving = true
        scope.launch {
            // Save onboarding settings to DataStore
            settingsRepository.completeOnboarding(
                totalWeeks = totalWeeks!!,
                periodsPerDay = periods!!,
                semesterStartDate = semesterStartDate,
                qiangzhiUrl = normalizedUrl!!
            )

            // Create the first semester from onboarding data
            val semesterId = semesterStartDate.replace("-", "").take(6) + "-" + totalWeeks.toString()
            val semester = SemesterEntity(
                id = semesterId,
                name = "默认学期",
                startDate = semesterStartDate,
                totalWeeks = totalWeeks!!,
                periodsPerDay = periods!!
            )
            semesterRepository.insert(semester)
            settingsRepository.setActiveSemester(semester.id)

            // Mark migration as done so ensureDefaultSemesterIfNeeded won't create a duplicate
            // (this is handled by the fact that count() > 0 after insert, so it's a no-op)

            isSaving = false
            onComplete()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = urlExpanded,
                        onExpandedChange = { urlExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedUrlPreset.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.settings_system_url)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urlExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = urlExpanded,
                            onDismissRequest = { urlExpanded = false }
                        ) {
                            urlPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.label) },
                                    onClick = {
                                        selectedUrlPreset = preset
                                        urlExpanded = false
                                        if (preset.url.isNotBlank()) {
                                            qiangzhiUrlDraft = preset.url
                                            urlError = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (selectedUrlPreset.url.isEmpty()) {
                        OutlinedTextField(
                            value = qiangzhiUrlDraft,
                            onValueChange = { qiangzhiUrlDraft = it; urlError = null },
                            label = { Text(stringResource(R.string.settings_system_url)) },
                            placeholder = { Text(stringResource(R.string.settings_system_url_placeholder)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            isError = urlError != null,
                            supportingText = { urlError?.let { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = totalWeeksDraft,
                        onValueChange = {
                            totalWeeksDraft = it
                            totalWeeksError = null
                        },
                        label = { Text(stringResource(R.string.settings_total_weeks)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {}),
                        singleLine = true,
                        isError = totalWeeksError != null,
                        supportingText = { totalWeeksError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = periodsPerDayDraft,
                        onValueChange = {
                            periodsPerDayDraft = it
                            periodsError = null
                        },
                        label = { Text(stringResource(R.string.settings_periods_per_day)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = periodsError != null,
                        supportingText = { periodsError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            val selectedDate = semesterStartDate
                                .takeIf { it.isNotBlank() }
                                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                            val calendar = Calendar.getInstance().apply {
                                if (selectedDate != null) {
                                    set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
                                }
                            }

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    semesterStartDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                                    semesterDateError = null
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = semesterStartDate.ifEmpty { stringResource(R.string.settings_semester_start_date) })
                    }
                    if (semesterDateError != null) {
                        Text(
                            text = semesterDateError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { submit() },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .height(16.dp)
                            )
                        } else {
                            Text(stringResource(R.string.onboarding_continue))
                        }
                    }
                    Text(
                        text = stringResource(R.string.onboarding_import_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scan import card
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.onboarding_scan_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_scan_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            scanError = null
                            scanLauncher.launch(Intent(context, ScanActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.scan_import_course))
                    }
                    scanError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
