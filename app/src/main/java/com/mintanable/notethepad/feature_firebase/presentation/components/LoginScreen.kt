package com.mintanable.notethepad.feature_firebase.presentation.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mintanable.notethepad.R
import com.mintanable.notethepad.feature_settings.domain.model.ThemeMode
import com.mintanable.notethepad.feature_settings.presentation.SettingsViewModel
import com.mintanable.notethepad.ui.theme.NoteThePadTheme
import com.mintanable.notethepad.ui.theme.ThemePreviews

@Composable
fun LoginScreen(
    navController: NavHostController,
    onGoogleSigInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
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
        themeMode = settings.themeMode,
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
                    id = if(darkTheme) R.drawable.google_signin_dark else R.drawable.google_signin_light
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