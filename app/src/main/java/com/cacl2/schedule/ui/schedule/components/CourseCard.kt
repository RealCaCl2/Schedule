package com.cacl2.schedule.ui.schedule.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacl2.schedule.R
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.ui.theme.CourseAccentColors
import com.cacl2.schedule.ui.theme.CourseColors
import com.cacl2.schedule.ui.theme.CourseDarkAccentColors
import com.cacl2.schedule.ui.theme.CourseDarkColors

@Composable
fun CourseCard(
    course: CourseEntity,
    cellHeight: Dp,
    dayWidth: Dp,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spanCount = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
    val cardHeight = cellHeight * spanCount - 2.dp
    val colorIndex = course.colorIndex.coerceIn(0, 9)

    val bgColor = if (isDarkTheme) CourseDarkColors[colorIndex] else CourseColors[colorIndex]
    val textColor = if (isDarkTheme) CourseDarkAccentColors[colorIndex] else CourseAccentColors[colorIndex]

    val nameMaxLines = when {
        spanCount >= 5 -> 11
        spanCount >= 4 -> 9
        spanCount >= 3 -> 7
        spanCount >= 2 -> 5
        else -> 3
    }
    val locationMaxLines = when {
        spanCount >= 5 -> 7
        spanCount >= 4 -> 6
        spanCount >= 3 -> 4
        spanCount >= 2 -> 3
        else -> 2
    }
    val teacherMaxLines = if (spanCount >= 4) 2 else 1

    val borderAlpha by animateFloatAsState(
        targetValue = if (isDarkTheme) 0.2f else 0.22f,
        label = "courseBorderAlpha"
    )

    val context = LocalContext.current
    val locationText = course.location.ifBlank { context.getString(R.string.settings_not_set) }
    val teacherText = course.teacher.ifBlank { context.getString(R.string.settings_not_set) }
    val cardContentDesc = stringResource(
        R.string.schedule_course_card_content_desc,
        course.courseName,
        locationText,
        course.startPeriod,
        course.endPeriod
    )
    val openDetailsLabel = stringResource(R.string.schedule_open_course_details)

    val cardShape = RoundedCornerShape(9.dp)

    Box(
        modifier = modifier
            .width(dayWidth - 2.dp)
            .height(cardHeight)
            .padding(horizontal = 0.5.dp, vertical = 1.dp)
            .clip(cardShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = borderAlpha),
                shape = cardShape
            )
            .semantics { contentDescription = cardContentDesc }
            .clickable(
                role = Role.Button,
                onClickLabel = openDetailsLabel,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = course.courseName,
                fontSize = if (spanCount >= 2) 11.5.sp else 10.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.5.sp,
                color = textColor,
                softWrap = true,
                maxLines = nameMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = locationText,
                fontSize = 10.5.sp,
                lineHeight = 11.5.sp,
                color = textColor.copy(alpha = 0.9f),
                softWrap = true,
                maxLines = locationMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = teacherText,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = textColor.copy(alpha = 0.78f),
                softWrap = true,
                maxLines = teacherMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
