package com.mintanable.notethepad.feature_firebase.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthState
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.ui.util.Screen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    navController: NavHostController,
    onGoogleSigInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            navController.navigate(Screen.NotesScreen.route) {
                popUpTo(Screen.FirebaseLoginScreen.route) { inclusive = true }
            }
        }
        if(state is AuthState.Error) {
            Toast.makeText(context,
                "Error occured while signing in, try again!",
                Toast.LENGTH_LONG)
                .show()
        }
    }

    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onGoogleSigInClick) {
                Text("Sign in with Google")
            }

            Button(onClick = onFacebookSignInClick) {
                Text("Sign in with Facebook")
            }
        }

        if (state is AuthState.Error) {
            Text((state as AuthState.Error).message, color = Color.Red)
        }

        if (state is AuthState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}