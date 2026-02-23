package com.mintanable.notethepad.feature_settings.presentation

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mintanable.notethepad.core.worker.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.use_case.CancelScheduledBackupUsecase
import com.mintanable.notethepad.feature_backup.domain.use_case.CheckForExistingBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.DownloadBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.ScheduleBackupUseCase
import com.mintanable.notethepad.feature_backup.presentation.BackupStatus
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_backup.presentation.RestoreEvent
import com.mintanable.notethepad.feature_backup.presentation.UploadDownload
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_note.domain.model.Note
import com.mintanable.notethepad.feature_note.domain.model.NoteColors
import com.mintanable.notethepad.feature_note.domain.use_case.NoteUseCases
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.data.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val driveRepository: GoogleAuthRepository,
    private val scheduleBackup: ScheduleBackupUseCase,
    private val cancelScheduledBackup: CancelScheduledBackupUsecase,
    private val checkForExistingBackup: CheckForExistingBackup,
    private val workManager: WorkManager,
    private val noteUseCases: NoteUseCases,
    private val downloadBackup: DownloadBackup
) : ViewModel() {

    private val _backupUploadStatus = workManager
        .getWorkInfosForUniqueWorkFlow(BackupSchedulerImpl.WORK_NAME)
        .map { list ->
            val workInfo = list.firstOrNull()
            Log.d("kptest", "upload status :${workInfo?.state}")

            when {
                workInfo == null -> BackupStatus.Idle
                workInfo.state == WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt("percent", 0)
                    Log.d("kptest", "upload status progress:$progress")
                    BackupStatus.Progress(progress, UploadDownload.UPLOAD)
                }
                workInfo.state == WorkInfo.State.SUCCEEDED ->{
                    Log.d("kptest", "upload successful")
                    BackupStatus.Success
                }
                workInfo.state == WorkInfo.State.FAILED -> BackupStatus.Error("Background backup failed")
                else -> BackupStatus.Idle
            }
        }
        .distinctUntilChanged()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackupStatus.Idle
    )

    private val _backupDownloadState = MutableStateFlow<BackupStatus>(BackupStatus.Idle)

    val backupUploadDownloadState: StateFlow<BackupStatus> = combine(
        _backupUploadStatus,
        _backupDownloadState
    ){ upload, download ->
        when{
            download is BackupStatus.Progress -> download
            upload is BackupStatus.Progress -> upload
            download is BackupStatus.Success || download is BackupStatus.Error -> download
            else -> upload
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackupStatus.Idle
    )

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val backupUiState = _backupUiState.asStateFlow()

    private val _restoreEvents = MutableSharedFlow<RestoreEvent>()
    val restoreEvents = _restoreEvents.asSharedFlow()

    val settingsState: StateFlow<Settings> = combine(
        authRepository.getSignedInFirebaseUser(),
        dataStore.settingsFlow
    ) { user, settings ->
        val googleEmail = if(user?.isGoogleSignedIn==true) user.email else null
        settings.copy(googleAccount=googleEmail)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _isProcessingBackupToggle = MutableStateFlow(false)
    val isAuthorisingBackup = _isProcessingBackupToggle.asStateFlow()

    val isGoogleSignedIn = authRepository.isUserSignedInWithGoogle()

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateNotifications(enabled) }
    }

    fun updateBackupTime(hours: Int, minutes: Int) {
        viewModelScope.launch { dataStore.updateBackupTime(hours, minutes) }
    }

    fun updateBackupFrequency(backupFrequency: BackupFrequency) {
        viewModelScope.launch { dataStore.updateBackupFrequency(backupFrequency) }
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch { dataStore.updateTheme(mode) }
    }

    fun toggleBackup(enabled: Boolean, onAuthRequired: (PendingIntent) -> Unit, onFailure:(String)->Unit) {
        viewModelScope.launch {
            if (!enabled) {
                dataStore.updateBackup(false)
                return@launch
            }

            if (driveRepository.hasDriveAccess()) {
                dataStore.updateBackup(true)
            } else {
                val email = settingsState.value.googleAccount
                if (email != null) {
                    driveRepository.authorizeDriveAccess(email)
                        .onSuccess { result ->
                            Log.d("kptest", "Has Resolution: ${result.hasResolution()}")
                            if (result.hasResolution()) {
                                result.pendingIntent?.let { onAuthRequired(it) }
                                _isProcessingBackupToggle.value = false
                            } else {
                                Log.d("kptest", "Attempting Silent Exchange")
                                exchangeCodeForTokens(result.serverAuthCode, onFailure)
                                _isProcessingBackupToggle.value = false
                            }
                        }
                        .onFailure { exception ->
                            _isProcessingBackupToggle.value = false
                            val errorMessage = exception.localizedMessage ?: exception.message ?: "Authorization failed"
                            onFailure(errorMessage)
                        }
                }
            }
        }
    }

    private suspend fun exchangeCodeForTokens(code: String?, onFailure: (String) -> Unit) {
        if (code != null) {
            val exchangeResult = driveRepository.exchangeCodeForTokens(code)
            exchangeResult
                .onSuccess {
                    Log.d("kptest", "Successfully exchanged tokens")
                    dataStore.updateBackup(true)
                }
                .onFailure { error ->
                    Log.e("kptest", "Token exchange failed ${error.message}")
                    onFailure("Token exchange failed ${error.message}") }
        } else {
            Log.e("kptest", "CRITICAL: No Resolution AND No Auth Code!")
            onFailure("Could not retrieve auth code")
        }
    }

    fun onAuthResultCompleted(data: Intent?, onFailure:(String)->Unit) {
        viewModelScope.launch {
            _isProcessingBackupToggle.value = true
            val authCode = driveRepository.getAuthCodeFromIntent(data)
            Log.d("kptest", "onAuthResultCompleted")
            exchangeCodeForTokens(authCode,onFailure)
            _isProcessingBackupToggle.value = false
        }
    }

    fun signOut(){
        viewModelScope.launch {
            driveRepository.clearDriveCredentials()
            dataStore.updateBackup(false)
            authRepository.signOut()
        }
    }

    fun updateBackupSettings(
        frequency: BackupFrequency,
        hour: Int,
        minute: Int,
        backupNow: Boolean = false
    ) {
        viewModelScope.launch {
            dataStore.updateBackupSettings(frequency,hour,minute)
            cancelScheduledBackup()
            if(frequency!=BackupFrequency.OFF) {
                scheduleBackup(frequency, hour, minute, backupNow)
            }
        }
    }

    fun loadBackupInfo() {
        viewModelScope.launch {
            _backupUiState.value = BackupUiState.Loading
            checkForExistingBackup().collect { metadata ->
                _backupUiState.value = if (metadata != null) {
                    BackupUiState.HasBackup(metadata)
                } else {
                    BackupUiState.NoBackup
                }
            }
        }
    }

    fun startRestore() {
        viewModelScope.launch {
            _backupDownloadState.value = BackupStatus.Progress(0,UploadDownload.DOWNLOAD)
            downloadBackup().collect { status ->
                _backupDownloadState.value = status

                if (status is BackupStatus.Success) {
                    _restoreEvents.emit(RestoreEvent.NavigateToHome)
                    delay(500)
                    _backupDownloadState.value = BackupStatus.Idle
                }
            }
        }
    }

    fun createMassiveDummyData() {
        viewModelScope.launch(Dispatchers.IO) {
            for (i in 1..10000){
                noteUseCases.addNote(Note(title = "Note #$i",
                    content = "Content for $i",
                    timestamp = System.currentTimeMillis(),
                    color = NoteColors.colors.random().toArgb()))
            }
        }
    }
}