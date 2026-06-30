package com.cacl2.schedule.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cacl2.schedule.data.local.entity.SemesterEntity
import com.cacl2.schedule.model.ScheduleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        const val MAX_TOTAL_WEEKS = 30
        const val MIN_TOTAL_WEEKS = 1
        const val MAX_PERIODS_PER_DAY = 16
        const val MIN_PERIODS_PER_DAY = 1
    }

    private object Keys {
        val TOTAL_WEEKS = intPreferencesKey("total_weeks")
        val PERIODS_PER_DAY = intPreferencesKey("periods_per_day")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val SHOW_WEEKEND = booleanPreferencesKey("show_weekend")
        val QIANGZHI_URL = stringPreferencesKey("qiangzhi_url")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val ACTIVE_SEMESTER_ID = stringPreferencesKey("active_semester_id")
        val MIGRATION_V2_COMPLETED = booleanPreferencesKey("migration_v2_completed")
        val SHOW_TEACHER = booleanPreferencesKey("show_teacher")
        val SHOW_LOCATION = booleanPreferencesKey("show_location")
    }

    val settings: Flow<ScheduleSettings> = context.dataStore.data.map { prefs ->
        ScheduleSettings(
            totalWeeks = prefs[Keys.TOTAL_WEEKS] ?: 20,
            periodsPerDay = prefs[Keys.PERIODS_PER_DAY] ?: 12,
            semesterStartDate = prefs[Keys.SEMESTER_START_DATE] ?: "",
            showWeekend = prefs[Keys.SHOW_WEEKEND] ?: true,
            qiangzhiUrl = prefs[Keys.QIANGZHI_URL] ?: "",
            themeMode = prefs[Keys.THEME_MODE] ?: 0,
            activeSemesterId = prefs[Keys.ACTIVE_SEMESTER_ID] ?: "default",
            showTeacher = prefs[Keys.SHOW_TEACHER] ?: true,
            showLocation = prefs[Keys.SHOW_LOCATION] ?: true
        )
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED]
            ?: ((prefs[Keys.QIANGZHI_URL]?.isNotBlank() == true) &&
                (prefs[Keys.SEMESTER_START_DATE]?.isNotBlank() == true))
    }

    val activeSemesterId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SEMESTER_ID] ?: "default"
    }

    suspend fun setActiveSemester(id: String) {
        context.dataStore.edit { it[Keys.ACTIVE_SEMESTER_ID] = id }
    }

    suspend fun ensureDefaultSemesterIfNeeded(semesterRepository: SemesterRepository) {
        val completed = context.dataStore.data.first()[Keys.MIGRATION_V2_COMPLETED] ?: false
        if (!completed) {
            val onboardingDone = context.dataStore.data.first()[Keys.ONBOARDING_COMPLETED] ?: false
            val count = semesterRepository.count()
            // Only auto-create a default semester for existing users upgrading from v1.
            // For fresh installs, the onboarding flow creates the first semester.
            if (count == 0 && onboardingDone) {
                val prefs = context.dataStore.data.first()
                val startDate = prefs[Keys.SEMESTER_START_DATE]?.takeIf { it.isNotBlank() }
                    ?: java.time.LocalDate.now().toString()
                val totalWeeks = prefs[Keys.TOTAL_WEEKS] ?: 20
                val periodsPerDay = prefs[Keys.PERIODS_PER_DAY] ?: 12
                semesterRepository.insert(
                    SemesterEntity(
                        id = "default",
                        name = "默认学期",
                        startDate = startDate,
                        totalWeeks = totalWeeks,
                        periodsPerDay = periodsPerDay
                    )
                )
                // Also set "default" as active semester for upgrades
                context.dataStore.edit { it[Keys.ACTIVE_SEMESTER_ID] = "default" }
            }
            context.dataStore.edit { it[Keys.MIGRATION_V2_COMPLETED] = true }
        }
    }

    suspend fun updateTotalWeeks(weeks: Int) {
        val sanitized = weeks.coerceIn(MIN_TOTAL_WEEKS, MAX_TOTAL_WEEKS)
        context.dataStore.edit { it[Keys.TOTAL_WEEKS] = sanitized }
    }

    suspend fun updatePeriodsPerDay(periods: Int) {
        val sanitized = periods.coerceIn(MIN_PERIODS_PER_DAY, MAX_PERIODS_PER_DAY)
        context.dataStore.edit { it[Keys.PERIODS_PER_DAY] = sanitized }
    }

    suspend fun updateSemesterStartDate(date: String) {
        context.dataStore.edit { it[Keys.SEMESTER_START_DATE] = date.trim() }
    }

    suspend fun updateShowWeekend(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_WEEKEND] = show }
    }

    suspend fun updateQiangzhiUrl(url: String) {
        context.dataStore.edit { it[Keys.QIANGZHI_URL] = url.trim() }
    }

    suspend fun updateThemeMode(mode: Int) {
        val sanitized = mode.coerceIn(0, 2)
        context.dataStore.edit { it[Keys.THEME_MODE] = sanitized }
    }

    suspend fun updateShowTeacher(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TEACHER] = show }
    }

    suspend fun updateShowLocation(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LOCATION] = show }
    }

    suspend fun completeOnboarding(
        totalWeeks: Int,
        periodsPerDay: Int,
        semesterStartDate: String,
        qiangzhiUrl: String
    ) {
        val sanitizedWeeks = totalWeeks.coerceIn(MIN_TOTAL_WEEKS, MAX_TOTAL_WEEKS)
        val sanitizedPeriods = periodsPerDay.coerceIn(MIN_PERIODS_PER_DAY, MAX_PERIODS_PER_DAY)
        val sanitizedDate = semesterStartDate.trim()
        val sanitizedUrl = qiangzhiUrl.trim()

        context.dataStore.edit {
            it[Keys.TOTAL_WEEKS] = sanitizedWeeks
            it[Keys.PERIODS_PER_DAY] = sanitizedPeriods
            it[Keys.SEMESTER_START_DATE] = sanitizedDate
            it[Keys.QIANGZHI_URL] = sanitizedUrl
            it[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
