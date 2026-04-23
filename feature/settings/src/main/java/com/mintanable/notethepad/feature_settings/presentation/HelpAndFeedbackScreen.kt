package com.mintanable.notethepad.feature_settings.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.feature_settings.R
import com.mintanable.notethepad.feature_settings.presentation.components.HelpItemCard
import com.mintanable.notethepad.feature_settings.presentation.components.SettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndFeedbackScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    val noEmailMsg = stringResource(R.string.msg_no_email_app)
    val noPlayStoreMsg = stringResource(R.string.msg_no_play_store)
    val feedbackSubject = stringResource(R.string.feedback_email_subject)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.help_and_feedback_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.help_section_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(items = helpItems, key = { it.id }) { helpItem ->
                HelpItemCard(
                    titleRes = helpItem.titleRes,
                    stepsRes = helpItem.stepsRes,
                    expanded = expandedId == helpItem.id,
                    onToggle = {
                        expandedId = if (expandedId == helpItem.id) null else helpItem.id
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.feedback_section_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingItem(
                    title = stringResource(R.string.help_email_us_title),
                    subtitle = stringResource(R.string.help_email_us_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:mintanables@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, null))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, noEmailMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            item {
                SettingItem(
                    title = stringResource(R.string.help_rate_us_title),
                    subtitle = stringResource(R.string.help_rate_us_subtitle),
                    onClick = {
                        val marketIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}")
                        )
                        try {
                            context.startActivity(marketIntent)
                        } catch (_: ActivityNotFoundException) {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                            try {
                                context.startActivity(webIntent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, noPlayStoreMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
