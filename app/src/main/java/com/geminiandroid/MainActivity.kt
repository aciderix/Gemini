package com.geminiandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GeminiCliApp() }
    }
}

private data class ChatEntry(val role: String, val content: String)

private val slashCommands = listOf(
    "/auth", "/login", "/logout", "/status", "/help", "/model", "/models", "/config", "/profile", "/workspace",
    "/pwd", "/ls", "/cd", "/cat", "/edit", "/write", "/mkdir", "/rm", "/mv", "/cp",
    "/find", "/search", "/grep", "/run", "/shell", "/terminal", "/exec", "/python", "/npm", "/git",
    "/branch", "/commit", "/diff", "/test", "/lint", "/format", "/plan", "/apply", "/undo", "/redo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeminiCliApp() {
    val backend = remember { GeminiCliBackend() }
    val messages = remember { mutableStateListOf<ChatEntry>() }
    var input by remember { mutableStateOf("") }
    var cwd by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("http://10.0.2.2:8765") }
    var apiToken by remember { mutableStateOf("") }
    var showCommands by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini CLI Mobile") },
                actions = {
                    TextButton(onClick = { showCommands = true }) { Text("/") }
                    DropdownMenu(expanded = showCommands, onDismissRequest = { showCommands = false }) {
                        slashCommands.forEach { command ->
                            DropdownMenuItem(text = { Text(command) }, onClick = {
                                input = command
                                showCommands = false
                            })
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    backend.setBaseUrl(it)
                },
                label = { Text("Bridge URL") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("Token account /api") }
                )
                Button(onClick = {
                    scope.launch {
                        val response = withContext(Dispatchers.IO) { backend.login(apiToken) }
                        messages.add(ChatEntry("system", "login: $response"))
                    }
                }) { Text("Login") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = cwd,
                    onValueChange = { cwd = it },
                    label = { Text("Working directory") }
                )
                Button(onClick = {
                    scope.launch {
                        val response = withContext(Dispatchers.IO) { backend.accountStatus() }
                        messages.add(ChatEntry("system", "status: $response"))
                    }
                }) { Text("Status") }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Text(text = "${msg.role}: ${msg.content}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Prompt ou /commande") }
                )
                Button(onClick = {
                    val current = input.trim()
                    if (current.isBlank()) return@Button
                    messages.add(ChatEntry("you", current))
                    input = ""
                    scope.launch {
                        val response = withContext(Dispatchers.IO) { backend.send(current, cwd) }
                        messages.add(ChatEntry("gemini", response))
                    }
                }) { Text("Envoyer") }
            }
        }
    }
}
