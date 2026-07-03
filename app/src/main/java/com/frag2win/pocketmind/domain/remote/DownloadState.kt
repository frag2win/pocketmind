package com.frag2win.pocketmind.domain.remote

/**
 * Represents the current state of a model download.
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}
