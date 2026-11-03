package com.example.ui

import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.RetroSoundManager
import com.example.data.GamePreferences
import com.example.engine.GameEngine
import com.example.graphics.PixelArtRenderer
import com.example.levels.LevelRepository
import kotlinx.coroutines.isActive

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(
    levelId: Int,
    soundManager: RetroSoundManager,
    preferences: GamePreferences,
    onBackToLevelSelect: () -> Unit
) {
    var currentLevelId by remember { mutableIntStateOf(levelId) }
    val levelData = remember(currentLevelId) { LevelRepository.getLevel(currentLevelId) }
    val engine = remember(currentLevelId) { GameEngine(levelData, soundManager, preferences) }

    var frameTick by remember { mutableLongStateOf(0L) }
    var showPauseMenu by remember { mutableStateOf(false) }
    var showHowToPlay by remember { mutableStateOf(false) }

    // Intercept back button to show pause menu or return to level select
    BackHandler {
        if (showPauseMenu || engine.levelCompleted) {
            onBackToLevelSelect()
        } else {
            showPauseMenu = true
            engine.isPaused = true
        }
    }

    // 60 FPS Game Loop
    LaunchedEffect(engine) {
        var lastNano = System.nanoTime()
        while (isActive) {
            withFrameNanos { now ->
                val dt = ((now - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastNano = now
                engine.update(dt)
                frameTick++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Bar
            GameTopHud(
                engine = engine,
                frameTick = frameTick,
                soundManager = soundManager,
                onPauseClick = {
                    showPauseMenu = true
                    engine.isPaused = true
                },
                onRestartClick = {
                    engine.resetToStart()
                }
            )

            // Game Viewport Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("game_canvas")
                ) {
                    // Suppress unused warning for frameTick (forces recomposition)
                    if (frameTick < 0) return@Canvas

                    // Calculate dynamic tile size so around 11-13 tiles fit vertically
                    val tileSizePx = (size.height / 11.5f).coerceIn(36f, 90f)

                    PixelArtRenderer.drawGame(
                        drawScope = this,
                        level = engine.level,
                        player = engine.player,
                        cameraX = engine.cameraX,
                        cameraY = engine.cameraY,
                        tileSizePx = tileSizePx,
                        particles = engine.particles,
                        floatingTexts = engine.floatingTexts,
                        gameTime = engine.gameTime
                    )
                }

                // Level Progress / Goal Distance Bar
                val progress = (engine.player.x / engine.level.mapWidth).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(3.dp)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFE52521), Color(0xFFFFD700))))
                    )
                }
            }

            // Bottom Arcade Controls
            ArcadeControlDeck(
                engine = engine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        // Pause Menu Dialog
        if (showPauseMenu) {
            PauseMenuDialog(
                levelTitle = engine.level.title,
                soundManager = soundManager,
                preferences = preferences,
                onResume = {
                    showPauseMenu = false
                    engine.isPaused = false
                },
                onRestart = {
                    showPauseMenu = false
                    engine.isPaused = false
                    engine.resetToStart()
                },
                onHowToPlay = {
                    showHowToPlay = true
                },
                onStageSelect = onBackToLevelSelect
            )
        }

        // Level Completed Victory Dialog
        if (engine.levelCompleted) {
            LevelClearDialog(
                engine = engine,
                isLastLevel = currentLevelId >= LevelRepository.totalLevels,
                onNextLevel = {
                    if (currentLevelId < LevelRepository.totalLevels) {
                        currentLevelId += 1
                    }
                },
                onReplay = {
                    engine.resetToStart()
                },
                onLevelSelect = onBackToLevelSelect
            )
        }

        // How To Play Guide Dialog
        if (showHowToPlay) {
            HowToPlayDialog(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
private fun GameTopHud(
    engine: GameEngine,
    frameTick: Long,
    soundManager: RetroSoundManager,
    onPauseClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(!soundManager.isSoundEnabled) }

    Surface(
        color = Color(0xFF1E1B4B),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .testTag("game_top_hud")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Level Name & Theme Badge
            Column {
                Text(
                    text = "LVL ${engine.level.id}: ${engine.level.title}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                val mins = (engine.gameTime / 60).toInt()
                val secs = (engine.gameTime % 60).toInt()
                val tenths = ((engine.gameTime * 10) % 10).toInt()
                Text(
                    text = "TIME: %02d:%02d.%d".format(mins, secs, tenths),
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats: Coins & Stars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Coins
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${engine.coinsCollected}",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = if (engine.starsCollected > 0) Color(0xFFFFD700) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${engine.starsCollected}/3",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        soundManager.isSoundEnabled = !isMuted
                    },
                    modifier = Modifier.size(34.dp).testTag("hud_mute_button")
                ) {
                    Icon(
                        imageVector = if (!isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Mute",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRestartClick,
                    modifier = Modifier.size(34.dp).testTag("hud_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Level",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier.size(34.dp).testTag("hud_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Ergonomic responsive multi-touch Arcade Control Deck.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ArcadeControlDeck(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: D-Pad (Left / Right buttons)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button
            ArcadeDirectionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "LEFT",
                isPressed = engine.inputLeft,
                onStateChange = { isDown -> engine.inputLeft = isDown },
                testTag = "dpad_left"
            )

            // Right Button
            ArcadeDirectionButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "RIGHT",
                isPressed = engine.inputRight,
                onStateChange = { isDown -> engine.inputRight = isDown },
                testTag = "dpad_right"
            )
        }

        // Right Side: Action Buttons (B: Dash, A: Jump)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button B: DASH / ROLL
            ArcadeActionButton(
                label = "B",
                subLabel = "DASH",
                baseColor = Color(0xFF0284C7),
                isPressed = engine.inputDash,
                onStateChange = { isDown -> engine.inputDash = isDown },
                sizeDp = 62,
                testTag = "btn_dash"
            )

            // Button A: JUMP
            ArcadeActionButton(
                label = "A",
                subLabel = "JUMP",
                baseColor = Color(0xFFDC2626),
                isPressed = engine.inputJump,
                onStateChange = { isDown -> engine.inputJump = isDown },
                sizeDp = 72,
                testTag = "btn_jump"
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ArcadeDirectionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPressed: Boolean,
    onStateChange: (Boolean) -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(if (isPressed) 2.dp else 6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) Color(0xFF475569) else Color(0xFF1E293B))
            .border(2.dp, if (isPressed) Color(0xFF94A3B8) else Color(0xFF334155), RoundedCornerShape(16.dp))
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onStateChange(true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onStateChange(false)
                        true
                    }
                    else -> false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPressed) Color(0xFFFFD700) else Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ArcadeActionButton(
    label: String,
    subLabel: String,
    baseColor: Color,
    isPressed: Boolean,
    onStateChange: (Boolean) -> Unit,
    sizeDp: Int,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .shadow(if (isPressed) 2.dp else 8.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isPressed) baseColor.copy(alpha = 0.7f) else baseColor)
            .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onStateChange(true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onStateChange(false)
                        true
                    }
                    else -> false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (sizeDp > 65) 24.sp else 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subLabel,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PauseMenuDialog(
    levelTitle: String,
    soundManager: RetroSoundManager,
    preferences: GamePreferences,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHowToPlay: () -> Unit,
    onStageSelect: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(soundManager.isHapticsEnabled) }

    Dialog(onDismissRequest = onResume) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(18.dp))
                .testTag("pause_menu_dialog"),
            color = Color(0xFF1E1B4B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    color = Color(0xFFFFD700),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = levelTitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                )

                // Resume
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Restart
                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color(0xFF94A3B8)))),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART LEVEL", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // How to Play
                OutlinedButton(
                    onClick = onHowToPlay,
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300)))),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_guide_button")
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFFFD54F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HOW TO PLAY", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Select
                Button(
                    onClick = onStageSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_stage_select_button")
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STAGE SELECT", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = soundEnabled,
                        onClick = {
                            val newS = !soundEnabled
                            soundEnabled = newS
                            soundManager.isSoundEnabled = newS
                            preferences.isSoundEnabled = newS
                        },
                        label = { Text(if (soundEnabled) "Sound ON" else "Sound OFF", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    FilterChip(
                        selected = hapticsEnabled,
                        onClick = {
                            val newH = !hapticsEnabled
                            hapticsEnabled = newH
                            soundManager.isHapticsEnabled = newH
                            preferences.isHapticsEnabled = newH
                        },
                        label = { Text(if (hapticsEnabled) "Vibe ON" else "Vibe OFF", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                if (hapticsEnabled) Icons.Default.Vibration else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelClearDialog(
    engine: GameEngine,
    isLastLevel: Boolean,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onLevelSelect: () -> Unit
) {
    val mins = (engine.gameTime / 60).toInt()
    val secs = (engine.gameTime % 60).toInt()
    val tenths = ((engine.gameTime * 10) % 10).toInt()
    val timeFormatted = "%02d:%02d.%d".format(mins, secs, tenths)

    val earnedStars = maxOf(1, minOf(3, engine.starsCollected + (if (engine.coinsCollected >= 5) 1 else 0)))

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(4.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                .testTag("level_clear_dialog"),
            color = Color(0xFF1E1B4B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STAGE CLEAR!!",
                    color = Color(0xFFFFD700),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = engine.level.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // Big Stars Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 1..3) {
                        val active = i <= earnedStars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (active) Color(0xFFFFD700) else Color(0xFF475569),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF312E81).copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CLEAR TIME", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(timeFormatted, color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COINS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("${engine.coinsCollected}", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Next Level Button
                if (!isLastLevel) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("next_level_button")
                    ) {
                        Text("NEXT LEVEL", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Replay Button
                OutlinedButton(
                    onClick = onReplay,
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color(0xFF94A3B8)))),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("replay_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REPLAY STAGE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Select
                Button(
                    onClick = onLevelSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("win_stage_select_button")
                ) {
                    Text("STAGE SELECT", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
