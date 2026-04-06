package com.mintanable.notethepad.feature_note.presentation.modify.components.audioanimation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
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
import com.mintanable.notethepad.core.model.note.NoteOrder
import com.mintanable.notethepad.theme.NoteThePadTheme
import com.mintanable.notethepad.theme.ThemePreviews
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sin

private const val CIRCULAR_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float amplitude;

// Palette based on requested colors
vec3 palette(float t) {
    vec3 c1 = vec3(0.384, 0.0, 0.933); // Deep Purple
    vec3 c2 = vec3(0.96, 0.56, 0.69); // RedPink
    vec3 c3 = vec3(0.012, 0.855, 0.776); // Teal    
    vec3 c4 = vec3(1.0, 0.008, 0.4);   // Pink 
       
    return mix(mix(c1, c2, sin(t)*0.5+0.5), mix(c3, c4, cos(t)*0.5+0.5), sin(t * 0.3)*0.5+0.5);
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution.xy) / min(iResolution.y, iResolution.x);
    
    // --- 1. Horizontal Energy Wave Logic ---
    float waveFreq = 12.0; 
    float waveAmpOffset = amplitude * 0.15; 
    float wave = sin(uv.x * waveFreq + iTime * 2.5) * waveAmpOffset;
    float waveDist = abs(uv.y + wave);
    float waveLine = smoothstep(0.006, 0.0, waveDist * (2.0 + amplitude * 10.0));
    
    // --- 2. Central Core Nucleus Logic ---
    float dist = length(uv);
    float angle = atan(uv.y, uv.x);
    float rays = sin(angle * 42.0 + iTime * 1.5) * 0.5 + 0.5;
    rays *= pow(sin(angle * 12.0 - iTime * 0.7) * 0.5 + 0.5, 4.0);
    
    float radius = 0.3 + (amplitude * 0.2); 
    float burstThreshold = radius + (rays * 0.18 * (amplitude + 0.3));
    float coreMask = smoothstep(burstThreshold, burstThreshold - 0.08, dist);
    float verticalMask = smoothstep(0.4, 0.15, abs(uv.y));
    
    // --- 3. Final Color Integration ---
    vec3 basePalette = palette(angle + iTime * 0.5);
    vec3 finalCol = basePalette * coreMask;
    
    // White hot center
    float centerHotspotCircle = smoothstep(radius * 0.7, radius * 0.35, dist);
    finalCol = mix(finalCol, vec3(1.0), centerHotspotCircle * 0.6);
    
    // Wave color integration
    vec3 wavePalette = palette(uv.x * 0.8 + iTime * 1.2);
    finalCol += wavePalette * waveLine * (amplitude + 0.1);

    // Vignette fade out
    float overallAlphaFade = smoothstep(0.8, 0.3, dist);
    
    // --- 4. TRANSPARENCY CALCULATION ---
    // We calculate alpha based on the intensity of our shapes
    float alpha = max(coreMask * verticalMask, waveLine * (amplitude + 0.1));
    alpha = clamp(alpha * overallAlphaFade, 0.0, 1.0);
    
    // For transparent surfaces, AGSL expects Premultiplied Alpha: vec4(RGB * Alpha, Alpha)
    return vec4(finalCol * alpha, alpha);
}
"""

@Composable
fun AudioWaveAnimation(
    amplitude: Int,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(CIRCULAR_SHADER) }
        val shaderBrush = remember { ShaderBrush(shader) }
        var iTime by remember { mutableFloatStateOf(0f) }

        val animatedAmplitude by animateFloatAsState(
            targetValue = (amplitude / 32767.0).pow(0.5).toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "amp"
        )

        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { frameTimeMs -> iTime = frameTimeMs / 1000f }
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                shader.setFloatUniform("iTime", iTime)
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("amplitude", animatedAmplitude)

                drawRect(brush = shaderBrush)
            }

            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Mic",
                modifier = Modifier.size(36.dp),
                tint = Color.White.copy(alpha = 0.95f)
            )
        }
    }
}


@ThemePreviews
@Composable
fun PreviewAudioWaveAnimation() {
    var mockAmplitude by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var time = 0f
        while(true) {
            // Stronger oscillation to show the "Expansion" effect clearly
            mockAmplitude = (10000 + sin(time) * 15000).toInt()
            time += 0.15f
            delay(50)
        }
    }

    NoteThePadTheme {
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AudioWaveAnimation(
                amplitude = mockAmplitude,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}