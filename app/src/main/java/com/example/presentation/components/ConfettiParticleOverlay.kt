package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.usecase.CelebrationEvent
import com.example.domain.usecase.GamificationEngine
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessEmerald
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    RIBBON,
    STAR
}

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val sizePx: Float,
    val color: Color,
    val shape: ConfettiShape,
    var rotationDeg: Float,
    val rotationSpeedDeg: Float,
    val flutterFreq: Float,
    val flutterAmplitude: Float,
    val tumbleFreq: Float,
    val phase: Float,
    val gravityScale: Float
)

private const val CONFETTI_DURATION_SECONDS = 3.2f
private const val FADE_OUT_WINDOW_SECONDS = 0.75f

private val CelebrationColors = listOf(
    Color(0xFF4F46E5), // Indigo
    Color(0xFF7C3AED), // Violet
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFFBBF24), // Gold
    Color(0xFFEC4899), // Rose Pink
    Color(0xFF38BDF8)  // Bright Cyan
)

@Composable
fun ConfettiParticleOverlay(
    celebration: CelebrationEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (celebration == null) return

    var elapsedSeconds by remember(celebration.id) { mutableFloatStateOf(0f) }
    var particles by remember(celebration.id) { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var initializedSize by remember(celebration.id) { mutableStateOf(Size.Zero) }

    LaunchedEffect(celebration.id) {
        elapsedSeconds = 0f
        particles = emptyList()
        initializedSize = Size.Zero

        var lastNanos = 0L
        while (elapsedSeconds < CONFETTI_DURATION_SECONDS) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = frameNanos
                elapsedSeconds += dt

                val currentParticles = particles
                if (currentParticles.isNotEmpty()) {
                    val gravity = 980f
                    val drag = 0.986f
                    currentParticles.forEach { p ->
                        p.vx *= drag
                        p.vy = (p.vy * drag) + (gravity * p.gravityScale * dt)
                        val flutterDx = sin(elapsedSeconds * p.flutterFreq + p.phase) * p.flutterAmplitude
                        p.x += (p.vx + flutterDx) * dt
                        p.y += p.vy * dt
                        p.rotationDeg += p.rotationSpeedDeg * dt
                    }
                }
            }
        }
        onDismiss()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Non-touch-blocking physics particle canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("confetti_particle_canvas")
        ) {
            // Read elapsedSeconds to trigger redraw every frame
            val currentElapsed = elapsedSeconds
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            if (particles.isEmpty() || initializedSize != size) {
                initializedSize = size
                particles = spawnConfettiBurst(
                    width = size.width,
                    height = size.height,
                    seed = celebration.id,
                    isLevelUp = celebration.isLevelUp
                )
            }

            val fadeAlpha = if (currentElapsed > CONFETTI_DURATION_SECONDS - FADE_OUT_WINDOW_SECONDS) {
                ((CONFETTI_DURATION_SECONDS - currentElapsed) / FADE_OUT_WINDOW_SECONDS).coerceIn(0f, 1f)
            } else {
                1f
            }

            if (fadeAlpha <= 0f) return@Canvas

            particles.forEach { p ->
                if (p.y > size.height + 60f || p.x < -60f || p.x > size.width + 60f) return@forEach
                val tumbleScaleY = (0.3f + 0.7f * kotlin.math.abs(
                    cos(currentElapsed * p.tumbleFreq + p.phase)
                )).coerceIn(0.25f, 1f)
                val colorWithAlpha = p.color.copy(alpha = fadeAlpha)

                withTransform({
                    translate(left = p.x, top = p.y)
                    rotate(degrees = p.rotationDeg, pivot = Offset.Zero)
                    scale(scaleX = 1f, scaleY = tumbleScaleY, pivot = Offset.Zero)
                }) {
                    drawConfettiShape(p.shape, p.sizePx, colorWithAlpha)
                }
            }
        }

        // Celebratory Milestone Pill Banner at top (auto-fades with burst, tap to dismiss early)
        AnimatedVisibility(
            visible = elapsedSeconds < CONFETTI_DURATION_SECONDS - 0.25f,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + scaleIn(initialScale = 0.9f) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val headline = when {
                celebration.newLevel != null && celebration.reachedStreakMilestone != null ->
                    "Level ${celebration.newLevel} & ${celebration.reachedStreakMilestone}-Day Streak!"
                celebration.newLevel != null ->
                    "Level Up! Level ${celebration.newLevel}"
                else ->
                    "${celebration.reachedStreakMilestone}-Day Streak Milestone!"
            }
            val subheadline = when {
                celebration.newLevel != null ->
                    "${GamificationEngine.titleForLevel(celebration.newLevel)} • ${celebration.habitTitle}"
                else ->
                    "Consistency paying off on ${celebration.habitTitle}"
            }
            val badgeTint = if (celebration.isLevelUp) StreakAmber else SuccessEmerald

            Surface(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onDismiss)
                    .testTag("confetti_milestone_banner"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, badgeTint.copy(alpha = 0.55f)),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(badgeTint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (celebration.isLevelUp) {
                                Icons.Rounded.AutoAwesome
                            } else {
                                Icons.Rounded.LocalFireDepartment
                            },
                            contentDescription = headline,
                            tint = badgeTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subheadline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun spawnConfettiBurst(
    width: Float,
    height: Float,
    seed: Long,
    isLevelUp: Boolean
): List<ConfettiParticle> {
    val rng = Random(seed)
    val count = if (isLevelUp) 110 else 88
    val shapes = ConfettiShape.entries

    return List(count) { index ->
        // Split into left cannon, right cannon, and center-top fountain
        val group = index % 3
        val (originX, originY, angleRad, speed) = when (group) {
            0 -> {
                // Left cannon launching up and right
                val ox = width * 0.08f
                val oy = height * 0.34f
                val angle = (-72f + rng.nextFloat() * 44f) * (PI / 180.0)
                val spd = 480f + rng.nextFloat() * 520f
                Quadruple(ox, oy, angle.toFloat(), spd)
            }
            1 -> {
                // Right cannon launching up and left
                val ox = width * 0.92f
                val oy = height * 0.34f
                val angle = (-152f + rng.nextFloat() * 44f) * (PI / 180.0)
                val spd = 480f + rng.nextFloat() * 520f
                Quadruple(ox, oy, angle.toFloat(), spd)
            }
            else -> {
                // Top center shower
                val ox = width * (0.18f + rng.nextFloat() * 0.64f)
                val oy = height * 0.06f
                val angle = (55f + rng.nextFloat() * 70f) * (PI / 180.0)
                val spd = 180f + rng.nextFloat() * 340f
                Quadruple(ox, oy, angle.toFloat(), spd)
            }
        }

        val shape = if (!isLevelUp && shapes[index % shapes.size] == ConfettiShape.STAR) {
            ConfettiShape.RECTANGLE
        } else {
            shapes[rng.nextInt(shapes.size)]
        }

        ConfettiParticle(
            x = originX,
            y = originY,
            vx = cos(angleRad) * speed,
            vy = sin(angleRad) * speed,
            sizePx = 10f + rng.nextFloat() * 12f,
            color = CelebrationColors[rng.nextInt(CelebrationColors.size)],
            shape = shape,
            rotationDeg = rng.nextFloat() * 360f,
            rotationSpeedDeg = (rng.nextFloat() - 0.5f) * 520f,
            flutterFreq = 4f + rng.nextFloat() * 6f,
            flutterAmplitude = 45f + rng.nextFloat() * 75f,
            tumbleFreq = 5f + rng.nextFloat() * 7f,
            phase = rng.nextFloat() * (2f * PI.toFloat()),
            gravityScale = 0.78f + rng.nextFloat() * 0.45f
        )
    }
}

private data class Quadruple(
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float
)

private fun DrawScope.drawConfettiShape(
    shape: ConfettiShape,
    sizePx: Float,
    color: Color
) {
    when (shape) {
        ConfettiShape.RECTANGLE -> {
            val w = sizePx
            val h = sizePx * 0.58f
            drawRoundRect(
                color = color,
                topLeft = Offset(-w / 2f, -h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
        }
        ConfettiShape.CIRCLE -> {
            drawCircle(
                color = color,
                radius = sizePx * 0.36f,
                center = Offset.Zero
            )
        }
        ConfettiShape.RIBBON -> {
            val w = sizePx * 1.45f
            val h = sizePx * 0.32f
            drawRoundRect(
                color = color,
                topLeft = Offset(-w / 2f, -h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
        }
        ConfettiShape.STAR -> {
            val rOuter = sizePx * 0.58f
            val rInner = sizePx * 0.22f
            val path = Path().apply {
                moveTo(0f, -rOuter)
                lineTo(rInner, -rInner)
                lineTo(rOuter, 0f)
                lineTo(rInner, rInner)
                lineTo(0f, rOuter)
                lineTo(-rInner, rInner)
                lineTo(-rOuter, 0f)
                lineTo(-rInner, -rInner)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}
