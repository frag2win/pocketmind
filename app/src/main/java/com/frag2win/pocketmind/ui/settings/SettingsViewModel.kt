package com.frag2win.pocketmind.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.data.repository.ModelDownloadRepository
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import com.frag2win.pocketmind.domain.remote.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelPreferences: ModelPreferences,
    private val downloadRepository: ModelDownloadRepository
) : ViewModel() {

    private val _isAutoSelect = MutableStateFlow(modelPreferences.isAutoSelectEnabled())
    val isAutoSelect = _isAutoSelect.asStateFlow()

    private val _selectedVariant = MutableStateFlow(
        modelPreferences.getSelectedVariant()?.let { GemmaVariant.valueOf(it) } 
            ?: if (_isAutoSelect.value) getAutoRecommendedVariant() else GemmaVariant.E2B
    )
    val selectedVariant = _selectedVariant.asStateFlow()

    private val _displayName = MutableStateFlow(modelPreferences.getDisplayName())
    val displayName = _displayName.asStateFlow()

    val userName = displayName

    private val _hfToken = MutableStateFlow(modelPreferences.getHfToken() ?: "")
    val hfToken = _hfToken.asStateFlow()

    private val _tavilyApiKey = MutableStateFlow(modelPreferences.getTavilyApiKey() ?: "")
    val tavilyApiKey = _tavilyApiKey.asStateFlow()

    private val _githubToken = MutableStateFlow(modelPreferences.getGitHubToken() ?: "")
    val githubToken = _githubToken.asStateFlow()

    private val _downloadStatuses = MutableStateFlow<Map<GemmaVariant, DownloadState>>(
        GemmaVariant.values().associateWith { variant ->
            if (downloadRepository.isModelDownloaded(variant)) DownloadState.Completed 
            else DownloadState.Idle
        }
    )
    val downloadStatuses = _downloadStatuses.asStateFlow()

    fun updateVariant(variant: GemmaVariant) {
        _selectedVariant.value = variant
        modelPreferences.setSelectedVariant(variant)
        modelPreferences.setAutoSelectEnabled(false)
        _isAutoSelect.value = false
    }

    fun updateDisplayName(name: String) {
        _displayName.value = name
        modelPreferences.setDisplayName(name)
    }

    fun updateUserName(name: String) = updateDisplayName(name)

    fun updateHfToken(token: String) {
        _hfToken.value = token
        modelPreferences.setHfToken(token)
    }

    fun updateTavilyApiKey(key: String) {
        _tavilyApiKey.value = key
        modelPreferences.setTavilyApiKey(key)
    }

    fun updateGitHubToken(token: String) {
        _githubToken.value = token
        modelPreferences.setGitHubToken(token)
    }

    fun downloadModel(variant: GemmaVariant) {
        viewModelScope.launch {
            downloadRepository.downloadModel(variant, variant.downloadUrl).collect { state ->
                _downloadStatuses.value = _downloadStatuses.value.toMutableMap().apply {
                    put(variant, state)
                }
            }
        }
    }

    fun deleteModel(variant: GemmaVariant) {
        downloadRepository.deleteModel(variant)
        _downloadStatuses.value = _downloadStatuses.value.toMutableMap().apply {
            put(variant, DownloadState.Idle)
        }
    }

    fun setAutoSelect(enabled: Boolean) {
        _isAutoSelect.value = enabled
        modelPreferences.setAutoSelectEnabled(enabled)
        if (enabled) {
            val autoVariant = getAutoRecommendedVariant()
            _selectedVariant.value = autoVariant
            modelPreferences.setSelectedVariant(autoVariant)
        }
    }

    fun getAvailableRamGb(): Double {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0)
    }

    fun getAutoRecommendedVariant(): GemmaVariant {
        val ram = getAvailableRamGb()
        return when {
            ram >= 6.0 -> GemmaVariant.E4B
            ram >= 3.5 -> GemmaVariant.E2B
            else -> GemmaVariant.E2B_INT4
        }
    }
}
