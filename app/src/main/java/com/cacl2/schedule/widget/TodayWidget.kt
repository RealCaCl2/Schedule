package com.cacl2.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.cacl2.schedule.MainActivity
import com.cacl2.schedule.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TodayWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    val repo = WidgetRepository(context)
                    val state = repo.getTodayState()

                    val views = RemoteViews(context.packageName, R.layout.widget_today)

                    views.setTextViewText(R.id.widget_day_label,
                        "${state.dateLabel} ${state.dayName}")
                    views.setTextViewText(R.id.widget_week_label,
                        "第${state.weekNumber}周")

                    if (state.isEmpty) {
                        views.setTextViewText(R.id.widget_course_count, "")
                        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_courses_list, View.GONE)
                    } else {
                        val count = state.courses.size
                        views.setTextViewText(R.id.widget_course_count,
                            "${count}门课")
                        views.setViewVisibility(R.id.widget_empty, View.GONE)
                        views.setViewVisibility(R.id.widget_courses_list, View.VISIBLE)
                    }

                    // Set up the scrolling list adapter
                    val adapterIntent = Intent(context, WidgetRemoteViewsService::class.java)
                    views.setRemoteAdapter(R.id.widget_courses_list, adapterIntent)

                    // Refresh the list data (use array version, non-deprecated)
                    appWidgetManager.notifyAppWidgetViewDataChanged(
                        intArrayOf(appWidgetId), R.id.widget_courses_list
                    )

                    val tapIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    // Widget update failures are non-critical
                }
            }
        }
    }
}
