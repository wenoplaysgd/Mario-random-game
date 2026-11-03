package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HowToPlayDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                .testTag("how_to_play_dialog"),
            color = Color(0xFF1E1B4B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARKOUR GUIDE",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_how_to_play")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ParkourTipItem(
                    emoji = "🏃",
                    title = "Run & Momentum",
                    desc = "Use LEFT / RIGHT arrows to build running momentum. Hold direction while jumping to clear wider chasms."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ParkourTipItem(
                    emoji = "🧱",
                    title = "Wall Jump (Key!)",
                    desc = "Jump towards any solid brick/stone wall. You'll slide down with friction—tap JUMP to kick off in the opposite direction!"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ParkourTipItem(
                    emoji = "⚡",
                    title = "Parkour Dash (B)",
                    desc = "Tap the B button for a high-speed dash burst! Perfect for crossing wide pits or dodging hazards in mid-air."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ParkourTipItem(
                    emoji = "🌀",
                    title = "Spring Pads",
                    desc = "Step on red & yellow spring pads to trigger high-velocity super bounces over tall walls."
                )

                Spacer(modifier = Modifier.height(10.dp))

                ParkourTipItem(
                    emoji = "🚩",
                    title = "Checkpoints & Stars",
                    desc = "Touch checkpoint flags to save your spot! Collect all 3 golden stars and finish fast for maximum score."
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE52521)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("got_it_button")
                ) {
                    Text(
                        text = "LET'S PARKOUR!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ParkourTipItem(
    emoji: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF312E81).copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 10.dp, top = 2.dp)
        )
        Column {
            Text(
                text = title,
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
