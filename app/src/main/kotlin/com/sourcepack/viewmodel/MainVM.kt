package com.sourcepack.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sourcepack.core.*
import com.sourcepack.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class UiState {
    data object Idle : UiState()
    data class Loading(val msg: String, val detail: String = "") : UiState()
    data class Success(val info: String) : UiState()
    data class Error(val err: String) : UiState()
}

class MainVM(app: Application) : AndroidViewModel(app) {
    private val prefs = PreferenceManager(app)
    
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state = _state.asStateFlow()
    
    private val _cfg = MutableStateFlow(prefs.config)
    val cfg = _cfg.asStateFlow()
    
    private val _isDark = MutableStateFlow(prefs.isDarkTheme)
    val isDark = _isDark.asStateFlow()
    
    private val _uFiles = MutableStateFlow(prefs.getSet("u_files"))
    val uFiles = _uFiles.asStateFlow()
    private val _uExts = MutableStateFlow(prefs.getSet("u_exts"))
    val uExts = _uExts.asStateFlow()

    // Export path (SAF Uri only)
    private val _exportDir = MutableStateFlow<Uri?>(
        prefs.exportUriStr?.let { Uri.parse(it) }
    )
    val exportDir = _exportDir.asStateFlow()

    private var currentJob: Job? = null

    init {
        prefs.initDefaultsIfNeeded()
        _uFiles.value = prefs.getSet("u_files")
        _uExts.value = prefs.getSet("u_exts")
    }

    fun saveCfg(c: PackerConfig) { prefs.config = c; _cfg.value = c }
    fun toggleTheme() {
        val newMode = !_isDark.value
        prefs.isDarkTheme = newMode
        _isDark.value = newMode
    }
    fun reset() { _state.value = UiState.Idle }
    fun cancelTask() {
        currentJob?.cancel()
        _state.value = UiState.Idle
    }
    fun addBlacklist(type: Int, items: List<String>) {
        val key = if(type == 0) "u_files" else "u_exts"
        val current = prefs.getSet(key).toMutableSet()
        items.forEach { item ->
            val cleanItem = if(type == 1 && !item.startsWith(".")) ".$item" else item
            current.add(cleanItem)
        }
        prefs.updateSet(key, current)
        refreshLists()
    }
    fun removeBlacklist(type: Int, items: List<String>) {
        val key = if(type == 0) "u_files" else "u_exts"
        val current = prefs.getSet(key).toMutableSet()
        current.removeAll(items.toSet())
        prefs.updateSet(key, current)
        refreshLists()
    }
    private fun refreshLists() {
        _uFiles.value = prefs.getSet("u_files")
        _uExts.value = prefs.getSet("u_exts")
    }

    // Set export path (SAF Uri only)
    fun setExportDirectory(uri: Uri?) {
        if (uri == null) {
            prefs.exportUriStr = null
            _exportDir.value = null
            return
        }
        try {
            // Request persistable permission (remembers location after app restart)
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            prefs.exportUriStr = uri.toString()
            _exportDir.value = uri
        } catch (e: Exception) {
            // If failed (rare), still attempt to save
            prefs.exportUriStr = uri.toString()
            _exportDir.value = uri
        }
    }

    // Create file in the specified directory
    private fun createDestFile(fileName: String): Uri {
        val dirUri = _exportDir.value ?: throw IllegalStateException("Export path not set")
        
        // Use DocumentFile to handle SAF
        val dir = DocumentFile.fromTreeUri(getApplication(), dirUri)
        if (dir == null || !dir.canWrite()) {
            throw IllegalStateException("Export directory is not writable, please select again")
        }
        
        // Delete old file if it already exists
        dir.findFile(fileName)?.delete()
        
        val mimeType = "text/markdown"
        val newFile = dir.createFile(mimeType, fileName) 
            ?: throw IllegalStateException("Cannot create file, please check permissions")
        return newFile.uri
    }

    private val progressCb = object : SourcePacker.ProgressCallback {
        private var lastUpdate = 0L
        override fun onProgress(currentFile: String) {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 100) {
                lastUpdate = now
                val current = _state.value
                if (current is UiState.Loading) {
                    _state.value = current.copy(detail = currentFile)
                }
            }
        }
    }

    fun packDirectly(srcUri: Uri, destUri: Uri?, fileName: String? = null) {
        _state.value = UiState.Loading("Processing...")
        currentJob = viewModelScope.launch {
            try {
                // If destUri is provided (Manual mode), use it; otherwise create in default dir (Auto mode)
                val finalDest = destUri ?: createDestFile(fileName ?: "output.md")
                
                SourcePacker.packToStream(
                    getApplication(), srcUri, finalDest, 
                    _uFiles.value, _uExts.value, _cfg.value, progressCb
                )
                _state.value = UiState.Success("File saved to: ${finalDest.path}")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun packListDirectly(srcUris: List<Uri>, destUri: Uri?, fileName: String? = null) {
        _state.value = UiState.Loading("Processing...")
        currentJob = viewModelScope.launch {
            try {
                val finalDest = destUri ?: createDestFile(fileName ?: "output.md")
                SourcePacker.packListToStream(
                    getApplication(), srcUris, finalDest, _cfg.value, progressCb
                )
                _state.value = UiState.Success("File saved to: ${finalDest.path}")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun runGit(url: String, destUri: Uri?, fileName: String? = null) {
        var cleanUrl = url.trim().removeSuffix("/")
        if (cleanUrl.endsWith(".git")) cleanUrl = cleanUrl.removeSuffix(".git")
        if (!cleanUrl.contains("github.com")) {
            _state.value = UiState.Error("Invalid GitHub link")
            return
        }
        val path = cleanUrl.substringAfter("github.com/")
        val finalPath = if (path.contains("/tree/")) path.substringBefore("/tree/") else path
        val zipUrl = "https://github.com/$finalPath/archive/HEAD.zip"

        _state.value = UiState.Loading("Downloading repository...")
        currentJob = viewModelScope.launch {
            try {
                val finalDest = destUri ?: createDestFile(fileName ?: "repo_export.md")
                SourcePacker.packGitHubRepo(
                    zipUrl, finalDest, getApplication(),
                    _uFiles.value, _uExts.value, _cfg.value, progressCb
                )
                _state.value = UiState.Success("GitHub repository exported")
            } catch (e: CancellationException) {
                _state.value = UiState.Idle
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = UiState.Error("Error: ${e.message}")
            }
        }
    }
}