package com.termuxagent.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termuxagent.data.api.OpenAIClient
import com.termuxagent.data.settings.AppSettings
import com.termuxagent.data.settings.SettingsStore
import com.termuxagent.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val testing: Boolean = false,
    val testResult: String? = null
)

class SettingsViewModel(private val context: Context) : ViewModel() {
    private val _mutable = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _mutable.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsStore.flow(context).collect { s ->
                _mutable.update { it.copy(settings = s) }
            }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            SettingsStore.update(context, transform)
        }
    }

    fun setApiKey(v: String) = update { it.copy(apiKey = v) }
    fun setBaseUrl(v: String) = update { it.copy(baseUrl = v) }
    fun setModel(v: String) = update { it.copy(model = v) }
    fun setSystemPrompt(v: String) = update { it.copy(systemPrompt = v) }
    fun setMaxIterations(v: Int) = update { it.copy(maxIterations = v) }
    fun setTemperature(v: Float) = update { it.copy(temperature = v) }
    fun setThemeMode(v: ThemeMode) = update { it.copy(themeMode = v) }
    fun setDynamicColor(v: Boolean) = update { it.copy(dynamicColor = v) }

    fun testConnection() {
        val s = _mutable.value.settings
        if (!s.isConfigured) {
            _mutable.update { it.copy(testResult = "Set API key, base URL, and model first.") }
            return
        }
        _mutable.update { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            val client = OpenAIClient(baseUrl = s.baseUrl, apiKey = s.apiKey)
            val result = client.listModels()
            _mutable.update {
                it.copy(
                    testing = false,
                    testResult = result.fold(
                        onSuccess = { r -> "OK — ${r.data.size} models visible. Using: ${s.model}" },
                        onFailure = { e -> "Failed: ${e.message ?: e::class.simpleName}" }
                    )
                )
            }
        }
    }

    fun clearTestResult() {
        _mutable.update { it.copy(testResult = null) }
    }
}
