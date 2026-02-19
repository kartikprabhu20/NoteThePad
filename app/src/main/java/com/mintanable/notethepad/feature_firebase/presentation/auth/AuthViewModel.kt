package com.mintanable.notethepad.feature_firebase.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_firebase.data.repository.toDomainUser
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
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
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.LoginEmail -> performAuth { repository.loginWithEmail(event.email, event.pass) }
            is AuthEvent.GoogleSignIn -> performAuth { repository.signInWithGoogle(event.idToken) }
            is AuthEvent.FacebookSignIn -> performAuth { repository.signInWithFacebook(event.accessToken) }
        }
    }

    private fun performAuth(authBlock: suspend () -> Result<User>) {
        viewModelScope.launch {
            manualStateTrigger.emit(AuthState.Loading)

            authBlock().onFailure { error ->
                manualStateTrigger.emit(
                    AuthState.Error(error.localizedMessage ?: "Error")
                )
            }
        }
    }
}