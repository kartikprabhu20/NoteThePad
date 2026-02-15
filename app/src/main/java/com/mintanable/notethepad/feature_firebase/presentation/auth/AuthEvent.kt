package com.mintanable.notethepad.feature_firebase.presentation.auth

sealed class AuthEvent {
    data class LoginEmail(val email: String, val pass: String) : AuthEvent()
    data class GoogleSignIn(val idToken: String) : AuthEvent()
    data class FacebookSignIn(val accessToken: String) : AuthEvent()
}