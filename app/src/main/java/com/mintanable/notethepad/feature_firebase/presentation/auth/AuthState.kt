package com.mintanable.notethepad.feature_firebase.presentation.auth

import com.mintanable.notethepad.feature_firebase.domain.model.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}