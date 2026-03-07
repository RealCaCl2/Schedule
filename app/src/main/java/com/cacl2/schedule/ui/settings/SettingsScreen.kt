package com.cacl2.schedule.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    courseRepository: CourseRepository,
    onImportClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository, courseRepository)
    )
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val packageName = context.packageName
    val packageInfo = remember(packageName) { context.packageManager.getPackageInfo(packageName, 0) }
    val versionName = packageInfo.versionName ?: "unknown"
    val versionCode = packageInfo.longVersionCode

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var totalWeeksDraft by remember { mutableStateOf("") }
    var totalWeeksFocused by remember { mutableStateOf(false) }
    var totalWeeksError by remember { mutableStateOf<String?>(null) }

    var periodsPerDayDraft by remember { mutableStateOf("") }
    var periodsPerDayFocused by remember { mutableStateOf(false) }
    var periodsPerDayError by remember { mutableStateOf<String?>(null) }

    var qiangzhiUrlDraft by remember { mutableStateOf("") }
    var qiangzhiUrlError by remember { mutableStateOf<String?>(null) }
    var urlFocused by remember { mutableStateOf(false) }

    val totalWeeksErrorText = stringResource(R.string.settings_total_weeks_error)
    val periodsPerDayErrorText = stringResource(R.string.settings_periods_per_day_error)
    val qiangzhiUrlErrorText = stringResource(R.string.settings_system_url_error)

    LaunchedEffect(settings.totalWeeks, totalWeeksFocused) {
        if (!totalWeeksFocused) {
            totalWeeksDraft = settings.totalWeeks.toString()
            totalWeeksError = null
        }
    }

    LaunchedEffect(settings.periodsPerDay, periodsPerDayFocused) {
        if (!periodsPerDayFocused) {
            periodsPerDayDraft = settings.periodsPerDay.toString()
            periodsPerDayError = null
        }
    }

    LaunchedEffect(settings.qiangzhiUrl, urlFocused) {
        if (!urlFocused) {
            qiangzhiUrlDraft = settings.qiangzhiUrl
            qiangzhiUrlError = null
        }
    }

    fun commitTotalWeeks() {
        val value = totalWeeksDraft.trim().toIntOrNull()
        if (value == null || value !in 1..30) {
            totalWeeksError = totalWeeksErrorText
            return
        }
        totalWeeksError = null
        viewModel.updateTotalWeeks(value)
    }

    fun commitPeriodsPerDay() {
        val value = periodsPerDayDraft.trim().toIntOrNull()
        if (value == null || value !in 1..16) {
            periodsPerDayError = periodsPerDayErrorText
            return
        }
        periodsPerDayError = null
        viewModel.updatePeriodsPerDay(value)
    }

    fun commitQiangzhiUrl() {
        val normalized = viewModel.normalizeQiangzhiUrl(qiangzhiUrlDraft)
        if (normalized == null) {
            qiangzhiUrlError = qiangzhiUrlErrorText
            return
        }
        qiangzhiUrlError = null
        qiangzhiUrlDraft = normalized
        viewModel.updateQiangzhiUrl(normalized)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.settings_subtitle),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))


            SettingsSection(
                title = stringResource(R.string.settings_semester_section),
                icon = Icons.Default.Tune
            ) {
                NumberFieldItem(
                    value = totalWeeksDraft,
                    onValueChange = {
                        totalWeeksDraft = it
                        totalWeeksError = null
                    },
                    label = stringResource(R.string.settings_total_weeks),
                    imeAction = ImeAction.Next,
                    onImeAction = { commitTotalWeeks() },
                    focused = totalWeeksFocused,
                    onFocusChange = { totalWeeksFocused = it },
                    onBlurCommit = { commitTotalWeeks() },
                    errorText = totalWeeksError
                )

                NumberFieldItem(
                    value = periodsPerDayDraft,
                    onValueChange = {
                        periodsPerDayDraft = it
                        periodsPerDayError = null
                    },
                    label = stringResource(R.string.settings_periods_per_day),
                    imeAction = ImeAction.Done,
                    onImeAction = { commitPeriodsPerDay() },
                    focused = periodsPerDayFocused,
                    onFocusChange = { periodsPerDayFocused = it },
                    onBlurCommit = { commitPeriodsPerDay() },
                    errorText = periodsPerDayError
                )

                OutlinedButton(
                    onClick = {
                        val selectedDate = settings.semesterStartDate
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
                                val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                                viewModel.updateSemesterStartDate(date)
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
                    Text(
                        text = settings.semesterStartDate.ifEmpty {
                            stringResource(R.string.settings_semester_start_date)
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.settings_show_weekend))
                            Text(
                                text = stringResource(R.string.settings_show_weekend_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showWeekend,
                            onCheckedChange = { viewModel.updateShowWeekend(it) }
                        )
                    }
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_system_section),
                icon = Icons.Default.SettingsSuggest
            ) {
                OutlinedTextField(
                    value = qiangzhiUrlDraft,
                    onValueChange = {
                        qiangzhiUrlDraft = it
                        qiangzhiUrlError = null
                    },
                    label = { Text(stringResource(R.string.settings_system_url)) },
                    placeholder = { Text(stringResource(R.string.settings_system_url_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitQiangzhiUrl() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            val wasFocused = urlFocused
                            urlFocused = focusState.isFocused
                            if (wasFocused && !focusState.isFocused) {
                                commitQiangzhiUrl()
                            }
                        },
                    singleLine = true,
                    isError = qiangzhiUrlError != null,
                    supportingText = { qiangzhiUrlError?.let { Text(it) } }
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_data_section),
                icon = Icons.Default.Storage
            ) {
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.import_title))
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_clear_all_courses))
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_more_section),
                icon = Icons.Default.Info
            ) {
                ActionButtonRow(
                    icon = Icons.Default.Policy,
                    text = stringResource(R.string.settings_privacy_policy),
                    onClick = { showPrivacyDialog = true }
                )
                HorizontalDivider()
                ActionButtonRow(
                    icon = Icons.Default.Info,
                    text = stringResource(R.string.settings_about_app),
                    onClick = { showAboutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_confirm_delete_title)) },
            text = { Text(stringResource(R.string.settings_confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllCourses()
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = { Text(stringResource(R.string.privacy_content)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.schedule_dialog_close))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutItem(
                        label = stringResource(R.string.about_app_name_label),
                        value = stringResource(R.string.app_name)
                    )
                    AboutItem(
                        label = stringResource(R.string.about_version_label),
                        value = stringResource(
                            R.string.about_version_value,
                            versionName,
                            versionCode.toInt()
                        )
                    )
                    AboutItem(
                        label = stringResource(R.string.about_package_label),
                        value = packageName
                    )
                    AboutItem(
                        label = stringResource(R.string.about_author_label),
                        value = stringResource(R.string.about_author_value)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.schedule_dialog_close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun NumberFieldItem(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onBlurCommit: () -> Unit,
    errorText: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = if (imeAction == ImeAction.Done) {
            KeyboardActions(onDone = { onImeAction() })
        } else {
            KeyboardActions(onNext = { onImeAction() })
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val wasFocused = focused
                onFocusChange(focusState.isFocused)
                if (wasFocused && !focusState.isFocused) {
                    onBlurCommit()
                }
            },
        singleLine = true,
        isError = errorText != null,
        supportingText = { errorText?.let { Text(it) } }
    )
}

@Composable
private fun ActionButtonRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

