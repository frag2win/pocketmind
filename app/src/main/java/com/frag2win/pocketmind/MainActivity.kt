package com.frag2win.pocketmind
// Dummy comment to trigger GitHub Action

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.frag2win.pocketmind.ui.chat.ChatScreenRoot
import com.frag2win.pocketmind.ui.github.GitHubScreen
import com.frag2win.pocketmind.ui.settings.SettingsScreen
import com.frag2win.pocketmind.ui.theme.PocketMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketMindTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val viewModel: com.frag2win.pocketmind.ui.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF131314), // Gemini drawer dark background
                            modifier = Modifier.width(320.dp),
                            drawerShape = RoundedCornerShape(0.dp)
                        ) {
                            val sessions by viewModel.sessions.collectAsState()
                            val searchQuery by viewModel.searchQuery.collectAsState()
                            val searchResults by viewModel.searchResults.collectAsState()

                            DrawerContent(
                                onNavigate = { route ->
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route)
                                },
                                onNewChat = {
                                    viewModel.startNewChat()
                                    scope.launch { drawerState.close() }
                                },
                                sessions = sessions,
                                onSessionClick = { sessionId ->
                                    viewModel.selectSession(sessionId)
                                    scope.launch { drawerState.close() }
                                },
                                onSessionDelete = { sessionId ->
                                    viewModel.deleteSession(sessionId)
                                },
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                                onResultClick = { message ->
                                    viewModel.selectSession(message.sessionId)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "chat",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("chat") {
                                ChatScreenRoot(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = viewModel,
                                    onMenuClick = {
                                        scope.launch { drawerState.open() }
                                    }
                                )
                            }
                            composable("settings") {
                                SettingsScreen()
                            }
                            composable("github") {
                                GitHubScreen(
                                    onAnalyzeFile = { path, content ->
                                        viewModel.sendGitHubMessage(
                                            uiDisplay = "Analyze file: $path",
                                            actualPrompt = com.frag2win.pocketmind.domain.inference.PromptBuilder.buildCodeAnalysisPrompt(path, content)
                                        )
                                        navController.navigate("chat")
                                    },
                                    onAnalyzePR = { owner, repo, num, diff ->
                                        viewModel.sendGitHubMessage(
                                            uiDisplay = "Summarize PR #$num for $owner/$repo",
                                            actualPrompt = com.frag2win.pocketmind.domain.inference.PromptBuilder.buildPRSummaryPrompt(owner, repo, num, diff)
                                        )
                                        navController.navigate("chat")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    onNavigate: (String) -> Unit, 
    onNewChat: () -> Unit,
    sessions: List<com.frag2win.pocketmind.data.local.ChatSession> = emptyList(),
    onSessionClick: (Int) -> Unit = {},
    onSessionDelete: (Int) -> Unit = {},
    searchQuery: String = "",
    searchResults: List<com.frag2win.pocketmind.data.local.ChatMessage> = emptyList(),
    onSearchQueryChange: (String) -> Unit = {},
    onResultClick: (com.frag2win.pocketmind.data.local.ChatMessage) -> Unit = {}
) {
    var searchActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131314))
            .padding(16.dp)
    ) {
        // Search Bar at the top of the sidebar
        DockedSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { searchActive = false },
            active = searchActive,
            onActiveChange = { searchActive = it },
            placeholder = { Text("Search chats...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            colors = SearchBarDefaults.colors(
                containerColor = Color(0xFF1E1F20),
                inputFieldColors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(searchResults) { result ->
                    ListItem(
                        headlineContent = { Text(result.content, color = Color.White, maxLines = 1) },
                        supportingContent = { Text(if (result.role == "user") "You" else "AI", color = Color.Gray) },
                        leadingContent = { Icon(if (result.role == "user") Icons.Default.Person else Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Cyan) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onResultClick(result)
                            searchActive = false
                        }
                    )
                }
            }
        }

        Text(
            "PocketMind",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            onClick = { 
                onNewChat()
                onNavigate("chat") 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1F20)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("New chat", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = { onNavigate("github") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("GitHub Browser", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Recents", 
                color = Color.White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(sessions) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionClick(session.id) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            session.title,
                            color = Color.LightGray,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            fontSize = 15.sp
                        )
                        IconButton(
                            onClick = { onSessionDelete(session.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Chat",
                                tint = Color.Gray.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2A2B2D), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22D3EE)) // Bright cyan for profile
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "shubham pawar", 
                    color = Color.White, 
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            IconButton(onClick = { onNavigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}
