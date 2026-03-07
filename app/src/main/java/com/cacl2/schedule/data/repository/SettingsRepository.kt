package com.cacl2.schedule.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cacl2.schedule.model.ScheduleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val TOTAL_WEEKS = intPreferencesKey("total_weeks")
        val PERIODS_PER_DAY = intPreferencesKey("periods_per_day")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val SHOW_WEEKEND = booleanPreferencesKey("show_weekend")
        val QIANGZHI_URL = stringPreferencesKey("qiangzhi_url")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<ScheduleSettings> = context.dataStore.data.map { prefs ->
        ScheduleSettings(
            totalWeeks = prefs[Keys.TOTAL_WEEKS] ?: 20,
            periodsPerDay = prefs[Keys.PERIODS_PER_DAY] ?: 12,
            semesterStartDate = prefs[Keys.SEMESTER_START_DATE] ?: "",
            showWeekend = prefs[Keys.SHOW_WEEKEND] ?: true,
            qiangzhiUrl = prefs[Keys.QIANGZHI_URL] ?: ""
        )
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED]
            ?: ((prefs[Keys.QIANGZHI_URL]?.isNotBlank() == true) &&
                (prefs[Keys.SEMESTER_START_DATE]?.isNotBlank() == true))
    }

    suspend fun updateTotalWeeks(weeks: Int) {
        val sanitized = weeks.coerceIn(1, 30)
        context.dataStore.edit { it[Keys.TOTAL_WEEKS] = sanitized }
    }

    suspend fun updatePeriodsPerDay(periods: Int) {
        val sanitized = periods.coerceIn(1, 16)
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

    suspend fun completeOnboarding(
        totalWeeks: Int,
        periodsPerDay: Int,
        semesterStartDate: String,
        qiangzhiUrl: String
    ) {
        val sanitizedWeeks = totalWeeks.coerceIn(1, 30)
        val sanitizedPeriods = periodsPerDay.coerceIn(1, 16)
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
