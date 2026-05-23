package com.cacl2.schedule.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.repository.CourseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    courseId: Long,
    courseRepository: CourseRepository,
    onNavigateBack: () -> Unit,
    viewModel: CourseEditViewModel = viewModel(
        factory = CourseEditViewModel.Factory(courseRepository)
    )
) {
    val formState by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    var validationError by remember { mutableStateOf<EditValidationError?>(null) }

    var dayOfWeekExpanded by remember { mutableStateOf(false) }
    var weekTypeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        viewModel.loadCourse(courseId)
    }

    LaunchedEffect(saveState) {
        if (saveState is EditSaveState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (courseId == 0L) stringResource(R.string.edit_title_add)
                               else stringResource(R.string.edit_title_edit),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = formState.courseName,
                        onValueChange = { viewModel.updateCourseName(it) },
                        label = { Text(stringResource(R.string.edit_course_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = formState.teacher,
                        onValueChange = { viewModel.updateTeacher(it) },
                        label = { Text(stringResource(R.string.edit_teacher)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = formState.location,
                        onValueChange = { viewModel.updateLocation(it) },
                        label = { Text(stringResource(R.string.edit_location)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = dayOfWeekExpanded,
                        onExpandedChange = { dayOfWeekExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = dayOfWeekLabel(formState.dayOfWeek),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.edit_day_of_week)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayOfWeekExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dayOfWeekExpanded,
                            onDismissRequest = { dayOfWeekExpanded = false }
                        ) {
                            dayOfWeekOptions().forEach { (index, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.updateDayOfWeek(index)
                                        dayOfWeekExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.startPeriod,
                            onValueChange = { viewModel.updateStartPeriod(it) },
                            label = { Text(stringResource(R.string.edit_start_period)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.endPeriod,
                            onValueChange = { viewModel.updateEndPeriod(it) },
                            label = { Text(stringResource(R.string.edit_end_period)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.startWeek,
                            onValueChange = { viewModel.updateStartWeek(it) },
                            label = { Text(stringResource(R.string.edit_start_week)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.endWeek,
                            onValueChange = { viewModel.updateEndWeek(it) },
                            label = { Text(stringResource(R.string.edit_end_week)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = weekTypeExpanded,
                        onExpandedChange = { weekTypeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = weekTypeLabel(formState.weekType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.edit_week_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = weekTypeExpanded,
                            onDismissRequest = { weekTypeExpanded = false }
                        ) {
                            weekTypeOptions().forEach { (type, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.updateWeekType(type)
                                        weekTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            validationError?.let { error ->
                Text(
                    text = when (error) {
                        EditValidationError.CourseNameEmpty -> stringResource(R.string.edit_error_course_name_empty)
                        EditValidationError.InvalidDayOfWeek -> stringResource(R.string.edit_error_day_of_week)
                        EditValidationError.InvalidPeriodRange -> stringResource(R.string.edit_error_period_range)
                        EditValidationError.InvalidWeekRange -> stringResource(R.string.edit_error_week_range)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (saveState is EditSaveState.Error) {
                Text(
                    text = (saveState as EditSaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    validationError = viewModel.save()
                },
                enabled = saveState !is EditSaveState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_save))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun dayOfWeekLabel(day: Int): String = when (day) {
    1 -> stringResource(R.string.edit_day_monday)
    2 -> stringResource(R.string.edit_day_tuesday)
    3 -> stringResource(R.string.edit_day_wednesday)
    4 -> stringResource(R.string.edit_day_thursday)
    5 -> stringResource(R.string.edit_day_friday)
    6 -> stringResource(R.string.edit_day_saturday)
    7 -> stringResource(R.string.edit_day_sunday)
    else -> day.toString()
}

@Composable
private fun dayOfWeekOptions(): List<Pair<Int, String>> = listOf(
    1 to stringResource(R.string.edit_day_monday),
    2 to stringResource(R.string.edit_day_tuesday),
    3 to stringResource(R.string.edit_day_wednesday),
    4 to stringResource(R.string.edit_day_thursday),
    5 to stringResource(R.string.edit_day_friday),
    6 to stringResource(R.string.edit_day_saturday),
    7 to stringResource(R.string.edit_day_sunday)
)

@Composable
private fun weekTypeLabel(type: Int): String = when (type) {
    1 -> stringResource(R.string.edit_week_type_odd)
    2 -> stringResource(R.string.edit_week_type_even)
    else -> stringResource(R.string.edit_week_type_every)
}

@Composable
private fun weekTypeOptions(): List<Pair<Int, String>> = listOf(
    0 to stringResource(R.string.edit_week_type_every),
    1 to stringResource(R.string.edit_week_type_odd),
    2 to stringResource(R.string.edit_week_type_even)
)
