package com.cacl2.schedule.ui.settings

import android.app.DatePickerDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.SemesterEntity
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.SharedScheduleCodec
import com.cacl2.schedule.model.ThemeMode
import com.cacl2.schedule.data.UpdateResult
import com.cacl2.schedule.ui.scan.ScanActivity
import com.cacl2.schedule.ui.update.UpdateAvailableDialog
import com.cacl2.schedule.ui.update.UpdateErrorDialog
import com.cacl2.schedule.widget.ScanResultHolder
import java.time.LocalDate
import java.util.Calendar

// ── Spacing system ──────────────────────────────────────────────
private object SettingsDimens {
    val PageHorizontal = 24.dp
    val SectionTop = 32.dp
    val SectionBottom = 16.dp
    val CardPadding = 20.dp
    val ItemPadding = 16.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    courseRepository: CourseRepository,
    semesterRepository: SemesterRepository,
    onImportClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            settingsRepository, courseRepository, semesterRepository,
            application = LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val settings by viewModel.settings.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val ctx = LocalContext.current
    val packageInfo = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }

    // ── Dialog / form state ──────────────────────────────────────
    var showDeleteCoursesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddChoice by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var deleteSemesterId by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<com.cacl2.schedule.model.SharedScheduleData?>(null) }
    var pendingImportRaw by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    val updateState by viewModel.updateState.collectAsState()
    val updateDownloading by viewModel.updateDownloading.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()

    var addName by remember { mutableStateOf("") }
    var addWeeks by remember { mutableStateOf("20") }
    var addPeriods by remember { mutableStateOf("12") }
    var addDate by remember { mutableStateOf("") }

    var editingSemester by remember { mutableStateOf<SemesterEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editWeeks by remember { mutableStateOf("") }
    var editPeriods by remember { mutableStateOf("") }

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
    var urlDraft by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var urlFocused by remember { mutableStateOf(false) }

    LaunchedEffect(settings.qiangzhiUrl) {
        val match = urlPresets.find { it.url == settings.qiangzhiUrl }
        if (match != null) {
            selectedUrlPreset = match
            urlDraft = match.url
        } else if (settings.qiangzhiUrl.isNotBlank()) {
            selectedUrlPreset = urlPresets.last()
            urlDraft = settings.qiangzhiUrl
        } else {
            selectedUrlPreset = urlPresets.first()
            urlDraft = urlPresets.first().url
        }
    }

    // ── Scan launcher ────────────────────────────────────────────
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringExtra("SCAN_RESULT")?.let { ScanResultHolder.setResult(it) }
        }
    }

    LaunchedEffect(Unit) {
        ScanResultHolder.result.collect { raw ->
            if (raw != null) {
                val data = SharedScheduleCodec.decode(raw)
                if (data != null) {
                    pendingImportData = data; pendingImportRaw = raw; showImportConfirm = true
                } else {
                    scanError = ctx.getString(R.string.import_error_invalid)
                }
                ScanResultHolder.consume()
            }
        }
    }

    fun commitUrl() {
        val n = viewModel.normalizeQiangzhiUrl(urlDraft)
        if (n == null) { urlError = ctx.getString(R.string.settings_system_url_error); return }
        urlError = null; urlDraft = n; viewModel.updateQiangzhiUrl(n)
    }

    // ═══════════════════════════════════════════════════════════
    //  UI
    // ═══════════════════════════════════════════════════════════

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontWeight = FontWeight.SemiBold
                        )
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
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(SettingsDimens.SectionTop),
                contentPadding = PaddingValues(
                    start = SettingsDimens.PageHorizontal,
                    top = 16.dp,
                    end = SettingsDimens.PageHorizontal,
                    bottom = 40.dp
                )
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(SettingsDimens.SectionTop)) {

            // ═══ 学期 ═══
                    SettingsSection(title = stringResource(R.string.settings_semester_management)) {
                        SettingsCard {
                if (semesters.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_no_semester),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(SettingsDimens.CardPadding)
                    )
                } else {
                    semesters.forEachIndexed { index, semester ->
                        if (index > 0) SettingsDivider()
                        val active = semester.id == settings.activeSemesterId
                        SemesterRow(
                            semester = semester,
                            isActive = active,
                            onClick = {
                                if (active) {
                                    editName = semester.name; editDate = semester.startDate
                                    editWeeks = semester.totalWeeks.toString(); editPeriods = semester.periodsPerDay.toString()
                                    editingSemester = semester
                                } else {
                                    viewModel.setActiveSemester(semester.id)
                                }
                            },
                            onDelete = { deleteSemesterId = semester.id }
                        )
                    }
                }
                SettingsDivider()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FilledTonalButton(
                        onClick = { showAddChoice = true },
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_add_semester))
                    }
                }
                        }
                    }

            // ═══ 外观 ═══
                    SettingsSection(title = stringResource(R.string.settings_appearance_section)) {
                        SettingsCard {
                Column(
                    modifier = Modifier.padding(
                        horizontal = SettingsDimens.CardPadding,
                        vertical = SettingsDimens.CardPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ThemeModeSelector(
                        selectedMode = settings.themeMode,
                        onModeSelected = { viewModel.updateThemeMode(ThemeMode.fromValue(it)) }
                    )
                }
                SettingsDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_show_weekend),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_show_weekend_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.showWeekend,
                            onCheckedChange = { viewModel.updateShowWeekend(it) }
                        )
                    },
                    colors = SettingsListItemColors()
                )
                SettingsDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_show_teacher),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_show_teacher_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.showTeacher,
                            onCheckedChange = { viewModel.updateShowTeacher(it) }
                        )
                    },
                    colors = SettingsListItemColors()
                )
                SettingsDivider()
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_show_location),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(R.string.settings_show_location_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.showLocation,
                            onCheckedChange = { viewModel.updateShowLocation(it) }
                        )
                    },
                    colors = SettingsListItemColors()
                )
                        }
                    }

            // ═══ 教务系统 ═══
                    SettingsSection(title = stringResource(R.string.settings_system_section)) {
                        SettingsCard {
                Column(
                    modifier = Modifier.padding(SettingsDimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = urlExpanded,
                        onExpandedChange = { urlExpanded = it }
                    ) {
                        TextField(
                            value = selectedUrlPreset.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.settings_system_url)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urlExpanded) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                            ),
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
                                            urlDraft = preset.url
                                            urlError = null
                                            viewModel.normalizeQiangzhiUrl(preset.url)?.let {
                                                viewModel.updateQiangzhiUrl(it)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (selectedUrlPreset.url.isEmpty()) {
                        TextField(
                            value = urlDraft, onValueChange = { urlDraft = it; urlError = null },
                            label = { Text(stringResource(R.string.settings_system_url)) },
                            placeholder = { Text(stringResource(R.string.settings_system_url_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commitUrl() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { fs ->
                                if (urlFocused && !fs.isFocused) commitUrl(); urlFocused = fs.isFocused
                            },
                            singleLine = true, isError = urlError != null,
                            supportingText = urlError?.let { e -> { Text(e) } }
                        )
                    }
                }
                        }
                    }

            // ═══ 关于 ═══
                    SettingsSection(title = stringResource(R.string.settings_more_section)) {
                        SettingsCard {
                AboutItem(
                    icon = Icons.Default.Policy,
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = { showPrivacyDialog = true }
                )
                SettingsDivider()
                AboutItem(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.settings_about_app),
                    onClick = { showAboutDialog = true }
                )
                SettingsDivider()
                AboutItem(
                    icon = Icons.Default.FileDownload,
                    label = stringResource(R.string.settings_check_update),
                    onClick = { viewModel.checkUpdate() }
                )
                        }
                    }

            // ═══ 危险操作 ═══
                    SettingsSection(title = stringResource(R.string.settings_danger_section)) {
                        SettingsCard {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_clear_all_courses),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.clickable { showDeleteCoursesDialog = true },
                    colors = SettingsListItemColors()
                )
                        }
                    }

                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Dialogs (unchanged logic)
    // ═══════════════════════════════════════════════════════════

    if (showDeleteCoursesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCoursesDialog = false },
            title = { Text(stringResource(R.string.settings_confirm_delete_title)) },
            text = { Text(stringResource(R.string.settings_confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllCourses(); showDeleteCoursesDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCoursesDialog = false }) {
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
                    AboutRow(stringResource(R.string.about_app_name_label), stringResource(R.string.app_name))
                    AboutRow(
                        stringResource(R.string.about_version_label),
                        stringResource(R.string.about_version_value, packageInfo.versionName ?: "?", packageInfo.longVersionCode.toInt())
                    )
                    AboutRow(stringResource(R.string.about_package_label), ctx.packageName)
                    AboutRow(stringResource(R.string.about_author_label), stringResource(R.string.about_author_value))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.schedule_dialog_close))
                }
            }
        )
    }

    if (showAddChoice) {
        AlertDialog(
            onDismissRequest = { showAddChoice = false },
            title = { Text(stringResource(R.string.settings_add_semester)) },
            text = { Text(stringResource(R.string.settings_import_choice_desc)) },
            confirmButton = {
                Column(Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { showAddChoice = false; scanLauncher.launch(Intent(ctx, ScanActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.scan_import_course))
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { showAddChoice = false; showAddForm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_jiaowu))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddChoice = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        )
    }

    if (showAddForm) {
        AlertDialog(
            onDismissRequest = { showAddForm = false },
            title = { Text(stringResource(R.string.settings_add_semester)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        addName, { addName = it },
                        label = { Text(stringResource(R.string.settings_semester_name)) },
                        placeholder = { Text(stringResource(R.string.settings_semester_name_hint)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            addDate.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { d ->
                                c.set(d.year, d.monthValue - 1, d.dayOfMonth)
                            }
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d -> addDate = String.format("%04d-%02d-%02d", y, m + 1, d) },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(addDate.ifEmpty { stringResource(R.string.settings_semester_start_date) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            addWeeks, { addWeeks = it },
                            label = { Text(stringResource(R.string.settings_total_weeks)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            addPeriods, { addPeriods = it },
                            label = { Text(stringResource(R.string.settings_periods_per_day)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createSemester(
                        addName.ifBlank { "未命名学期" }, addDate,
                        (addWeeks.toIntOrNull() ?: 20).coerceIn(1, 30),
                        (addPeriods.toIntOrNull() ?: 12).coerceIn(1, 16)
                    )
                    addName = ""; addWeeks = "20"; addPeriods = "12"; addDate = ""; showAddForm = false; onImportClick()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddForm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    editingSemester?.let { s ->
        AlertDialog(
            onDismissRequest = { editingSemester = null },
            title = { Text(stringResource(R.string.settings_edit_semester)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        editName, { editName = it },
                        label = { Text(stringResource(R.string.settings_semester_name)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            editDate.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { d ->
                                c.set(d.year, d.monthValue - 1, d.dayOfMonth)
                            }
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d -> editDate = String.format("%04d-%02d-%02d", y, m + 1, d) },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(editDate.ifEmpty { stringResource(R.string.settings_semester_start_date) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            editWeeks, { editWeeks = it },
                            label = { Text(stringResource(R.string.settings_total_weeks)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            editPeriods, { editPeriods = it },
                            label = { Text(stringResource(R.string.settings_periods_per_day)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSemester(
                        s.copy(
                            name = editName.ifBlank { s.name }, startDate = editDate.ifBlank { s.startDate },
                            totalWeeks = (editWeeks.toIntOrNull() ?: s.totalWeeks).coerceIn(1, 30),
                            periodsPerDay = (editPeriods.toIntOrNull() ?: s.periodsPerDay).coerceIn(1, 16)
                        )
                    )
                    editingSemester = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { editingSemester = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deleteSemesterId?.let { id ->
        val name = semesters.find { it.id == id }?.name ?: id
        AlertDialog(
            onDismissRequest = { deleteSemesterId = null },
            title = { Text(stringResource(R.string.settings_confirm_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_semester_confirm, name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSemester(id); deleteSemesterId = null }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSemesterId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showImportConfirm && pendingImportData != null) {
        val d = pendingImportData!!
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImportData = null },
            title = { Text(stringResource(R.string.qr_import_title)) },
            text = { Text(stringResource(R.string.import_confirm_message, d.semester.name, d.courses.size)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportRaw?.let { viewModel.importFromScanResult(it) }
                    showImportConfirm = false; pendingImportData = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImportData = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    scanError?.let { err ->
        AlertDialog(
            onDismissRequest = { scanError = null },
            title = { Text(stringResource(R.string.qr_import_title)) },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { scanError = null }) {
                    Text(stringResource(R.string.schedule_dialog_close))
                }
            }
        )
    }

    // ── Update dialogs ─────────────────────────────────────────
    when (val state = updateState) {
        is UpdateResult.Checking -> {
            // Checking state is shown as a brief loading indicator;
            // we don't show a dialog here — the AboutItem click handles the trigger.
            // If checking takes too long, a Snackbar would be better.
        }

        is UpdateResult.Available -> {
            UpdateAvailableDialog(
                info = state.info,
                isDownloading = updateDownloading,
                downloadProgress = updateProgress,
                onDownload = { viewModel.downloadUpdate(state.info) },
                onDismiss = { viewModel.clearUpdateState() }
            )
        }

        is UpdateResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearUpdateState() },
                title = { Text(stringResource(R.string.settings_check_update)) },
                text = { Text(stringResource(R.string.update_uptodate)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearUpdateState() }) {
                        Text(stringResource(R.string.schedule_dialog_close))
                    }
                }
            )
        }

        is UpdateResult.Error -> {
            UpdateErrorDialog(
                message = state.message,
                onDismiss = { viewModel.clearUpdateState() }
            )
        }

        null -> { /* idle */ }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Reusable composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SemesterRow(
    semester: SemesterEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = semester.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.settings_active_semester),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = "${semester.startDate} · ${semester.totalWeeks}周 · ${semester.periodsPerDay}节/天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.settings_edit_semester),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.settings_delete_semester),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            else Color.Transparent
        )
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SettingsDimens.SectionBottom)) {
        SectionTitle(title = title)
        content()
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingsListItemColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val buttonColors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            inactiveContainerColor = MaterialTheme.colorScheme.surface,
            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
        SegmentedButton(
            selected = selectedMode == 0,
            onClick = { onModeSelected(0) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            colors = buttonColors,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_system),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
        SegmentedButton(
            selected = selectedMode == 1,
            onClick = { onModeSelected(1) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            colors = buttonColors,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_light),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
        SegmentedButton(
            selected = selectedMode == 2,
            onClick = { onModeSelected(2) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            colors = buttonColors,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme_dark),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AboutItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = SettingsListItemColors()
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
