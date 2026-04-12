package com.mintanable.notethepad.feature_firebase.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.auth.repository.AuthRepository
import com.mintanable.notethepad.core.analytics.AnalyticsEvent
import com.mintanable.notethepad.core.analytics.AnalyticsTracker
import com.mintanable.notethepad.core.model.settings.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker
): ViewModel() {

    private val manualStateTrigger = MutableSharedFlow<AuthState>()
    val authState: StateFlow<AuthState> = merge (
        repository.getSignedInFirebaseUser()
        .map { user ->
            if (user != null) AuthState.Success(user) else AuthState.Idle
        },
        manualStateTrigger
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Idle
        )

    val currentUser: StateFlow<User?> = authState
        .map { state ->
            (state as? AuthState.Success)?.user
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun signOut() {
        analyticsTracker.track(AnalyticsEvent.AuthSignOut)
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.LoginEmail -> performAuth("email") { repository.loginWithEmail(event.email, event.pass) }
            is AuthEvent.GoogleSignIn -> performAuth("google") { repository.signInWithGoogle(event.idToken) }
            is AuthEvent.FacebookSignIn -> performAuth("facebook") { repository.signInWithFacebook(event.accessToken) }
            is AuthEvent.LogOut -> signOut()
        }
    }

    private fun performAuth(method: String, authBlock: suspend () -> Result<User>) {
        analyticsTracker.track(AnalyticsEvent.AuthSignInAttempted(method))
        viewModelScope.launch {
            manualStateTrigger.emit(AuthState.Loading)

            authBlock()
                .onSuccess {
                    analyticsTracker.track(AnalyticsEvent.AuthSignInResult(true))
                }
                .onFailure { error ->
                    analyticsTracker.track(AnalyticsEvent.AuthSignInResult(false, error::class.simpleName))
                    manualStateTrigger.emit(
                        AuthState.Error(error.localizedMessage ?: "Error")
                    )
                }
        }
    }
}