package com.mintanable.notethepad.feature_settings.presentation

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.mintanable.notethepad.core.worker.BackupSchedulerImpl
import com.mintanable.notethepad.feature_backup.domain.repository.GoogleAuthRepository
import com.mintanable.notethepad.feature_backup.domain.use_case.CancelScheduledBackupUsecase
import com.mintanable.notethepad.feature_backup.domain.use_case.CheckForExistingBackup
import com.mintanable.notethepad.feature_backup.domain.use_case.ScheduleBackupUseCase
import com.mintanable.notethepad.feature_backup.presentation.BackupUiState
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import com.mintanable.notethepad.feature_settings.domain.model.Settings
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.data.repository.UserPreferencesRepository
import com.mintanable.notethepad.feature_settings.domain.model.BackupFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val workManager: WorkManager
) : ViewModel() {

    val backupWorkInfo = workManager
        .getWorkInfosForUniqueWorkFlow(BackupSchedulerImpl.WORK_NAME)
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _backupUiState = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val backupUiState = _backupUiState.asStateFlow()

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
}