package com.cacl2.schedule.ui.import_

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.parser.QiangZhiParser
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.ScheduleSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Immutable
sealed class ImportState {
    data object Idle : ImportState()
    data object Extracting : ImportState()
    data class Parsed(val courses: List<CourseEntity>, val errors: List<String>) : ImportState()
    data object Saving : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

class ImportViewModel(
    private val courseRepository: CourseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ScheduleSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleSettings())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    fun onPageFinished(url: String) {
        _currentUrl.value = url
    }

    fun extractHtml(webView: WebView?) {
        val activeWebView = webView ?: run {
            _importState.value = ImportState.Error("WebView is not ready.")
            return
        }

        _importState.value = ImportState.Extracting

        activeWebView.evaluateJavascript(EXTRACT_SCHEDULE_HTML_SCRIPT) { result ->
            if (result == null || result == "null") {
                _importState.value = ImportState.Error("Unable to extract page HTML.")
                return@evaluateJavascript
            }

            val extraction = parseExtractionResult(result)
            if (extraction.html.isBlank()) {
                _importState.value = ImportState.Error("Extracted HTML is empty.")
                return@evaluateJavascript
            }

            viewModelScope.launch {
                parseAndProcess(extraction.html, extraction.source)
            }
        }
    }

    private suspend fun parseAndProcess(html: String, source: String) {
        withContext(Dispatchers.Default) {
            val parseResult = QiangZhiParser.parse(html)

            if (parseResult.courses.isEmpty()) {
                val detail = parseResult.errors.joinToString("\n").ifBlank {
                    "No course data was parsed. Make sure you are on the schedule page."
                }
                _importState.value = ImportState.Error("$detail\nSource: $source")
                return@withContext
            }

            _importState.value = ImportState.Parsed(parseResult.courses, parseResult.errors)
        }
    }

    fun confirmImport(courses: List<CourseEntity>) {
        viewModelScope.launch {
            _importState.value = ImportState.Saving
            try {
                courseRepository.replaceAll(courses)
                _importState.value = ImportState.Success(courses.size)
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Failed to save courses: ${e.message}")
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    private fun parseExtractionResult(result: String): ExtractionResult {
        val unescaped = unescapeJsString(result)

        return try {
            val json = JSONObject(unescaped)
            ExtractionResult(
                html = json.optString("html", ""),
                source = json.optString("source", "unknown")
            )
        } catch (_: Exception) {
            ExtractionResult(html = unescaped, source = "fallback-raw")
        }
    }

    private fun unescapeJsString(jsString: String): String {
        var s = jsString
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        return s
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\u003C", "<")
            .replace("\\u003c", "<")
            .replace("\\u003E", ">")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
            .replace("\\u0027", "'")
            .replace("\\u0022", "\"")
            .replace("\\/", "/")
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportViewModel(courseRepository, settingsRepository) as T
        }
    }

    private data class ExtractionResult(
        val html: String,
        val source: String
    )

    companion object {
        private const val EXTRACT_SCHEDULE_HTML_SCRIPT = """
            (function() {
              function docHtml(doc) {
                try {
                  return doc && doc.documentElement ? doc.documentElement.outerHTML : '';
                } catch (e) {
                  return '';
                }
              }

              var current = document;
              try {
                if (current.querySelector('#kbtable')) {
                  return JSON.stringify({ html: docHtml(current), source: 'current-kbtable' });
                }
              } catch (e) {}

              var iframes = current.querySelectorAll('iframe');
              for (var i = 0; i < iframes.length; i++) {
                var frameDoc = null;
                try {
                  frameDoc = iframes[i].contentDocument || (iframes[i].contentWindow && iframes[i].contentWindow.document);
                } catch (e) {
                  frameDoc = null;
                }
                if (!frameDoc) continue;

                try {
                  if (frameDoc.querySelector('#kbtable') || frameDoc.querySelector('div.kbcontent, div.kbcontent1')) {
                    return JSON.stringify({ html: docHtml(frameDoc), source: 'iframe-' + i });
                  }
                } catch (e) {}
              }

              return JSON.stringify({ html: docHtml(current), source: 'fallback-current' });
            })();
        """
    }
}
