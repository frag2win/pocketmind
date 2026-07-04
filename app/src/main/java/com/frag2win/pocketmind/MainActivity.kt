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

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF131314), // Gemini drawer dark background
                            modifier = Modifier.width(320.dp),
                            drawerShape = RoundedCornerShape(0.dp)
                        ) {
                            val viewModel: com.frag2win.pocketmind.ui.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            DrawerContent(
                                onNavigate = { route ->
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route)
                                },
                                onNewChat = {
                                    viewModel.startNewChat()
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
                                val viewModel: com.frag2win.pocketmind.ui.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerContent(onNavigate: (String) -> Unit, onNewChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131314))
            .padding(16.dp)
    ) {
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
            DrawerItem(Icons.Default.Search, "Search chats")

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Recents", 
                color = Color.White, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val recents = listOf(
                "PocketMind User Experience",
                "App Icon Design Concepts",
                "Generate DevOps PAT Token",
                "Video Editing Capabilities"
            )
            recents.forEach { title ->
                Text(
                    title,
                    color = Color.LightGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    maxLines = 1,
                    fontSize = 15.sp
                )
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
                Text(
                    "PRO", 
                    color = Color(0xFF6366F1), // Indigo color for PRO
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
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
