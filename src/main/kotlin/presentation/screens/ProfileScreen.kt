package presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import auth.backup.GoogleDriveBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import presentation.theme.PizzaButtonColors
import session.CredentialsManager
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    val snackbarHostState = remember { SnackbarHostState() }
    val backupManager = remember { GoogleDriveBackupManager() }

    // Backup states
    var backupState by remember { mutableStateOf<BackupState>(BackupState.Idle) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var authCode by remember { mutableStateOf("") }
    var isGoogleDriveConfigured by remember { mutableStateOf(backupManager.isConfigured()) }

    // Network states
    var hasInternetConnection by remember { mutableStateOf(true) }
    var lastBackupError by remember { mutableStateOf<String?>(null) }

    // Auto-backup loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000 * 60) // 60 minutes interval

            isGoogleDriveConfigured = backupManager.isConfigured()
            if (!isGoogleDriveConfigured) return@LaunchedEffect

            hasInternetConnection = checkInternetConnection()
            if (!hasInternetConnection) {
                backupState = BackupState.Error("No internet connection")
                lastBackupError = "No internet connection"
                continue
            }

            try {
                backupState = BackupState.Loading
                backupManager.backupDatabase("pizza_pos.db")
                backupState = BackupState.Success
                lastBackupError = null
            } catch (e: Exception) {
                backupState = BackupState.Error(e.message ?: "Backup failed")
                lastBackupError = e.message
                println("Backup error: ${e.stackTraceToString()}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Credentials Section
            CredentialManagementSection(
                onLogout = onLogout,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )

            Spacer(Modifier.height(24.dp))
            Divider(Modifier.padding(vertical = 16.dp))

            // Backup Section
            BackupSection(
                isConfigured = isGoogleDriveConfigured,
                backupState = backupState,
                hasInternetConnection = hasInternetConnection,
                lastBackupError = lastBackupError,
                onManualBackupRequest = {
                    if (isGoogleDriveConfigured && hasInternetConnection) {
                        coroutineScope.launch {
                            try {
                                backupState = BackupState.Loading
                                backupManager.backupDatabase("pizza_pos.db")
                                backupState = BackupState.Success
                                lastBackupError = null
                            } catch (e: Exception) {
                                backupState = BackupState.Error(e.message ?: "Backup failed")
                                lastBackupError = e.message
                            }
                        }
                    }
                },
                onConfigureGoogleDrive = { showSetupWizard = true },
                onResetConfig = {
//                    backupManager.clearCredentials()
                    isGoogleDriveConfigured = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Google Drive configuration reset")
                    }
                }
            )

            // Setup Wizard Dialog
            if (showSetupWizard) {
                GoogleDriveSetupWizardDialog(
                    authCode = authCode,
                    onAuthCodeChange = { authCode = it },
                    onDismiss = { showSetupWizard = false },
                    onComplete = {
                        coroutineScope.launch {
                            try {
                                backupManager.completeAuthFlow(authCode)
                                isGoogleDriveConfigured = true
                                showSetupWizard = false
                                snackbarHostState.showSnackbar("Google Drive configured successfully!")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Authorization failed: ${e.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CredentialManagementSection(
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    var currentPassword by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Account Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; errorMessage = "" },
            label = { Text("Current Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(400.dp)
        )

        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it; errorMessage = "" },
            label = { Text("New Username") },
            modifier = Modifier.width(400.dp)
        )

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; errorMessage = "" },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(400.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = "" },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(400.dp),
            isError = errorMessage.isNotEmpty()
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                CredentialsManager.changeCredentials(newUsername, newPassword)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Credentials updated successfully")
                }
            },
            modifier = Modifier.width(200.dp)
        ) {
            Text("Save Changes")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.width(200.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun BackupSection(
    isConfigured: Boolean,
    backupState: BackupState,
    hasInternetConnection: Boolean,
    lastBackupError: String?,
    onManualBackupRequest: () -> Unit,
    onConfigureGoogleDrive: () -> Unit,
    onResetConfig: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Cloud Backup", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(16.dp))

        // Configuration status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Cloud Status",
                tint = if (isConfigured) Color.Green else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isConfigured) "Google Drive Linked" else "Not Configured",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        if (!isConfigured) {
            Button(
                onClick = onConfigureGoogleDrive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Configure")
                Spacer(Modifier.width(8.dp))
                Text("Configure Google Drive")
            }
        } else {
            // Connection status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasInternetConnection) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = "Internet Connection",
                    tint = if (hasInternetConnection) Color.Green else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasInternetConnection) "Connected" else "No Internet",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))

            when {
                !hasInternetConnection -> BackupStatus(
                    icon = Icons.Default.Error,
                    message = "Internet connection required for backup",
                    color = Color.Red
                )

                backupState is BackupState.Error -> BackupStatus(
                    icon = Icons.Default.Error,
                    message = "Backup failed: ${backupState.message}",
                    color = Color.Red
                )

                backupState == BackupState.Loading -> BackupStatus(
                    icon = null,
                    message = "Uploading backup to Google Drive...",
                    color = Color.Blue,
                    showProgress = true
                )

                backupState == BackupState.Success -> BackupStatus(
                    icon = Icons.Default.CloudDone,
                    message = "Backup completed successfully!",
                    color = Color.Green
                )

                else -> Button(
                    onClick = onManualBackupRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup")
                    Spacer(Modifier.width(8.dp))
                    Text("Backup Now")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onResetConfig,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.width(200.dp)
            ) {
                Text("Reset Configuration")
            }

            lastBackupError?.let {
                Text(
                    text = "Last error: $it",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BackupStatus(
    icon: Any?,
    message: String,
    color: Color,
    showProgress: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showProgress) {
            CircularProgressIndicator()
        } else if (icon != null) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = "Status",
                tint = color,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(message, color = color)
    }
}

@Composable
private fun GoogleDriveSetupWizardDialog(
    authCode: String,
    onAuthCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    Dialog(
        onCloseRequest = onDismiss,
        title = "Google Drive Setup"
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .height(IntrinsicSize.Max)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("1. Open authorization URL in your browser:")

                Button(
                    onClick = {
                        try {
                            val authUrl = "https://accounts.google.com/o/oauth2/auth?" +
                                    "response_type=code&" +
                                    "client_id=270968204688-fltmlegavuemi1pqkkt974ovlgklpa3c.apps.googleusercontent.com&" +
                                    "redirect_uri=http://localhost:8888&" +
                                    "scope=https://www.googleapis.com/auth/drive.file"

                            Desktop.getDesktop().browse(URI.create(authUrl))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Browser")
                }

                Text("2. Enter authorization code below:")

                OutlinedTextField(
                    value = authCode,
                    onValueChange = onAuthCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onComplete,
                    colors = PizzaButtonColors()
                ) {
                    Text("Complete Setup")
                }
            }
        }
    }
}

// Internet connection check
fun checkInternetConnection(): Boolean {
    return try {
        val timeoutMs = 1500
        val socket = java.net.Socket()
        val socketAddress = java.net.InetSocketAddress("8.8.8.8", 53)
        socket.connect(socketAddress, timeoutMs)
        socket.close()
        true
    } catch (e: IOException) {
        false
    }
}

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    object Success : BackupState()
    data class Error(val message: String) : BackupState()
}