package com.mintanable.notethepad.feature_settings.presentation

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.mintanable.notethepad.core.worker.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.domain.network.NetworkMonitor
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
import com.mintanable.notethepad.feature_settings.domain.model.BackupSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    workManager: WorkManager,
    private val noteUseCases: NoteUseCases,
    private val downloadBackup: DownloadBackup,
    private val savedStateHandle: SavedStateHandle,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val KEY_PENDING_BACKUP_NOW = "pending_backup_now"
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    private val _backupUploadStatus = workManager.getBackupStatusFlow(
        workName = BackupSchedulerImpl.WORK_NAME,
        type = UploadDownload.UPLOAD,
        onSuccess = { refreshTrigger.tryEmit(Unit) }
    )

    private val _backupDownloadState = workManager.getBackupStatusFlow(
        workName = DownloadBackup.DOWNLOAD_BACKUP_TASK,
        type = UploadDownload.DOWNLOAD
    )

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val backupUiState: StateFlow<BackupUiState> = combine(
        settingsState.map { it.googleAccount }.distinctUntilChanged(),
        refreshTrigger
    ) { email, _ -> email }
        .flatMapLatest { email ->
            if (email == null) {
                flow<BackupUiState> { emit(BackupUiState.NoBackup) }
            }else {

                flow<BackupUiState> {
                    emit(BackupUiState.Loading)
                    try {
                        checkForExistingBackup().collect { metadata ->
                            emit(
                                if (metadata != null)
                                    BackupUiState.HasBackup(metadata)
                                else
                                    BackupUiState.NoBackup
                            )
                        }
                    } catch (e: Exception) {
                        val errorMessage = if (e is java.io.IOException) {
                            "No internet connection"
                        } else {
                            "Failed to check backup"
                        }
                        emit(BackupUiState.Error(errorMessage))
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BackupUiState.Loading
        )

    private val _isAuthorisingBackup = MutableStateFlow(false)
    val isAuthorisingBackup = _isAuthorisingBackup.asStateFlow()

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch { dataStore.updateTheme(mode) }
    }

    fun signOut(){
        viewModelScope.launch {
            driveRepository.clearDriveCredentials()
            resetBackupSettings()
            authRepository.signOut()
        }
    }

    private fun resetBackupSettings() {
        viewModelScope.launch {
            dataStore.updateBackupSettings(
                settingsState.value.backupSettings.copy(
                    backupFrequency = BackupFrequency.OFF
                )
            )
        }
    }

    fun updateBackupSettings(
        backupSettings: BackupSettings,
        backupNow: Boolean = false,
        onAuthRequired: (PendingIntent) -> Unit,
        onFailure:(String)->Unit
    ) {
        viewModelScope.launch {
            dataStore.updateBackupSettings(backupSettings)

            if (!canPerformNetworkTask(onFailure)) return@launch
            if(backupSettings.backupFrequency==BackupFrequency.OFF) {
                cancelScheduledBackup()
                return@launch
            }

            if (driveRepository.hasDriveAccess()) {
                rescheduleBackup(backupSettings, backupNow)
            }else{
                handleNewAuthorization(backupNow, onAuthRequired, onFailure)
            }
        }
    }

    private fun rescheduleBackup(settings: BackupSettings, now: Boolean) {
        cancelScheduledBackup()
        scheduleBackup(settings, now)
    }

    private suspend fun handleNewAuthorization(
        backupNow: Boolean,
        onAuthRequired: (PendingIntent) -> Unit,
        onFailure: (String) -> Unit
    ) {
        _isAuthorisingBackup.value = true

        driveRepository.authorizeDriveAccess(settingsState.value.googleAccount!!)
            .onSuccess { result ->
                if (result.hasResolution()) {
                    savedStateHandle[KEY_PENDING_BACKUP_NOW] = backupNow
                    result.pendingIntent?.let { onAuthRequired(it) }
                } else {
                    exchangeCodeForTokens(
                        code = result.serverAuthCode,
                        onSuccess = {
                            rescheduleBackup(settingsState.value.backupSettings, backupNow)
                        },
                        onFailure = { message ->
                            onFailure(message)
                            resetBackupSettings()
                        }
                    )
                    _isAuthorisingBackup.value = false
                }
            }
            .onFailure { exception ->
                _isAuthorisingBackup.value = false
                resetBackupSettings()
                onFailure(exception.localizedMessage ?: "Authorization failed")
            }
    }

    private suspend fun exchangeCodeForTokens(
        code: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (code != null) {
            val exchangeResult = driveRepository.exchangeCodeForTokens(code)
            exchangeResult
                .onSuccess {
                    Log.d("kptest", "Successfully exchanged tokens")
                    onSuccess()
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
            _isAuthorisingBackup.value = true
            val authCode = driveRepository.getAuthCodeFromIntent(data)
            Log.d("kptest", "onAuthResultCompleted")
            exchangeCodeForTokens(
                authCode,
                onSuccess =
                    {
                        val wasBackupNowRequested = savedStateHandle.get<Boolean>(KEY_PENDING_BACKUP_NOW) ?: false
                        cancelScheduledBackup()
                        scheduleBackup(settingsState.value.backupSettings, wasBackupNowRequested)
                        savedStateHandle[KEY_PENDING_BACKUP_NOW] = false
                    },
                onFailure = { message ->
                    resetBackupSettings()
                    onFailure(message)
                }
            )
            _isAuthorisingBackup.value = false
        }
    }

    fun onAuthCancelled() {
        resetBackupSettings()
        _isAuthorisingBackup.value = false
        savedStateHandle[KEY_PENDING_BACKUP_NOW] = false
    }

    fun startRestore(onFailure: (String) -> Unit) {
        viewModelScope.launch {
            if (!canPerformNetworkTask(onFailure)) return@launch
            downloadBackup()
        }

    }

    private suspend fun canPerformNetworkTask(
        onFailure: (String) -> Unit
    ): Boolean {
        if (!networkMonitor.isOnline.first()) {
            onFailure("You are offline. Check your connection.")
            return false
        }
        val isMetered = !networkMonitor.isUnmetered.first()

        val allowMetered = false //dataStore.settingsFlow.first().allowMeteredBackup

        if (isMetered && !allowMetered) {
            onFailure("Warning: Using Cellular. Enable 'Backup/Restore on Cellular' in settings.")
            return false
        }

        return true
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