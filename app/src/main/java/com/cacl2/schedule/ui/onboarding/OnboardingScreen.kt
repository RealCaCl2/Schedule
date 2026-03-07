package com.cacl2.schedule.ui.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cacl2.schedule.R
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.util.QiangZhiUrlNormalizer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var totalWeeksDraft by remember { mutableStateOf("20") }
    var periodsPerDayDraft by remember { mutableStateOf("12") }
    var semesterStartDate by remember { mutableStateOf("") }
    var qiangzhiUrlDraft by remember { mutableStateOf("") }

    var totalWeeksError by remember { mutableStateOf<String?>(null) }
    var periodsError by remember { mutableStateOf<String?>(null) }
    var semesterDateError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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
            settingsRepository.completeOnboarding(
                totalWeeks = totalWeeks!!,
                periodsPerDay = periods!!,
                semesterStartDate = semesterStartDate,
                qiangzhiUrl = normalizedUrl!!
            )
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
                    OutlinedTextField(
                        value = qiangzhiUrlDraft,
                        onValueChange = {
                            qiangzhiUrlDraft = it
                            urlError = null
                        },
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
                    )                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

