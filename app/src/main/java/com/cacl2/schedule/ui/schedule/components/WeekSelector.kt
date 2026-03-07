package com.cacl2.schedule.ui.schedule.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cacl2.schedule.R
import com.cacl2.schedule.util.WeekUtils

@Composable
fun WeekSelector(
    currentWeek: Int,
    totalWeeks: Int,
    semesterStartDate: String,
    onWeekChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateRange = WeekUtils.getWeekDateRange(semesterStartDate, currentWeek)

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { if (currentWeek > 1) onWeekChange(currentWeek - 1) },
            enabled = currentWeek > 1
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.schedule_prev_week)
            )
        }

        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = currentWeek to dateRange,
                transitionSpec = {
                    fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(110))
                },
                label = "weekSelectorChange"
            ) { (week, range) ->
                Text(
                    text = if (range.isNotEmpty()) {
                        stringResource(R.string.schedule_week_with_range, week, range)
                    } else {
                        stringResource(R.string.schedule_week_label, week)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        IconButton(
            onClick = { if (currentWeek < totalWeeks) onWeekChange(currentWeek + 1) },
            enabled = currentWeek < totalWeeks
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.schedule_next_week)
            )
        }
    }
}
