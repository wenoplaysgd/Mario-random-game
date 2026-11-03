package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamePreferences
import com.example.levels.LevelRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    preferences: GamePreferences,
    onSelectLevel: (Int) -> Unit
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(preferences.isSoundEnabled) }

    val levels = remember { LevelRepository.getAllLevelInfo() }
    val progressList = remember(preferences) {
        levels.map { preferences.getLevelProgress(it.id) }
    }

    val totalStars = progressList.sumOf { it.starsEarned }
    val totalCoins = progressList.sumOf { it.coinsCollected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MARIO PARKOUR",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val newSetting = !soundEnabled
                            soundEnabled = newSetting
                            preferences.isSoundEnabled = newSetting
                        },
                        modifier = Modifier.testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showHowToPlay = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How to play",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1B4B)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Header Card
            item {
                StatsHeaderCard(
                    totalStars = totalStars,
                    maxStars = levels.size * 3,
                    totalCoins = totalCoins,
                    onHowToPlayClick = { showHowToPlay = true }
                )
            }

            // Level List Header
            item {
                Text(
                    text = "SELECT STAGE",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            // Level Cards
            items(levels) { levelInfo ->
                val progress = progressList.firstOrNull { it.levelId == levelInfo.id }
                val isUnlocked = progress?.isUnlocked ?: (levelInfo.id == 1)
                val starsEarned = progress?.starsEarned ?: 0
                val bestTime = progress?.bestTimeSeconds ?: 0f

                LevelCard(
                    levelInfo = levelInfo,
                    isUnlocked = isUnlocked,
                    starsEarned = starsEarned,
                    bestTime = bestTime,
                    onClick = {
                        if (isUnlocked) {
                            onSelectLevel(levelInfo.id)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (showHowToPlay) {
            HowToPlayDialog(onDismiss = { showHowToPlay = false })
        }
    }
}

@Composable
private fun StatsHeaderCard(
    totalStars: Int,
    maxStars: Int,
    totalCoins: Int,
    onHowToPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFFE52521), Color(0xFFFFD700))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WORLD PROGRESS",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Plumber Parkour",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = onHowToPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guide",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalStars / $maxStars",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Coins
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalCoins COINS",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    levelInfo: LevelRepository.LevelInfo,
    isUnlocked: Boolean,
    starsEarned: Int,
    bestTime: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_card_${levelInfo.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.7f)
        ),
        border = if (isUnlocked) {
            CardDefaults.outlinedCardBorder().copy(width = 1.5.dp, brush = Brush.linearGradient(listOf(levelInfo.theme.brickColor, levelInfo.theme.skyColorTop)))
        } else {
            CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Number Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) levelInfo.theme.brickColor else Color(0xFF334155)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = "${levelInfo.id}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Level Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = levelInfo.title,
                        color = if (isUnlocked) Color.White else Color(0xFF64748B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = levelInfo.subtitle,
                    color = if (isUnlocked) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stars earned row & Best Time
                if (isUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (s in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (s <= starsEarned) Color(0xFFFFD700) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (bestTime > 0f) {
                            Spacer(modifier = Modifier.width(10.dp))
                            val mins = (bestTime / 60).toInt()
                            val secs = (bestTime % 60).toInt()
                            val tenths = ((bestTime * 10) % 10).toInt()
                            Text(
                                text = "Best: %02d:%02d.%d".format(mins, secs, tenths),
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Complete Level ${levelInfo.id - 1} to unlock",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right Action Arrow or Difficulty Badge
            if (isUnlocked) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.testTag("play_level_${levelInfo.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play Level",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}
