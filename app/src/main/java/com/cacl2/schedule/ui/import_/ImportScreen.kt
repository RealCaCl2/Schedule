package com.cacl2.schedule.ui.import_

import android.webkit.WebView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cacl2.schedule.R
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.ui.import_.components.QiangZhiWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    courseRepository: CourseRepository,
    settingsRepository: SettingsRepository,
    onImportSuccess: () -> Unit,
    viewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.Factory(courseRepository, settingsRepository)
    )
) {
    val settings by viewModel.settings.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var expectNavigationOnSuccess by remember { mutableStateOf(false) }
    var lastStateWasSaving by remember { mutableStateOf(false) }
    var showGuideTip by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Saving -> {
                lastStateWasSaving = true
            }

            is ImportState.Success -> {
                val shouldNavigate = expectNavigationOnSuccess && lastStateWasSaving
                expectNavigationOnSuccess = false
                lastStateWasSaving = false

                if (shouldNavigate) {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.import_success, state.count)
                    )
                    viewModel.resetState()
                    onImportSuccess()
                } else {
                    viewModel.resetState()
                }
            }

            is ImportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                expectNavigationOnSuccess = false
                lastStateWasSaving = false
                viewModel.resetState()
            }

            else -> {
                if (state !is ImportState.Parsed) {
                    lastStateWasSaving = false
                }
            }
        }
    }

    val statusText = when (importState) {
        is ImportState.Idle -> {
            if (currentUrl.isBlank()) {
                stringResource(R.string.import_status_login_first)
            } else {
                val shownUrl = if (currentUrl.length > 42) "${currentUrl.take(42)}..." else currentUrl
                stringResource(R.string.import_status_loaded_url, shownUrl)
            }
        }

        is ImportState.Extracting -> stringResource(R.string.import_status_extracting)
        is ImportState.Parsed -> stringResource(
            R.string.import_status_parsed,
            (importState as ImportState.Parsed).courses.size
        )

        is ImportState.Saving -> stringResource(R.string.import_status_saving)
        is ImportState.Success -> stringResource(R.string.import_status_done)
        is ImportState.Error -> stringResource(
            R.string.import_status_error,
            (importState as ImportState.Error).message
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.import_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.import_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.extractHtml(webViewRef) },
                        enabled = importState is ImportState.Idle || importState is ImportState.Error
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.import_extract)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animateContentSize()
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (importState is ImportState.Extracting || importState is ImportState.Saving) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val loadUrl = settings.qiangzhiUrl.ifBlank { "about:blank" }

            QiangZhiWebView(
                url = loadUrl,
                onWebViewCreated = { webViewRef = it },
                onPageFinished = { viewModel.onPageFinished(it) },
                modifier = Modifier.fillMaxSize(),
                onWebViewDisposed = { disposed ->
                    if (webViewRef === disposed) {
                        webViewRef = null
                    }
                }
            )

            if (showGuideTip) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.import_status_login_first),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showGuideTip = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.schedule_dialog_close)
                            )
                        }
                    }
                }
            }

            if (importState is ImportState.Extracting || importState is ImportState.Saving) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (importState is ImportState.Parsed) {
        val parsed = importState as ImportState.Parsed
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.import_confirm_count, parsed.courses.size))
                    if (parsed.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.import_confirm_warning)}\n${parsed.errors.joinToString("\n")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.import_confirm_replace_tip),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    expectNavigationOnSuccess = true
                    viewModel.confirmImport(parsed.courses)
                }) {
                    Text(stringResource(R.string.import_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}




