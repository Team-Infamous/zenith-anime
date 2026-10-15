package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Animated startup splash screen inspired by modern cinematic streaming apps,
 * featuring a breathing neon emblem, glowing emerald aura, and elegant entrance transitions.
 */
@Composable
fun AnimatedSplashScreen(
    onFinish: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "logo_scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1250L)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(contentAlpha)
        ) {
            // Emblem with pulsing glow ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Outer subtle glowing aura ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFF00DC82).copy(alpha = pulseGlow * 0.45f),
                            shape = CircleShape
                        )
                )

                // Soft background radial aura
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00DC82).copy(alpha = pulseGlow * 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Logo Container
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161B26))
                        .border(1.dp, Color(0xFF00DC82).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFF00DC82)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_zenith_logo),
                        contentDescription = "Zenith Anime Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZENITH ",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "ANIME",
                    color = Color(0xFF00DC82),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "STREAM • DISCOVER • TRACK",
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
        }

        // Bottom Loading Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sleek mini progress pill
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF242C3D))
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(3.dp)
                        .scale(ringScale)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF00DC82))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "v${com.example.BuildConfig.VERSION_NAME} • Streaming Experience",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
