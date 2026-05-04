package com.sourcepack.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sourcepack.data.*
import com.sourcepack.viewmodel.MainVM
import com.sourcepack.Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoot(onBack: () -> Unit, onNav: (Page) -> Unit, vm: MainVM) {
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Settings") }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Ico.ArrowBack, null) } }
            ) 
        }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            
            SettingHeader("General")
            SettingLink(Ico.Settings, "General Config", "Export path, format, compression, mode") { onNav(Page.CONFIG_GEN) }
            SettingLink(Ico.Delete, "Blacklist Management", "Manage ignore rules (Batch)") { onNav(Page.CONFIG_BL) }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneralSettings(vm: MainVM, back: () -> Unit) {
    val cfg by vm.cfg.collectAsStateWithLifecycle()
    val exportDir by vm.exportDir.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Restore standard system directory picker
    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.setExportDirectory(uri)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("General Configuration") }, navigationIcon = { IconButton(onClick = back) { Icon(Ico.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            
            SettingHeader("Export Location")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Default Save Directory", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    
                    val pathText = if (exportDir != null) {
                        // Try to parse display name
                        DocumentFile.fromTreeUri(context, exportDir!!)?.uri?.path ?: "Set (Content Uri)"
                    } else {
                        "Not set (Will ask for location every time)"
                    }
                    
                    Text(pathText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { dirPicker.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Select Folder")
                        }
                        
                        if (exportDir != null) {
                            OutlinedButton(onClick = { vm.setExportDirectory(null) }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            SettingHeader("Filtering")
            SwitchItem("Use .gitignore", cfg.useGitIgnore) { vm.saveCfg(cfg.copy(useGitIgnore = it)) }
            SwitchItem("Ignore .gradle folder", cfg.ignoreGradle) { vm.saveCfg(cfg.copy(ignoreGradle = it)) }
            SwitchItem("Ignore .git folder", cfg.ignoreGit) { vm.saveCfg(cfg.copy(ignoreGit = it)) }
            SwitchItem("Ignore build folder", cfg.ignoreBuild) { vm.saveCfg(cfg.copy(ignoreBuild = it)) }
            
            SettingHeader("Output Content")
            SwitchItem("Compress content (Remove line breaks)", cfg.compress) { vm.saveCfg(cfg.copy(compress = it)) }
            SwitchItem("Remove code comments", cfg.removeComments) { vm.saveCfg(cfg.copy(removeComments = it)) }
            
            SettingHeader("Output Format")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Format.values().forEach { f ->
                    FilterChip(
                        selected = cfg.format == f,
                        onClick = { vm.saveCfg(cfg.copy(format = f)) },
                        label = { Text(f.name) }
                    )
                }
            }

            SettingHeader("Output Mode")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mode.values().forEach { m ->
                    FilterChip(
                        selected = cfg.mode == m,
                        onClick = { vm.saveCfg(cfg.copy(mode = m)) },
                        label = { 
                            Text(when(m) {
                                Mode.FULL -> "Full Content"
                                Mode.TREE -> "Directory Tree Only"
                            })
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistSettings(vm: MainVM, back: () -> Unit) {
    val files by vm.uFiles.collectAsStateWithLifecycle()
    val exts by vm.uExts.collectAsStateWithLifecycle()
    var type by remember { mutableIntStateOf(0) } 
    val selectedItems = remember { mutableStateListOf<String>() }
    LaunchedEffect(type) { selectedItems.clear() }

    val currentList = (if(type==0) files else exts).toList().sorted()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    if (selectedItems.isEmpty()) Text("Blacklist") 
                    else Text("${selectedItems.size} Selected")
                },
                navigationIcon = { 
                    if (selectedItems.isEmpty()) IconButton(onClick = back) { Icon(Ico.ArrowBack, null) }
                    else IconButton(onClick = { selectedItems.clear() }) { Icon(Ico.UnselectAll, null) }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            vm.removeBlacklist(type, selectedItems.toList())
                            selectedItems.clear()
                        }) { Icon(Ico.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    } else {
                        IconButton(onClick = { 
                            selectedItems.clear()
                            selectedItems.addAll(currentList)
                        }) { Icon(Ico.SelectAll, null) }
                    }
                }
            ) 
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Ico.Add, null) } }
    ) { p ->
        Column(Modifier.padding(p)) {
            TabRow(selectedTabIndex = type) {
                Tab(selected = type==0, onClick = { type=0 }, text = { Text("Files/Folders") })
                Tab(selected = type==1, onClick = { type=1 }, text = { Text("Extensions (.ext)") })
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(currentList) { item ->
                    val isSel = item in selectedItems
                    ListItem(
                        modifier = Modifier.clickable { 
                            if(isSel) selectedItems.remove(item) else selectedItems.add(item) 
                        },
                        leadingContent = { Checkbox(checked = isSel, onCheckedChange = { if(it) selectedItems.add(item) else selectedItems.remove(item) }) },
                        headlineContent = { Text(item) }
                    )
                }
                if (currentList.isEmpty()) item { Text("Empty List", Modifier.padding(32.dp), color = Color.Gray) }
            }
        }
    }
    
    if (showAdd) {
        var txt by remember { mutableStateOf(if(type==1) "." else "") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Rule") },
            text = { 
                Column {
                    OutlinedTextField(txt, { txt=it }, singleLine = true, label = { Text(if(type==1) ".ext" else "Name") })
                    if(type==1 && !txt.startsWith(".")) Text("Must start with .", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    if(txt.isNotEmpty()) { 
                        vm.addBlacklist(type, listOf(txt))
                        showAdd = false 
                    } 
                }) { Text("Confirm") } 
            }
        )
    }
}