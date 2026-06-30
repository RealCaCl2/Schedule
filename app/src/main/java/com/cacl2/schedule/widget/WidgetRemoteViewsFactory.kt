package com.cacl2.schedule.widget

import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.cacl2.schedule.R

class WidgetRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var state: WidgetState = WidgetState(
        dayName = "", weekNumber = 0, dateLabel = "",
        courses = emptyList(), isEmpty = true
    )

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        try {
            val repo = WidgetRepository(context)
            state = repo.getTodayState()
        } catch (e: Exception) {
            Log.e("TodayWidget", "Failed to load course data", e)
            state = WidgetState(
                dayName = "", weekNumber = 0, dateLabel = "",
                courses = emptyList(), isEmpty = true
            )
        }
    }

    override fun getCount(): Int = if (state.isEmpty) 0 else state.courses.size

    override fun getViewAt(position: Int): RemoteViews {
        val course = state.courses[position]
        val views = RemoteViews(context.packageName, R.layout.widget_course_item)

        views.setTextViewText(R.id.widget_course_name, course.courseName)
        views.setTextViewText(
            R.id.widget_course_time,
            "${course.startPeriod}-${course.endPeriod}节"
        )

        val parts = mutableListOf<String>()
        if (course.location.isNotBlank()) parts.add(course.location)
        if (course.teacher.isNotBlank()) parts.add(course.teacher)
        views.setTextViewText(R.id.widget_course_location, parts.joinToString(" · "))

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
    override fun onDestroy() = Unit
}
