package com.frag2win.pocketmind.ui.canvas

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val streamingPayload by viewModel.streamingRawPayload.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Canvas",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetCanvas() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Canvas",
                            tint = Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131314)
                )
            )
        },
        containerColor = Color(0xFF131314)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is CanvasUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "AI Canvas Offline Document Bridge",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Generate a presentation or structured document using Gemma, or trigger canvas mode from Chat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                is CanvasUiState.Generating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.Cyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Structuring Canvas JSON Payload...",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (streamingPayload.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFF1E1F20),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                Text(
                                    text = viewModel.sanitizeJsonPayload(streamingPayload),
                                    color = Color.Cyan,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                is CanvasUiState.Ready -> {
                    CanvasWebViewBridge(
                        jsonPayload = state.rawJsonPayload,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CanvasUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Canvas Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.message,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Embedded Android WebView acting as the secure rendering bridge for structured JSON.
 */
@Composable
fun CanvasWebViewBridge(
    jsonPayload: String,
    modifier: Modifier = Modifier
) {
    fun evaluateCanvasJs(webView: WebView, payload: String) {
        val escapedJson = payload
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val jsCall = """
            if (typeof window.renderCanvas === 'function') {
                window.renderCanvas('$escapedJson');
            } else {
                document.body.innerHTML = '<pre style="color:#22D3EE;padding:16px;">' + '$escapedJson' + '</pre>';
            }
        """.trimIndent()

        webView.evaluateJavascript(jsCall, null)
    }

    val htmlTemplate = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    background-color: #131314;
                    color: #FFFFFF;
                    font-family: system-ui, -apple-system, sans-serif;
                    margin: 0;
                    padding: 16px;
                }
                .slide-card {
                    background: #1E1F20;
                    border: 1px solid #2A2B2D;
                    border-radius: 12px;
                    padding: 16px;
                    margin-bottom: 16px;
                }
                h2 { color: #22D3EE; margin-top: 0; }
                ul { padding-left: 20px; color: #E3E3E3; }
                li { margin-bottom: 8px; }
            </style>
        </head>
        <body>
            <div id="canvas-container">
                <p style="color: #8E8E93;">Loading AI Canvas Bridge...</p>
            </div>
            <script>
                window.renderCanvas = function(jsonStr) {
                    const container = document.getElementById('canvas-container');
                    try {
                        const data = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
                        let html = '<h1>' + (data.title || 'AI Canvas') + '</h1>';
                        if (Array.isArray(data.slides)) {
                            data.slides.forEach((slide, idx) => {
                                html += '<div class="slide-card">';
                                html += '<h2>Slide ' + (idx + 1) + ': ' + (slide.title || '') + '</h2>';
                                if (Array.isArray(slide.bullets)) {
                                    html += '<ul>';
                                    slide.bullets.forEach(b => html += '<li>' + b + '</li>');
                                    html += '</ul>';
                                }
                                html += '</div>';
                            });
                        } else {
                            html += '<pre style="color:#22D3EE;">' + JSON.stringify(data, null, 2) + '</pre>';
                        }
                        container.innerHTML = html;
                    } catch (err) {
                        container.innerHTML = '<p style="color:#FF453A;">Invalid JSON Payload</p><pre>' + jsonStr + '</pre>';
                    }
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (view != null && jsonPayload.isNotBlank()) {
                            evaluateCanvasJs(view, jsonPayload)
                        }
                    }
                }
                loadDataWithBaseURL(
                    "https://app.pocketmind.canvas",
                    htmlTemplate,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            if (jsonPayload.isNotBlank()) {
                evaluateCanvasJs(webView, jsonPayload)
            }
        },
        modifier = modifier
    )
}
