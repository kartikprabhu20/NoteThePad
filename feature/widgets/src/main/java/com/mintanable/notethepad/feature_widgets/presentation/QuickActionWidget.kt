package com.mintanable.notethepad.feature_widgets.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.mintanable.notethepad.core.common.NavigationConstants
import com.mintanable.notethepad.feature_widgets.R
import com.mintanable.notethepad.feature_widgets.presentation.utils.MediumWidgetPreview
import com.mintanable.notethepad.feature_widgets.presentation.utils.buildOpenNoteIntent

class QuickActionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickActionContent()
            }
        }
    }
}

@Composable
private fun QuickActionContent() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuickActionIcon(
                iconRes = R.drawable.baseline_photo_camera_24,
                action = NavigationConstants.INITIAL_ACTION_CAMERA,
                contentDescription = context.getString(R.string.widget_qa_cd_camera),
                modifier = GlanceModifier.defaultWeight()
            )
            QuickActionIcon(
                iconRes = R.drawable.baseline_videocam_24,
                action = NavigationConstants.INITIAL_ACTION_VIDEO,
                contentDescription = context.getString(R.string.widget_qa_cd_video),
                modifier = GlanceModifier.defaultWeight()
            )
            QuickActionIcon(
                iconRes = R.drawable.baseline_mic_24,
                action = NavigationConstants.INITIAL_ACTION_AUDIO,
                contentDescription = context.getString(R.string.widget_qa_cd_audio),
                modifier = GlanceModifier.defaultWeight()
            )
            QuickActionIcon(
                iconRes = R.drawable.baseline_edit_24,
                action = NavigationConstants.INITIAL_ACTION_TEXT,
                contentDescription = context.getString(R.string.widget_qa_cd_text),
                modifier = GlanceModifier.defaultWeight()
            )
            QuickActionIcon(
                iconRes = R.drawable.baseline_checklist_24,
                action = NavigationConstants.INITIAL_ACTION_CHECKLIST,
                contentDescription = context.getString(R.string.widget_qa_cd_checklist),
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun QuickActionIcon(
    iconRes: Int,
    action: String,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(
                    actionStartActivity(
                        buildOpenNoteIntent(context, initialAction = action)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = contentDescription,
                modifier = GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 300, heightDp = 100)
@MediumWidgetPreview
@Composable
fun QuickActionContentPreview() {
    GlanceTheme {
        QuickActionContent()
    }
}
