package com.frag2win.pocketmind.ui.github

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frag2win.pocketmind.data.remote.GitHubContentDto
import com.frag2win.pocketmind.data.remote.GitHubRepoDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(
    viewModel: GitHubViewModel = hiltViewModel(),
    onAnalyzeFile: (String, String) -> Unit, // path, content
    onAnalyzePR: (String, String, Int, String) -> Unit, // owner, repo, num, diff
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadRepositories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Browser") },
                navigationIcon = {
                    if (uiState !is GitHubUiState.ReposLoaded && uiState !is GitHubUiState.Loading && uiState !is GitHubUiState.Idle && uiState !is GitHubUiState.NoToken) {
                        IconButton(onClick = { viewModel.loadRepositories() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is GitHubUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is GitHubUiState.NoToken -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No GitHub Token Found", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Please add a Personal Access Token in Settings to browse your repositories.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(onClick = onNavigateToSettings, modifier = Modifier.padding(top = 24.dp)) {
                            Text("Go to Settings")
                        }
                    }
                }
                is GitHubUiState.ReposLoaded -> RepoList(state.repos) { repo ->
                    viewModel.loadDirectory(repo.full_name.split("/")[0], repo.name)
                }
                is GitHubUiState.DirectoryLoaded -> {
                    Column {
                        // Virtual PR entry if we are at root
                        if (state.path.isEmpty()) {
                            ListItem(
                                headlineContent = { Text("Pull Requests", fontWeight = FontWeight.Bold) },
                                leadingContent = { Icon(Icons.Default.Merge, contentDescription = null, tint = Color.Magenta) },
                                modifier = Modifier.clickable { viewModel.loadPullRequests(state.owner, state.repo) }
                            )
                            Divider()
                        }
                        
                        ContentList(
                            state.contents,
                            onDirClick = { dir -> viewModel.loadDirectory(state.owner, state.repo, dir.path) },
                            onFileClick = { file ->
                                scope.launch {
                                    val content = viewModel.getFileContent(state.owner, state.repo, file.path)
                                    if (content != null) {
                                        onAnalyzeFile(file.path, content)
                                    }
                                }
                            }
                        )
                    }
                }
                is GitHubUiState.PullRequestsLoaded -> PullRequestList(
                    state.pulls,
                    onPullClick = { pull ->
                        scope.launch {
                            val diff = viewModel.getPRDiff(state.owner, state.repo, pull.number)
                            if (diff != null) {
                                onAnalyzePR(state.owner, state.repo, pull.number, diff)
                            }
                        }
                    }
                )
                is GitHubUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                else -> {}
            }
        }
    }
}

@Composable
fun PullRequestList(
    pulls: List<com.frag2win.pocketmind.data.remote.GitHubPullDto>,
    onPullClick: (com.frag2win.pocketmind.data.remote.GitHubPullDto) -> Unit
) {
    if (pulls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No open pull requests found.")
        }
    } else {
        LazyColumn {
            items(pulls) { pull ->
                ListItem(
                    headlineContent = { Text("#${pull.number} ${pull.title}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("by ${pull.user.login}") },
                    leadingContent = { Icon(Icons.Default.Merge, contentDescription = null, tint = Color.Magenta) },
                    trailingContent = {
                        IconButton(onClick = { onPullClick(pull) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summarize", tint = Color(0xFF6366F1))
                        }
                    },
                    modifier = Modifier.clickable { /* Future: Open details */ }
                )
                Divider()
            }
        }
    }
}

@Composable
fun RepoList(repos: List<GitHubRepoDto>, onRepoClick: (GitHubRepoDto) -> Unit) {
    LazyColumn {
        items(repos) { repo ->
            ListItem(
                headlineContent = { Text(repo.name, fontWeight = FontWeight.Bold) },
                supportingContent = { Text(repo.description ?: "No description") },
                leadingContent = { Icon(Icons.Default.Book, contentDescription = null) },
                modifier = Modifier.clickable { onRepoClick(repo) }
            )
            Divider()
        }
    }
}

@Composable
fun ContentList(
    contents: List<GitHubContentDto>,
    onDirClick: (GitHubContentDto) -> Unit,
    onFileClick: (GitHubContentDto) -> Unit
) {
    LazyColumn {
        items(contents) { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                leadingContent = {
                    Icon(
                        if (item.type == "dir") Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (item.type == "dir") Color(0xFFFACC15) else Color.Gray
                    )
                },
                trailingContent = {
                    if (item.type == "file") {
                        IconButton(onClick = { onFileClick(item) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Explain", tint = Color(0xFF6366F1))
                        }
                    }
                },
                modifier = Modifier.clickable {
                    if (item.type == "dir") onDirClick(item) else { /* Future: Open viewer */ }
                }
            )
            Divider()
        }
    }
}
