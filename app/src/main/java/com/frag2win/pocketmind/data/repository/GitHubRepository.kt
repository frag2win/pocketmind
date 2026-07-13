package com.frag2win.pocketmind.data.repository

import com.frag2win.pocketmind.data.local.ModelPreferences
import com.frag2win.pocketmind.data.remote.GitHubContentDto
import com.frag2win.pocketmind.data.remote.GitHubRepoDto
import com.frag2win.pocketmind.data.remote.GitHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubRepository @Inject constructor(
    private val gitHubService: GitHubService,
    private val modelPreferences: ModelPreferences
) {
    fun hasToken(): Boolean = !modelPreferences.getGitHubToken().isNullOrBlank()

    private fun getAuthHeader(): String {
        val token = modelPreferences.getGitHubToken()
        return if (token.isNullOrBlank()) "" else "token $token"
    }

    suspend fun getRepositories(): List<GitHubRepoDto> = withContext(Dispatchers.IO) {
        try {
            gitHubService.getRepositories(getAuthHeader())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getDirectoryContents(owner: String, repo: String, path: String = ""): List<GitHubContentDto> = withContext(Dispatchers.IO) {
        try {
            gitHubService.getDirectoryContents(getAuthHeader(), owner, repo, path)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPullRequests(owner: String, repo: String): List<com.frag2win.pocketmind.data.remote.GitHubPullDto> = withContext(Dispatchers.IO) {
        try {
            gitHubService.getPullRequests(getAuthHeader(), owner, repo)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetches raw file content.
     * Uses Accept: application/vnd.github.v3.raw to get the plain text directly.
     */
    suspend fun getFileContent(owner: String, repo: String, path: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = gitHubService.getFileContent(
                getAuthHeader(),
                "application/vnd.github.v3.raw",
                owner,
                repo,
                path
            )
            response.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetches PR diff content.
     * Uses Accept: application/vnd.github.v3.diff to get the raw diff text.
     */
    suspend fun getPullRequestDiff(owner: String, repo: String, pullNumber: Int): String? = withContext(Dispatchers.IO) {
        try {
            val response = gitHubService.getPullRequestDiff(
                getAuthHeader(),
                "application/vnd.github.v3.diff",
                owner,
                repo,
                pullNumber
            )
            response.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
