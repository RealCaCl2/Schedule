package com.cacl2.schedule.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScanResultHolder {
    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun setResult(raw: String?) {
        _result.value = raw
    }

    fun consume(): String? {
        val v = _result.value
        _result.value = null
        return v
    }
}
