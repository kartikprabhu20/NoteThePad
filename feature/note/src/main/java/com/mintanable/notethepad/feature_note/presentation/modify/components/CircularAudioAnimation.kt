package com.mintanable.notethepad.feature_note.presentation.modify.components


import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mintanable.notethepad.theme.ThemePreviews
import kotlin.math.pow
import kotlin.math.sin

private const val CIRCULAR_SHADER = """
uniform float2 iResolution;
uniform vec4 bgColor;
uniform float iTime;
uniform float amplitude;
uniform float pOffset;

// Helper to create the multi-color gradient
vec3 getGradient(float2 uv, float angle) {
    vec3 col1 = vec3(0.992, 0.875, 0.522); // yellow
    vec3 col2 = vec3(0.627, 0.816, 0.686); // green
    vec3 col3 = vec3(0.886, 0.372, 0.341); // red
    vec3 col4 = vec3(0.522, 0.694, 0.973); // blue
    
    // Rotate colors over time
    float shift = iTime * 0.5;
    return mix(mix(col1, col2, sin(angle + shift)), 
               mix(col3, col4, cos(angle - shift)), 
               0.5);
}

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float perlin_noise_1d(float x) {
    float i = floor(x);
    float f = fract(x);
    float u = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), u);
}

half4 main(float2 fragCoord) {
    // Center the coordinates (-1.0 to 1.0)
    float2 uv = (fragCoord - 0.5 * iResolution.xy) / min(iResolution.y, iResolution.x);
    
    float dist = length(uv);
    float angle = atan(uv.y, uv.x);
    
    // Base radius of the circle
    float baseRadius = 0.25 + (amplitude * 0.15);
    
    // Add "wobble" based on noise and angle
    // We sample noise using the angle so it wraps around
    float noise = perlin_noise_1d(pOffset + angle * 2.0 + iTime) * (amplitude + 0.05);
    float finalRadius = baseRadius + (noise * 0.1);
    
    // Create a soft edge for the circle
    float edge = smoothstep(finalRadius, finalRadius - 0.05, dist);
    
    vec3 color = getGradient(uv, angle);
    
    // Fade out as it gets further from the center
    float alpha = edge * smoothstep(finalRadius + 0.1, finalRadius, dist);
    
    // Mix with background
    return mix(bgColor, vec4(color, 1.0), alpha);
}
"""
@Composable
fun CircularAudioAnimation(
    bgColor: Color,
    amplitude: Int,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(CIRCULAR_SHADER) }
        val shaderBrush = remember { ShaderBrush(shader) }

        // Animation States
        var iTime by remember { mutableFloatStateOf(0f) }
        val normalizedAmplitude = (amplitude / 32767.0).pow(0.5).toFloat()

        // Smooth out the amplitude jumps
        val animatedAmplitude by animateFloatAsState(
            targetValue = normalizedAmplitude,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "amplitude"
        )

        // Continuous time loop
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { frameTimeMs -> iTime = frameTimeMs / 1000f }
            }
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Physical rotation of the canvas for extra flair
                    rotationZ = iTime * 10f
                }
        ) {
            shader.setFloatUniform("iTime", iTime)
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("bgColor", bgColor.red, bgColor.green, bgColor.blue, bgColor.alpha)
            shader.setFloatUniform("amplitude", animatedAmplitude)
            shader.setFloatUniform("pOffset", 0f)

            drawRect(brush = shaderBrush)
        }
    }
}

@ThemePreviews
@Composable
fun PreviewCircularAnimation() {
    var mockAmplitude by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var time = 0f
        while(true) {
            // Stronger oscillation to show the "Expansion" effect clearly
            mockAmplitude = (10000 + sin(time) * 15000).toInt()
            time += 0.15f
            kotlinx.coroutines.delay(50)
        }
    }

    Box(
        modifier = Modifier
            .size(400.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        CircularAudioAnimation(
            bgColor = MaterialTheme.colorScheme.surface,
            amplitude = mockAmplitude,
            modifier = Modifier.size(300.dp)
        )
    }
}