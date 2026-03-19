package com.mintanable.notethepad.feature_widgets.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

@Composable
fun WidgetTitleBar(
    title: String,
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int,
    titleBarActionIconContentDescription: String,
    titleBarAction: () -> Unit,
) {
    val isWideEnough = LocalSize.current.width >= 150.dp

    TitleBar(
        startIcon = if (isWideEnough) ImageProvider(titleIconRes) else null,
        title = title,
        iconColor = null,
        textColor = GlanceTheme.colors.onSurface,
        actions = {
            CircleIconButton(
                imageProvider = ImageProvider(titleBarActionIconRes),
                contentDescription = titleBarActionIconContentDescription,
                contentColor = GlanceTheme.colors.secondary,
                backgroundColor = null,
                onClick = titleBarAction
            )
        }
    )
}

@Composable
private fun TitleBar(
    startIcon: ImageProvider?,
    title: String,
    iconColor: ColorProvider? = GlanceTheme.colors.onSurface,
    textColor: ColorProvider = GlanceTheme.colors.onSurface,
    modifier: GlanceModifier = GlanceModifier,
    fontFamily: FontFamily? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    @Composable
    fun StartIcon() {
        Box(
            GlanceModifier.size(48.dp).padding(start = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            startIcon?.let {
                Image(
                    modifier = GlanceModifier.size(24.dp),
                    provider = it,
                    contentDescription = "",
                    colorFilter = iconColor?.let { ColorFilter.tint(iconColor) }
                )
            }
        }
    }

    @Composable
    fun RowScope.Title() {
        if(startIcon==null) Box (modifier = GlanceModifier.padding(4.dp)){}
        Text(
            text = title,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                fontFamily = fontFamily
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }

    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        startIcon?.let {
            StartIcon()
        }
        Title()
        actions()
    }
}