package com.frag2win.pocketmind.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {
    @GET("user/repos")
    suspend fun getRepositories(
        @Header("Authorization") token: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): List<GitHubRepoDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): ResponseBody

    @GET("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun getPullRequestDiff(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int
    ): ResponseBody

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getDirectoryContents(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): List<GitHubContentDto>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open"
    ): List<GitHubPullDto>
}

data class GitHubRepoDto(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val html_url: String,
    val updated_at: String
)

data class GitHubContentDto(
    val name: String,
    val path: String,
    val type: String, // "file" or "dir"
    val size: Long,
    val download_url: String?
)

data class GitHubPullDto(
    val number: Int,
    val title: String,
    val user: GitHubUserDto,
    val body: String?,
    val html_url: String
)

data class GitHubUserDto(
    val login: String,
    val avatar_url: String
)
