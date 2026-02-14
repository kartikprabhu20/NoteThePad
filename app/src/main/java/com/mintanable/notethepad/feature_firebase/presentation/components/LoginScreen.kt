package com.mintanable.notethepad.feature_firebase.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthState
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.authState.value
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { /* Launch Google SignIn Intent - then call: */
                 viewModel.onEvent(AuthEvent.GoogleSignIn(idToken))
            }){
                Text("Sign in with Google")
            }

            Button(onClick = { /* Launch Facebook Login - then call: */
                // viewModel.onEvent(AuthEvent.FacebookSignIn(token))
            }) { Text("Sign in with Facebook") }
        }

        if (state is AuthState.Loading)
            CircularProgressIndicator()

        if (state is AuthState.Error) {
            Text((state as AuthState.Error).message, color = Color.Red)
        }
    }
}