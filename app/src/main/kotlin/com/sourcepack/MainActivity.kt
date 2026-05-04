package com.sourcepack

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sourcepack.ui.*
import com.sourcepack.ui.theme.AppTheme
import com.sourcepack.viewmodel.MainVM
import com.sourcepack.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen on to prevent interruption during large file packing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            val vm: MainVM by viewModels() 
            val isDark by vm.isDark.collectAsStateWithLifecycle()
            
            AppTheme(darkTheme = isDark) { 
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(vm) 
                }
            }
        }
    }
}

@Composable
fun AppContent(vm: MainVM) {
    var page by remember { mutableStateOf(Page.HOME) }
    
    // Navigation logic
    BackHandler(page != Page.HOME) {
        if(page == Page.CONFIG_GEN || page == Page.CONFIG_BL) page = Page.CONFIG_ROOT else page = Page.HOME
    }
    
    // Page transition animation
    AnimatedContent(
        targetState = page, 
        label = "Nav",
        transitionSpec = {
            val animSpec = tween<IntOffset>(300)
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(animationSpec = animSpec) { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally(animationSpec = animSpec) { width -> -width / 3 } + fadeOut())
            } else {
                (slideInHorizontally(animationSpec = animSpec) { width -> -width / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally(animationSpec = animSpec) { width -> width } + fadeOut())
            }
        }
    ) { p ->
        when (p) {
            Page.HOME -> HomeScreen(vm, { page = Page.CONFIG_ROOT })
            Page.CONFIG_ROOT -> SettingsRoot(onBack = { page = Page.HOME }, onNav = { page = it }, vm = vm)
            Page.CONFIG_GEN -> GeneralSettings(vm, { page = Page.CONFIG_ROOT })
            Page.CONFIG_BL -> BlacklistSettings(vm, { page = Page.CONFIG_ROOT })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainVM, toCfg: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isDark by vm.isDark.collectAsStateWithLifecycle()
    val exportDir by vm.exportDir.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // --- State Management ---
    if (state is UiState.Loading) {
        // Prevent accidental back press during loading, double-tap to cancel
        var lastBackPressTime by remember { mutableLongStateOf(0L) }
        BackHandler {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                vm.cancelTask()
                Toast.makeText(context, "Operation cancelled", Toast.LENGTH_SHORT).show()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Press again to cancel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceUris by remember { mutableStateOf<List<Uri>?>(null) }
    var gitUrl by remember { mutableStateOf<String?>(null) }
    var projectName by remember { mutableStateOf("Project") }
    var mode by remember { mutableIntStateOf(0) } // 0:Folder, 1:Files, 2:Git

    // --- File Saver ---
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { dest ->
        if (dest != null) {
            when (mode) {
                0 -> sourceUri?.let { vm.packDirectly(it, dest) }
                1 -> sourceUris?.let { vm.packListDirectly(it, dest) }
                2 -> gitUrl?.let { vm.runGit(it, dest) }
            }
        }
    }

    fun startPacking() {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${projectName}_$time.md"

        if (exportDir != null) {
            // Auto save mode
            when (mode) {
                0 -> sourceUri?.let { vm.packDirectly(it, null, fileName) }
                1 -> sourceUris?.let { vm.packListDirectly(it, null, fileName) }
                2 -> gitUrl?.let { vm.runGit(it, null, fileName) }
            }
        } else {
            // Manual save mode
            saver.launch(fileName)
        }
    }

    // --- Core Pickers ---

    // 1. Folder Picker (Must use OpenDocumentTree for directory permissions)
    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            sourceUri = uri
            mode = 0
            val df = DocumentFile.fromTreeUri(context, uri)
            projectName = df?.name ?: "Project"
            startPacking()
        }
    }

    // 2. File Picker (Using GetMultipleContents / "Browser mode")
    // This can directly trigger third-party apps like MT Manager for a better experience
    val filePicker = rememberLauncherForActivityResult(GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            sourceUris = uris
            mode = 1
            projectName = "Selected_Files"
            startPacking()
        }
    }
    
    var showGitDialog by remember { mutableStateOf(false) }

    // --- UI Layout ---
    Scaffold { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentAlignment = Alignment.Center 
        ) {
            when (val s = state) {
                is UiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Button Bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp), 
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { vm.toggleTheme() }) {
                                Icon(if (isDark) Ico.Sun else Ico.Moon, "Theme", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = toCfg) {
                                Icon(Ico.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.height(80.dp))
                        Icon(Ico.Inventory2, "Logo", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("SourcePack", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(80.dp))

                        // Primary Function Entries
                        HomeBtn(Ico.Folder, "Project Folder", "Recursively scan the entire project. Ideal for helping AI understand architecture.") { 
                            // Direct launch, no additional permission check required here
                            dirPicker.launch(null) 
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        HomeBtn(Ico.File, "Select Files", "Precisely extract specific files. Uses system picker (supports MT Manager).") { 
                            // Start browser mode picker, passing */* for all files
                            filePicker.launch("*/*") 
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        HomeBtn(Ico.CloudDownload, "GitHub", "Download and parse remote repositories directly. Good for open source analysis.") { 
                            showGitDialog = true 
                        }
                        
                        Spacer(Modifier.height(48.dp))
                    }
                }

                is UiState.Loading -> LoadingView(msg = s.msg, detail = s.detail)
                is UiState.Success -> ResultCard(success = true, msg = s.info, onReset = { vm.reset() })
                is UiState.Error -> ResultCard(success = false, msg = s.err, onReset = { vm.reset() })
            }
        }
    }
    
    // Git Input Dialog
    if (showGitDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGitDialog = false },
            title = { Text("GitHub URL") },
            text = { 
                Column {
                    Text("Enter repository URL. We will download and build the full file tree structure.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(url, { url = it }, placeholder = { Text("https://github.com/user/repo") }, modifier = Modifier.fillMaxWidth()) 
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    if(url.length > 10) { 
                        gitUrl = url
                        mode = 2
                        showGitDialog = false
                        startPacking()
                    } 
                }) { Text("Next") } 
            }
        )
    }
}