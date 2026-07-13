package com.frag2win.pocketmind.ui.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frag2win.pocketmind.data.remote.GitHubContentDto
import com.frag2win.pocketmind.data.remote.GitHubRepoDto
import com.frag2win.pocketmind.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GitHubUiState {
    object Idle : GitHubUiState()
    object Loading : GitHubUiState()
    object NoToken : GitHubUiState()
    data class ReposLoaded(val repos: List<GitHubRepoDto>) : GitHubUiState()
    data class DirectoryLoaded(val owner: String, val repo: String, val path: String, val contents: List<GitHubContentDto>) : GitHubUiState()
    data class PullRequestsLoaded(val owner: String, val repo: String, val pulls: List<com.frag2win.pocketmind.data.remote.GitHubPullDto>) : GitHubUiState()
    data class Error(val message: String) : GitHubUiState()
}

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val repository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GitHubUiState>(GitHubUiState.Idle)
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()

    fun loadRepositories() {
        if (!repository.hasToken()) {
            _uiState.value = GitHubUiState.NoToken
            return
        }
        viewModelScope.launch {
            _uiState.value = GitHubUiState.Loading
            val repos = repository.getRepositories()
            if (repos.isNotEmpty()) {
                _uiState.value = GitHubUiState.ReposLoaded(repos)
            } else {
                _uiState.value = GitHubUiState.Error("No repositories found. Check your token in Settings.")
            }
        }
    }

    fun loadDirectory(owner: String, repo: String, path: String = "") {
        viewModelScope.launch {
            _uiState.value = GitHubUiState.Loading
            val contents = repository.getDirectoryContents(owner, repo, path)
            _uiState.value = GitHubUiState.DirectoryLoaded(owner, repo, path, contents)
        }
    }

    fun loadPullRequests(owner: String, repo: String) {
        viewModelScope.launch {
            _uiState.value = GitHubUiState.Loading
            val pulls = repository.getPullRequests(owner, repo)
            _uiState.value = GitHubUiState.PullRequestsLoaded(owner, repo, pulls)
        }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String): String? {
        return repository.getFileContent(owner, repo, path)
    }

    suspend fun getPRDiff(owner: String, repo: String, pullNumber: Int): String? {
        return repository.getPullRequestDiff(owner, repo, pullNumber)
    }
}
