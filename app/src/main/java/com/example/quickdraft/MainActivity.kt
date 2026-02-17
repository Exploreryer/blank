package com.example.quickdraft

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.quickdraft.ui.Destination
import com.example.quickdraft.ui.MainViewModel
import com.example.quickdraft.ui.editor.EditorScreen
import com.example.quickdraft.ui.settings.SettingsScreen
import com.example.quickdraft.ui.sidebar.HistoryDrawer
import com.example.quickdraft.ui.theme.BlankTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BlankTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val snackbarHost = remember { SnackbarHostState() }
                val uiState by viewModel.uiState.collectAsState()
                val notes by viewModel.notes.collectAsState()
                val selectedNote = viewModel.selectedNoteFrom(notes)
                val conflictCount = viewModel.conflictCount(notes)
                val app = application as BlankApp

                val signInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val outcome = app.container.authManager.handleSignInResult(result.data)
                    if (outcome.isSuccess) {
                        viewModel.refreshAuthState()
                        viewModel.triggerSyncNow()
                    } else {
                        Toast.makeText(this, "Google 登录失败", Toast.LENGTH_SHORT).show()
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    scrimColor = Color.Black.copy(alpha = 0.12f),
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.White,
                            drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        ) {
                            HistoryDrawer(
                                tab = uiState.selectedTab,
                                currentNotes = notes.first,
                                archivedNotes = notes.second,
                                onTabChange = viewModel::setTab,
                                onSelectNote = {
                                    viewModel.selectNote(it)
                                    scope.launch { drawerState.close() }
                                },
                                onToggleStar = viewModel::toggleStar,
                                onArchiveToggle = viewModel::toggleArchive,
                                onSettings = {
                                    viewModel.navigate(Destination.SETTINGS)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.White,
                                    scrolledContainerColor = Color.White
                                ),
                                title = {
                                    if (conflictCount > 0) {
                                        Text("存在 $conflictCount 条冲突副本", color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("Blank")
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = viewModel::createFreshNote) {
                                        Icon(Icons.Default.Add, contentDescription = "新建")
                                    }
                                    if (uiState.destination == Destination.EDITOR) {
                                        IconButton(
                                            onClick = {
                                                val copied = viewModel.editorContent(selectedNote)
                                                copyToClipboard(copied)
                                                scope.launch {
                                                    snackbarHost.showSnackbar("已复制到剪贴板")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                        }
                                    }
                                    IconButton(onClick = viewModel::triggerSyncNow) {
                                        Icon(Icons.Default.Sync, contentDescription = "立即同步")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        when (uiState.destination) {
                            Destination.EDITOR -> EditorScreen(
                                noteId = selectedNote?.id.orEmpty(),
                                content = viewModel.editorContent(selectedNote),
                                onContentChange = viewModel::onEditorChanged,
                                modifier = Modifier.padding(padding)
                            )

                            Destination.SETTINGS -> SettingsScreen(
                                isSyncEnabled = uiState.isSyncEnabled,
                                isSignedIn = uiState.isSignedIn,
                                signedInEmail = uiState.signedInEmail,
                                onSyncToggle = viewModel::setSyncEnabled,
                                onSignIn = { signInLauncher.launch(app.container.authManager.signInIntent()) },
                                onSignOut = {
                                    scope.launch {
                                        app.container.authManager.signOut()
                                        viewModel.refreshAuthState()
                                    }
                                },
                                appVersion = BuildConfig.VERSION_NAME
                            )
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(content: String) {
        val clipboard = getSystemService<android.content.ClipboardManager>() ?: return
        val clip = android.content.ClipData.newPlainText("note", content)
        clipboard.setPrimaryClip(clip)
    }
}
