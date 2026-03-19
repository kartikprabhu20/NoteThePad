package com.mintanable.notethepad.feature_settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.mintanable.notethepad.core.model.AiModel
import com.mintanable.notethepad.core.model.BackupFrequency
import com.mintanable.notethepad.core.model.BackupSettings
import com.mintanable.notethepad.core.model.LoadStatus
import com.mintanable.notethepad.core.model.LoadType
import com.mintanable.notethepad.core.model.Settings
import com.mintanable.notethepad.core.model.ThemeMode
import com.mintanable.notethepad.feature_ai.data.ModelDownloadWorker.Companion.MODEL_DOWNLOAD_TASK
import com.mintanable.notethepad.feature_ai.domain.use_cases.DownloadAiModelUseCase
import com.mintanable.notethepad.feature_ai.domain.use_cases.GetSupportedAiModels
import com.mintanable.notethepad.feature_backup.domain.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.domain.network.NetworkMonitor
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.use_case.CancelScheduledBackupUsecase
import com.mintanable.notethepad.feature_backup.domain.use_case.CheckForExistingBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.DownloadBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.ScheduleBackupUseCase
import com.mintanable.notethepad.feature_backup.presentation.RestoreEvent
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_settings.presentation.BackupUiState
import com.mintanable.notethepad.feature_settings.presentation.SettingsEvent
import com.mintanable.notethepad.feature_settings.presentation.SettingsState
import com.mintanable.notethepad.feature_settings.presentation.getLoadStatusFLow
import com.mintanable.notethepad.preference.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val driveRepository: GoogleAuthRepository,
    private val scheduleBackup: ScheduleBackupUseCase,
    private val cancelScheduledBackup: CancelScheduledBackupUsecase,
    private val checkForExistingBackup: CheckForExistingBackup,
    private val workManager: WorkManager,
    private val downloadBackup: DownloadBackup,
    private val savedStateHandle: SavedStateHandle,
    private val networkMonitor: NetworkMonitor,
    private val downloadAiModelUseCase: DownloadAiModelUseCase,
    private val getSupportedAiModels: GetSupportedAiModels
) : ViewModel() {

    companion object {
        private const val KEY_PENDING_BACKUP_NOW = "pending_backup_now"
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

    private val _backupUploadStatus = workManager.getLoadStatusFLow(
        workName = BackupSchedulerImpl.WORK_NAME,
        type = LoadType.UPLOAD,
        onSuccess = { refreshTrigger.tryEmit(Unit) }
    )

    private val _backupDownloadStatus = workManager.getLoadStatusFLow(
        workName = DownloadBackup.DOWNLOAD_BACKUP_TASK,
        type = LoadType.DOWNLOAD
    )

    private val _isAuthorisingBackup = MutableStateFlow(false)

    private val _aiModelDownloadStatus = workManager.getLoadStatusFLow(
        workName = MODEL_DOWNLOAD_TASK,
        type = LoadType.DOWNLOAD
    )

    private val _downloadDialogModel = MutableStateFlow<AiModel?>(null)

    private val _dataStoreSettings : StateFlow<Settings> = combine(
        authRepository.getSignedInFirebaseUser(),
        dataStore.settingsFlow
    ) { user, settings ->
        val googleEmail = if (user?.isGoogleSignedIn == true) user.email else null
        settings.copy(googleAccount = googleEmail)
    }
        .distinctUntilChanged()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _backupMetadataFlow = _dataStoreSettings
        .map { it.googleAccount }
        .distinctUntilChanged()
        .flatMapLatest { email ->
            if (email == null) {
                flowOf(BackupUiState.NoBackup)
            } else {
                refreshTrigger.flatMapLatest {
                    flow<BackupUiState> {
                        emit(BackupUiState.Loading)
                        try {
                            checkForExistingBackup().collect { metadata ->
                                emit(if (metadata != null) BackupUiState.HasBackup(metadata) else BackupUiState.NoBackup)
                            }
                        } catch (e: Exception) {
                            emit(BackupUiState.Error("Failed to check backup"))
                        }
                    }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SettingsState> = combine(
        _dataStoreSettings,
        _backupMetadataFlow,
        _backupUploadStatus,
        _backupDownloadStatus,
        _aiModelDownloadStatus,
        getSupportedAiModels(),
        _isAuthorisingBackup,
        _downloadDialogModel
    ) { args: Array<Any?> ->
        val settings = args[0] as Settings
        val metadata = args[1] as BackupUiState
        val upload = args[2] as LoadStatus
        val download = args[3] as LoadStatus
        val aiStatus = args[4] as LoadStatus
        val models = args[5] as List<AiModel>
        val isAuthorising = args[6] as Boolean
        val downloadDialogModel = args[7] as AiModel?

        val activeTransferStatus = when {
            download is LoadStatus.Progress -> download
            upload is LoadStatus.Progress -> upload
            download is LoadStatus.Success || download is LoadStatus.Error -> download
            else -> upload
        }

        SettingsState(
            settings = settings,
            backupUiState = metadata,
            backupUploadDownloadState = activeTransferStatus,
            aiModels = models,
            aiModelDownloadStatus = aiStatus,
            isAuthorisingBackup = isAuthorising,
            showDownloadModelDialog = downloadDialogModel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    private val _restoreEvents = MutableSharedFlow<RestoreEvent>()
    val restoreEvents = _restoreEvents.asSharedFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateTheme -> updateTheme(event.themeMode)
            is SettingsEvent.UpdateBackupSettings -> updateBackupSettings(
                event.backupSettings,
                event.backupNow,
                event.onAuthRequired,
                event.onFailure
            )
            is SettingsEvent.AuthResultCompleted -> onAuthResultCompleted(event.intent, event.onFailure)
            SettingsEvent.AuthCancelled -> onAuthCancelled()
            is SettingsEvent.StartRestore -> startRestore(event.onFailure)
            is SettingsEvent.SelectAiModel -> selectAiModel(event.aiModel)
            is SettingsEvent.ConfirmDownloadAiModel -> confirmDownloadAiModel(event.aiModel, event.onFailure)
            SettingsEvent.DismissDownloadDialog -> _downloadDialogModel.value = null
            SettingsEvent.SignOut -> signOut()
        }
    }

    private fun selectAiModel(aiModel: AiModel) {
        viewModelScope.launch {
            if (aiModel.url.isEmpty()) {
                withContext(Dispatchers.IO) { dataStore.updateAiModel(aiModel.name) }
                return@launch
            }

            val isDownloaded = withContext(Dispatchers.IO) {
                val externalFilesDir = context.getExternalFilesDir(null)
                val modelFile = externalFilesDir?.let { File(it, aiModel.downloadFileName) }
                modelFile?.exists() == true && modelFile.length() == aiModel.sizeInBytes
            }

            if (isDownloaded) {
                withContext(Dispatchers.IO) { dataStore.updateAiModel(aiModel.name) }
            } else {
                _downloadDialogModel.value = aiModel
            }
        }
    }

    private fun confirmDownloadAiModel(aiModel: AiModel, onFailure: (String) -> Unit) {
        _downloadDialogModel.value = null
        viewModelScope.launch(Dispatchers.Default) {
            val isNetworkReady = canPerformNetworkTask { message ->
                viewModelScope.launch(Dispatchers.Main) {
                    onFailure(message)
                }
            }
            if (!isNetworkReady) return@launch
            dataStore.updateAiModel(aiModel.name)
            downloadAiModelUseCase(aiModel.url, aiModel.downloadFileName)
        }
    }

    private fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch { dataStore.updateTheme(mode) }
    }

    private fun signOut() {
        viewModelScope.launch {
            driveRepository.clearDriveCredentials()
            resetBackupSettings()
            authRepository.signOut()
        }
    }

    private fun resetBackupSettings() {
        viewModelScope.launch {
            val currentSettings = state.value.settings
            dataStore.updateBackupSettings(
                currentSettings.backupSettings.copy(
                    backupFrequency = BackupFrequency.OFF
                )
            )
        }
    }

    private fun updateBackupSettings(
        backupSettings: BackupSettings,
        backupNow: Boolean,
        onAuthRequired: (PendingIntent) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            dataStore.updateBackupSettings(backupSettings)

            if (!canPerformNetworkTask(onFailure)) return@launch
            if (backupSettings.backupFrequency == BackupFrequency.OFF) {
                cancelScheduledBackup()
                return@launch
            }

            if (driveRepository.hasDriveAccess()) {
                rescheduleBackup(backupSettings, backupNow)
            } else {
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

        val googleAccount = state.value.settings.googleAccount
        if (googleAccount == null) {
            _isAuthorisingBackup.value = false
            onFailure("No Google account linked")
            return
        }

        driveRepository.authorizeDriveAccess(googleAccount)
            .onSuccess { result ->
                if (result.hasResolution()) {
                    savedStateHandle[KEY_PENDING_BACKUP_NOW] = backupNow
                    result.pendingIntent?.let { onAuthRequired(it) }
                } else {
                    exchangeCodeForTokens(
                        code = result.serverAuthCode,
                        onSuccess = {
                            rescheduleBackup(state.value.settings.backupSettings, backupNow)
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
                    onFailure("Token exchange failed ${error.message}")
                }
        } else {
            Log.e("kptest", "CRITICAL: No Resolution AND No Auth Code!")
            onFailure("Could not retrieve auth code")
        }
    }

    private fun onAuthResultCompleted(data: Intent?, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthorisingBackup.value = true
            val authCode = driveRepository.getAuthCodeFromIntent(data)
            Log.d("kptest", "onAuthResultCompleted")
            exchangeCodeForTokens(
                authCode,
                onSuccess =
                    {
                        val wasBackupNowRequested =
                            savedStateHandle.get<Boolean>(KEY_PENDING_BACKUP_NOW) ?: false
                        cancelScheduledBackup()
                        scheduleBackup(state.value.settings.backupSettings, wasBackupNowRequested)
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

    private fun onAuthCancelled() {
        resetBackupSettings()
        _isAuthorisingBackup.value = false
        savedStateHandle[KEY_PENDING_BACKUP_NOW] = false
    }

    private fun startRestore(onFailure: (String) -> Unit) {
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

}