package com.mintanable.notethepad.feature_firebase.presentation.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mintanable.notethepad.core.common.Screen
import com.mintanable.notethepad.core.model.settings.ThemeMode
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthState
import com.mintanable.notethepad.feature_firebase.presentation.auth.AuthViewModel
import com.mintanable.notethepad.feature_firebase.R
import com.mintanable.notethepad.core.ui.R as UiR
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews

@Composable
fun LoginScreen(
    navController: NavHostController,
    onGoogleSigInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    currentTheme: ThemeMode = ThemeMode.SYSTEM,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMsg = stringResource(R.string.error_signin)

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            navController.navigate(Screen.NotesScreen.route) {
                popUpTo(Screen.FirebaseLoginScreen.route) { inclusive = true }
            }
        }
        if(state is AuthState.Error) {
            Toast.makeText(context,
                errorMsg,
                Toast.LENGTH_LONG)
                .show()
        }
    }

    LoginScreenContent(
        state = state,
        themeMode = currentTheme,
        onGoogleSigInClick = onGoogleSigInClick
    )
}

@Composable
fun LoginScreenContent(
    state: AuthState,
    themeMode: ThemeMode,
    onGoogleSigInClick: () -> Unit,
    // onFacebookSignInClick: () -> Unit // if needed
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(
                    id = if(darkTheme) UiR.drawable.google_signin_dark else UiR.drawable.google_signin_light
                ),
                contentDescription = stringResource(R.string.content_description_google_signin),
                modifier = Modifier
                    .clickable(enabled = true, onClick = onGoogleSigInClick)
            )
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

@ThemePreviews
@Composable
fun PreviewLoginScreen(){
    NoteThePadTheme {
        LoginScreenContent(
            state = AuthState.Idle,
            themeMode = ThemeMode.SYSTEM,
            onGoogleSigInClick = {}
        )
    }
}