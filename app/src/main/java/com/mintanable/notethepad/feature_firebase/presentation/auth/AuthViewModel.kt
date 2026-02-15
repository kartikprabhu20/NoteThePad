package com.mintanable.notethepad.feature_firebase.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mintanable.notethepad.feature_firebase.domain.model.User
import com.mintanable.notethepad.feature_firebase.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
): ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(
        repository.getSignedInUser()?.let { AuthState.Success(it) } ?: AuthState.Idle
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<User?> = authState.map { state ->
        (state as? AuthState.Success)?.user
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getSignedInUser()
    )

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
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
            _authState.value = AuthState.Loading
            authBlock()
                .onSuccess { _authState.value = AuthState.Success(it) }
                .onFailure { _authState.value = AuthState.Error(it.localizedMessage ?: "Error") }
        }
    }
}