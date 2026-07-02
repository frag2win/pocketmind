package com.frag2win.pocketmind.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.domain.inference.GemmaVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelPreferences: ModelPreferences
) : ViewModel() {

    private val _selectedVariant = MutableStateFlow(
        modelPreferences.getSelectedVariant()?.let { GemmaVariant.valueOf(it) } ?: GemmaVariant.E2B
    )
    val selectedVariant = _selectedVariant.asStateFlow()

    private val _isAutoSelect = MutableStateFlow(modelPreferences.isAutoSelectEnabled())
    val isAutoSelect = _isAutoSelect.asStateFlow()

    fun updateVariant(variant: GemmaVariant) {
        _selectedVariant.value = variant
        modelPreferences.setSelectedVariant(variant)
        modelPreferences.setAutoSelectEnabled(false)
        _isAutoSelect.value = false
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
