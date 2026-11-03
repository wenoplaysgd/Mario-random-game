package com.example.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import com.example.model.*
import kotlin.math.sin

/**
 * High-performance 2D Pixel Art Canvas renderer.
 * Handles parallax backgrounds, animated tiles, entities, and Mario sprite states.
 */
object PixelArtRenderer {

    // Classic NES/SNES Color Palette
    private val COLOR_RED = Color(0xFFE52521)
    private val COLOR_RED_SHADOW = Color(0xFFA01815)
    private val COLOR_BLUE = Color(0xFF004CB8)
    private val COLOR_BLUE_SHADOW = Color(0xFF002E7A)
    private val COLOR_YELLOW = Color(0xFFFFD700)
    private val COLOR_SKIN = Color(0xFFFFCC99)
    private val COLOR_BROWN = Color(0xFF5A2C00)
    private val COLOR_WHITE = Color(0xFFFFFFFF)
    private val COLOR_BLACK = Color(0xFF1A1A1A)

    fun drawGame(
        drawScope: DrawScope,
        level: LevelData,
        player: PlayerState,
        cameraX: Float,
        cameraY: Float,
        tileSizePx: Float,
        particles: List<Particle>,
        floatingTexts: List<FloatingText>,
        gameTime: Float
    ) {
        with(drawScope) {
            val screenW = size.width
            val screenH = size.height

            // 1. Draw Sky & Parallax Background
            drawBackground(level.theme, cameraX, cameraY, screenW, screenH, tileSizePx, gameTime)

            // 2. Camera Viewport Transformation for world entities
            // Camera position translates world coordinate (cameraX, cameraY) to screen center
            val originX = screenW * 0.4f - cameraX * tileSizePx
            val originY = screenH * 0.55f - cameraY * tileSizePx

            translate(left = originX, top = originY) {
                // Calculate visible tile range
                val minTileX = maxOf(0, ((cameraX * tileSizePx - screenW * 0.5f) / tileSizePx).toInt() - 2)
                val maxTileX = minOf(level.mapWidth - 1, ((cameraX * tileSizePx + screenW * 0.6f) / tileSizePx).toInt() + 2)
                val minTileY = maxOf(0, ((cameraY * tileSizePx - screenH * 0.6f) / tileSizePx).toInt() - 2)
                val maxTileY = minOf(level.mapHeight - 1, ((cameraY * tileSizePx + screenH * 0.6f) / tileSizePx).toInt() + 2)

                // 3. Draw Tiles
                for (y in minTileY..maxTileY) {
                    for (x in minTileX..maxTileX) {
                        val tileId = level.grid[y][x]
                        if (tileId != 0) {
                            drawTile(tileId, x.toFloat(), y.toFloat(), tileSizePx, level.theme, gameTime)
                        }
                    }
                }

                // 4. Draw Crumbling Platforms
                for (cp in level.crumblingPlatforms) {
                    if (!cp.isCollapsed) {
                        drawCrumblingPlatform(cp, tileSizePx, gameTime)
                    }
                }

                // 5. Draw Moving Platforms
                for (mp in level.movingPlatforms) {
                    drawMovingPlatform(mp, tileSizePx, gameTime)
                }

                // 6. Draw Checkpoints
                for (cp in level.checkpoints) {
                    drawCheckpoint(cp, tileSizePx, gameTime)
                }

                // 7. Draw Coins
                for (coin in level.coins) {
                    if (!coin.isCollected) {
                        drawCoin(coin, tileSizePx, gameTime)
                    }
                }

                // 8. Draw Stars
                for (star in level.stars) {
                    if (!star.isCollected) {
                        drawStar(star, tileSizePx, gameTime)
                    }
                }

                // 9. Draw Dash Ghosts (Motion Trail)
                if (player.trail.isNotEmpty()) {
                    for (i in player.trail.indices) {
                        val alpha = (i + 1).toFloat() / (player.trail.size + 1) * 0.45f
                        val (gx, gy) = player.trail[i]
                        drawMario(gx, gy, player, tileSizePx, alpha = alpha)
                    }
                }

                // 10. Draw Mario
                if (!player.isDead || (player.respawnTimer % 0.15f < 0.08f)) {
                    drawMario(player.x, player.y, player, tileSizePx, alpha = 1.0f)
                }

                // 11. Draw Particles
                for (p in particles) {
                    drawParticle(p, tileSizePx)
                }

                // 12. Draw Floating Texts
                for (ft in floatingTexts) {
                    drawFloatingText(ft, tileSizePx)
                }
            }
        }
    }

    private fun DrawScope.drawBackground(
        theme: LevelTheme,
        cameraX: Float,
        cameraY: Float,
        screenW: Float,
        screenH: Float,
        tileSizePx: Float,
        gameTime: Float
    ) {
        // Sky Gradient
        drawRect(
            color = theme.skyColorTop,
            size = Size(screenW, screenH * 0.5f)
        )
        drawRect(
            color = theme.skyColorBottom,
            topLeft = Offset(0f, screenH * 0.45f),
            size = Size(screenW, screenH * 0.55f)
        )

        // Distant Parallax Mountains / Hills (layer 1)
        val hillOffset = (-cameraX * tileSizePx * 0.15f) % (screenW * 0.8f)
        for (i in -1..2) {
            val hx = i * screenW * 0.8f + hillOffset
            val hy = screenH * 0.55f
            val path = Path().apply {
                moveTo(hx - screenW * 0.3f, screenH)
                lineTo(hx + screenW * 0.1f, hy)
                lineTo(hx + screenW * 0.5f, screenH)
                close()
            }
            drawPath(path, color = theme.hillColor.copy(alpha = 0.5f))
        }

        // Secondary Hills (layer 2)
        val hill2Offset = (-cameraX * tileSizePx * 0.3f) % (screenW * 0.6f)
        for (i in -1..2) {
            val hx = i * screenW * 0.6f + hill2Offset
            val hy = screenH * 0.65f
            val path = Path().apply {
                moveTo(hx - screenW * 0.2f, screenH)
                lineTo(hx + screenW * 0.1f, hy)
                lineTo(hx + screenW * 0.4f, screenH)
                close()
            }
            drawPath(path, color = theme.hillColor)
        }

        // Drifting Pixel Clouds
        val cloudSpeed = gameTime * 15f
        for (c in 0..4) {
            val cloudX = (c * 240f + cloudSpeed - cameraX * tileSizePx * 0.08f) % (screenW + 200f) - 100f
            val cloudY = 40f + (c * 37f) % 110f
            drawPixelCloud(cloudX, cloudY, theme.cloudColor)
        }
    }

    private fun DrawScope.drawPixelCloud(x: Float, y: Float, color: Color) {
        val pw = 12f
        drawRect(color = color.copy(alpha = 0.85f), topLeft = Offset(x, y + pw), size = Size(pw * 5, pw * 2))
        drawRect(color = color.copy(alpha = 0.85f), topLeft = Offset(x + pw, y), size = Size(pw * 3, pw))
        drawRect(color = color.copy(alpha = 0.85f), topLeft = Offset(x + pw * 4, y + pw * 0.5f), size = Size(pw * 2, pw * 2))
    }

    private fun DrawScope.drawTile(
        tileId: Int,
        tx: Float,
        ty: Float,
        ts: Float,
        theme: LevelTheme,
        gameTime: Float
    ) {
        val px = tx * ts
        val py = ty * ts

        when (tileId) {
            1 -> { // GROUND
                // Earthen body
                drawRect(color = theme.groundBodyColor, topLeft = Offset(px, py), size = Size(ts, ts))
                // Grass / snowy top edge
                drawRect(color = theme.groundTopColor, topLeft = Offset(px, py), size = Size(ts, ts * 0.25f))
                // Pixel grass blades/shading
                drawRect(color = theme.groundTopColor.copy(alpha = 0.8f), topLeft = Offset(px + ts * 0.15f, py + ts * 0.25f), size = Size(ts * 0.2f, ts * 0.15f))
                drawRect(color = theme.groundTopColor.copy(alpha = 0.8f), topLeft = Offset(px + ts * 0.65f, py + ts * 0.25f), size = Size(ts * 0.2f, ts * 0.15f))
                // Dark bottom line
                drawRect(color = Color(0x33000000), topLeft = Offset(px, py + ts - 2f), size = Size(ts, 2f))
            }
            2 -> { // BRICK
                drawRect(color = theme.brickColor, topLeft = Offset(px, py), size = Size(ts, ts))
                val mortar = Color(0xFF2B1008)
                // Outer mortar border
                drawRect(color = mortar, topLeft = Offset(px, py), size = Size(ts, 2f))
                drawRect(color = mortar, topLeft = Offset(px, py + ts * 0.5f), size = Size(ts, 2f))
                drawRect(color = mortar, topLeft = Offset(px, py + ts - 2f), size = Size(ts, 2f))
                // Vertical lines (staggered bricks)
                drawRect(color = mortar, topLeft = Offset(px + ts * 0.5f, py), size = Size(2f, ts * 0.5f))
                drawRect(color = mortar, topLeft = Offset(px + ts * 0.25f, py + ts * 0.5f), size = Size(2f, ts * 0.5f))
                drawRect(color = mortar, topLeft = Offset(px + ts * 0.75f, py + ts * 0.5f), size = Size(2f, ts * 0.5f))
                // Brick highlight
                drawRect(color = Color(0x40FFFFFF), topLeft = Offset(px + 2f, py + 2f), size = Size(ts * 0.45f, 2f))
            }
            3 -> { // QUESTION_BLOCK
                val gold = Color(0xFFFFB300)
                val goldDark = Color(0xFFFF8F00)
                drawRect(color = gold, topLeft = Offset(px, py), size = Size(ts, ts))
                drawRect(color = goldDark, topLeft = Offset(px, py + ts - 3f), size = Size(ts, 3f))
                drawRect(color = goldDark, topLeft = Offset(px + ts - 3f, py), size = Size(3f, ts))
                drawRect(color = Color(0x80FFFFFF), topLeft = Offset(px + 2f, py + 2f), size = Size(ts - 4f, 2f))
                // 4 Corner bolts
                val boltSize = ts * 0.1f
                drawRect(color = Color(0xFF5D4037), topLeft = Offset(px + 2f, py + 2f), size = Size(boltSize, boltSize))
                drawRect(color = Color(0xFF5D4037), topLeft = Offset(px + ts - boltSize - 2f, py + 2f), size = Size(boltSize, boltSize))
                drawRect(color = Color(0xFF5D4037), topLeft = Offset(px + 2f, py + ts - boltSize - 2f), size = Size(boltSize, boltSize))
                drawRect(color = Color(0xFF5D4037), topLeft = Offset(px + ts - boltSize - 2f, py + ts - boltSize - 2f), size = Size(boltSize, boltSize))
                // Question mark '?'
                val qColor = Color(0xFF4E342E)
                val cw = ts * 0.12f
                drawRect(color = qColor, topLeft = Offset(px + ts * 0.35f, py + ts * 0.22f), size = Size(ts * 0.3f, cw))
                drawRect(color = qColor, topLeft = Offset(px + ts * 0.53f, py + ts * 0.32f), size = Size(cw, ts * 0.2f))
                drawRect(color = qColor, topLeft = Offset(px + ts * 0.44f, py + ts * 0.5f), size = Size(cw, ts * 0.16f))
                drawRect(color = qColor, topLeft = Offset(px + ts * 0.44f, py + ts * 0.72f), size = Size(cw, cw))
            }
            4 -> { // HARD_BLOCK / STONE
                drawRect(color = Color(0xFF78909C), topLeft = Offset(px, py), size = Size(ts, ts))
                drawRect(color = Color(0xFFCFD8DC), topLeft = Offset(px + 2f, py + 2f), size = Size(ts - 4f, 3f))
                drawRect(color = Color(0xFF455A64), topLeft = Offset(px + 2f, py + ts - 4f), size = Size(ts - 4f, 3f))
                drawRect(color = Color(0xFF37474F), topLeft = Offset(px, py), size = Size(ts, ts), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
            5 -> { // ICE_BLOCK
                drawRect(color = Color(0xFFB2EBF2), topLeft = Offset(px, py), size = Size(ts, ts))
                drawRect(color = Color(0xFFE0F7FA), topLeft = Offset(px + 3f, py + 3f), size = Size(ts - 6f, ts - 6f))
                // Glint
                drawRect(color = Color.White, topLeft = Offset(px + ts * 0.2f, py + ts * 0.2f), size = Size(ts * 0.25f, 2f))
                drawRect(color = Color(0xFF80DEEA), topLeft = Offset(px, py + ts - 3f), size = Size(ts, 3f))
            }
            6 -> { // SPIKE
                val spikePath = Path().apply {
                    moveTo(px, py + ts)
                    lineTo(px + ts * 0.25f, py + ts * 0.1f)
                    lineTo(px + ts * 0.5f, py + ts)
                    lineTo(px + ts * 0.75f, py + ts * 0.1f)
                    lineTo(px + ts, py + ts)
                    close()
                }
                drawPath(spikePath, color = Color(0xFFB0BEC5))
                // Spike highlights
                drawLine(color = Color.White, start = Offset(px, py + ts), end = Offset(px + ts * 0.25f, py + ts * 0.1f), strokeWidth = 1.5f)
                drawLine(color = Color.White, start = Offset(px + ts * 0.5f, py + ts), end = Offset(px + ts * 0.75f, py + ts * 0.1f), strokeWidth = 1.5f)
            }
            7 -> { // LAVA
                val lavaWobble = sin(gameTime * 4f + tx) * (ts * 0.1f)
                drawRect(color = Color(0xFFE64A19), topLeft = Offset(px, py + lavaWobble), size = Size(ts, ts - lavaWobble))
                drawRect(color = Color(0xFFFFD54F), topLeft = Offset(px, py + lavaWobble), size = Size(ts, ts * 0.2f))
            }
            8 -> { // SPRING PAD
                val baseH = ts * 0.35f
                // Red Base
                drawRect(color = COLOR_RED, topLeft = Offset(px + ts * 0.1f, py + ts - baseH), size = Size(ts * 0.8f, baseH))
                // Steel Coil
                drawLine(color = Color(0xFFCFD8DC), start = Offset(px + ts * 0.3f, py + ts - baseH), end = Offset(px + ts * 0.7f, py + ts * 0.35f), strokeWidth = 3f)
                // Yellow Bounce Top Plate
                drawRect(color = COLOR_YELLOW, topLeft = Offset(px + ts * 0.05f, py + ts * 0.15f), size = Size(ts * 0.9f, ts * 0.2f))
                drawRect(color = Color(0xFFFF8F00), topLeft = Offset(px + ts * 0.05f, py + ts * 0.3f), size = Size(ts * 0.9f, 2f))
            }
            11 -> { // GOAL POLE
                // Flagpole shaft
                val poleW = ts * 0.2f
                val poleX = px + (ts - poleW) * 0.5f
                drawRect(color = Color(0xFFB0BEC5), topLeft = Offset(poleX, py), size = Size(poleW, ts))
                // Gold sphere at top
                if (ty < 5) {
                    drawCircle(color = COLOR_YELLOW, radius = ts * 0.3f, center = Offset(px + ts * 0.5f, py + ts * 0.2f))
                }
                // Checkered Banner Flag fluttering
                val flagWobble = sin(gameTime * 5f + ty) * 4f
                val flagPath = Path().apply {
                    moveTo(poleX + poleW, py + ts * 0.1f)
                    lineTo(poleX + poleW + ts * 1.3f + flagWobble, py + ts * 0.4f)
                    lineTo(poleX + poleW, py + ts * 0.7f)
                    close()
                }
                drawPath(flagPath, color = Color(0xFF43A047))
                drawLine(color = Color.White, start = Offset(poleX + poleW, py + ts * 0.4f), end = Offset(poleX + poleW + ts * 1.3f + flagWobble, py + ts * 0.4f), strokeWidth = 2f)
            }
            12 -> { // CASTLE DOOR
                drawRect(color = Color(0xFF212121), topLeft = Offset(px, py), size = Size(ts, ts))
                // Arched doorway
                val doorPath = Path().apply {
                    moveTo(px + ts * 0.2f, py + ts)
                    lineTo(px + ts * 0.2f, py + ts * 0.35f)
                    cubicTo(
                        px + ts * 0.2f, py,
                        px + ts * 0.8f, py,
                        px + ts * 0.8f, py + ts * 0.35f
                    )
                    lineTo(px + ts * 0.8f, py + ts)
                    close()
                }
                drawPath(doorPath, color = Color(0xFF000000))
                drawRect(color = Color(0xFFCFD8DC), topLeft = Offset(px + ts * 0.45f, py + ts * 0.6f), size = Size(4f, 4f)) // Keyhole
            }
        }
    }

    private fun DrawScope.drawCrumblingPlatform(cp: CrumblingPlatform, ts: Float, gameTime: Float) {
        val px = (cp.x + cp.shakeX) * ts
        val py = cp.y * ts
        val pw = cp.width * ts
        val ph = cp.height * ts

        val baseColor = if (cp.isSteppedOn) Color(0xFFD32F2F) else Color(0xFF795548)
        drawRect(color = baseColor, topLeft = Offset(px, py), size = Size(pw, ph))
        drawRect(color = Color(0xFF8D6E63), topLeft = Offset(px + 2f, py + 2f), size = Size(pw - 4f, ph - 4f))

        // Cracks when stepped on
        if (cp.isSteppedOn) {
            drawLine(color = Color(0xFF3E2723), start = Offset(px + pw * 0.3f, py), end = Offset(px + pw * 0.5f, py + ph), strokeWidth = 2f)
            drawLine(color = Color(0xFF3E2723), start = Offset(px + pw * 0.7f, py), end = Offset(px + pw * 0.6f, py + ph), strokeWidth = 2f)
        }
    }

    private fun DrawScope.drawMovingPlatform(mp: MovingPlatform, ts: Float, gameTime: Float) {
        val px = mp.x * ts
        val py = mp.y * ts
        val pw = mp.width * ts
        val ph = mp.height * ts

        // Metallic Platform Body
        drawRect(color = Color(0xFF37474F), topLeft = Offset(px, py), size = Size(pw, ph))
        drawRect(color = Color(0xFF546E7A), topLeft = Offset(px + 2f, py + 2f), size = Size(pw - 4f, ph - 4f))

        // Caution Stripes
        val stripeW = 8f
        var sx = px + 4f
        var toggle = false
        while (sx < px + pw - 4f) {
            val color = if (toggle) COLOR_YELLOW else Color(0xFF263238)
            drawRect(color = color, topLeft = Offset(sx, py + 3f), size = Size(minOf(stripeW, px + pw - 4f - sx), ph - 6f))
            sx += stripeW
            toggle = !toggle
        }
    }

    private fun DrawScope.drawCheckpoint(cp: Checkpoint, ts: Float, gameTime: Float) {
        val px = cp.x * ts
        val py = cp.y * ts

        // Pole
        drawRect(color = Color(0xFF78909C), topLeft = Offset(px + ts * 0.45f, py), size = Size(ts * 0.1f, ts))
        // Flag
        val flagColor = if (cp.isActivated) COLOR_YELLOW else Color(0xFFB0BEC5)
        val flagPath = Path().apply {
            moveTo(px + ts * 0.55f, py + ts * 0.1f)
            lineTo(px + ts * 0.95f, py + ts * 0.3f)
            lineTo(px + ts * 0.55f, py + ts * 0.5f)
            close()
        }
        drawPath(flagPath, color = flagColor)
    }

    private fun DrawScope.drawCoin(coin: DynamicCoin, ts: Float, gameTime: Float) {
        val cx = coin.x * ts + ts * 0.5f
        val cy = coin.y * ts + ts * 0.5f
        // Spinning width effect
        val spin = kotlin.math.abs(sin(gameTime * 6f + coin.id))
        val radius = ts * 0.32f
        val coinW = (radius * spin).coerceAtLeast(2f)

        drawOval(color = COLOR_YELLOW, topLeft = Offset(cx - coinW, cy - radius), size = Size(coinW * 2, radius * 2))
        drawOval(color = Color(0xFFFF8F00), topLeft = Offset(cx - coinW * 0.7f, cy - radius * 0.7f), size = Size(coinW * 1.4f, radius * 1.4f))
        // Glint
        drawRect(color = Color.White, topLeft = Offset(cx - coinW * 0.3f, cy - radius * 0.5f), size = Size(coinW * 0.4f, radius * 0.4f))
    }

    private fun DrawScope.drawStar(star: DynamicStar, ts: Float, gameTime: Float) {
        val cx = star.x * ts + ts * 0.5f
        val cy = star.y * ts + ts * 0.5f
        val pulse = 1f + sin(gameTime * 8f) * 0.15f
        val r = ts * 0.45f * pulse

        // Golden Star polygon
        val path = Path()
        val numPoints = 5
        val angleStep = Math.PI / numPoints
        for (i in 0 until (numPoints * 2)) {
            val dist = if (i % 2 == 0) r else r * 0.45f
            val angle = i * angleStep - Math.PI / 2.0
            val x = (cx + dist * kotlin.math.cos(angle)).toFloat()
            val y = (cy + dist * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(path, color = COLOR_YELLOW)
        // Outer sparkle aura
        drawCircle(color = COLOR_YELLOW.copy(alpha = 0.25f), radius = r * 1.3f, center = Offset(cx, cy))
    }

    /**
     * Procedural Pixel Art Mario Renderer.
     * Renders authentic 16x16 grid plumber character with animation states.
     */
    private fun DrawScope.drawMario(
        worldX: Float,
        worldY: Float,
        player: PlayerState,
        ts: Float,
        alpha: Float = 1.0f
    ) {
        val px = worldX * ts
        val py = worldY * ts
        val pw = player.width * ts
        val ph = player.height * ts
        val pixelSize = pw / 12f

        // Center Mario in bounds
        translate(left = px, top = py) {
            // Flip horizontally if facing left
            val scaleX = if (player.facingRight) 1f else -1f
            clipRect(left = -ts * 0.5f, top = -ts * 0.5f, right = ts * 1.5f, bottom = ts * 1.5f) {
                // If facing left, pivot around center
                if (!player.facingRight) {
                    translate(left = pw, top = 0f) {
                        // Render flipped
                        renderMarioPixels(player, pixelSize, alpha = alpha, scaleX = -1f)
                    }
                } else {
                    renderMarioPixels(player, pixelSize, alpha = alpha, scaleX = 1f)
                }
            }
        }
    }

    private fun DrawScope.renderMarioPixels(
        player: PlayerState,
        p: Float,
        alpha: Float,
        scaleX: Float
    ) {
        val red = COLOR_RED.copy(alpha = alpha)
        val blue = COLOR_BLUE.copy(alpha = alpha)
        val skin = COLOR_SKIN.copy(alpha = alpha)
        val brown = COLOR_BROWN.copy(alpha = alpha)
        val white = COLOR_WHITE.copy(alpha = alpha)
        val yellow = COLOR_YELLOW.copy(alpha = alpha)

        fun drawP(x: Float, y: Float, w: Float, h: Float, color: Color) {
            val drawX = if (scaleX < 0) -x - w else x
            drawRect(color = color, topLeft = Offset(drawX, y), size = Size(w, h))
        }

        // Determine pose
        val isJumping = !player.isGrounded && !player.isWallSliding && !player.isDashing
        val isWallSliding = player.isWallSliding
        val isDashing = player.isDashing
        val isRunning = player.isGrounded && kotlin.math.abs(player.vx) > 0.1f
        val isWon = player.hasWon

        // 1. CAP (Red)
        if (isWon) {
            // Victory: Arms raised!
            drawP(3 * p, 0 * p, 6 * p, 2 * p, red)
            drawP(2 * p, 1 * p, 8 * p, 2 * p, red)
        } else if (isWallSliding) {
            // Clinging to wall
            drawP(2 * p, 0.5f * p, 7 * p, 2.5f * p, red)
            drawP(4 * p, 0.5f * p, 6 * p, 1.5f * p, red)
        } else {
            // Normal cap
            drawP(3 * p, 0 * p, 6 * p, 2 * p, red)
            drawP(2 * p, 1.5f * p, 8 * p, 1.8f * p, red) // Visor brim
        }

        // 2. FACE & HAIR
        drawP(2 * p, 3 * p, 7 * p, 3 * p, skin)
        // Hair & sideburns (brown)
        drawP(1.5f * p, 2 * p, 2 * p, 3 * p, brown)
        drawP(1.5f * p, 3.5f * p, 1.5f * p, 2 * p, brown)
        // Eye (black)
        drawP(6.5f * p, 3 * p, 1 * p, 1.5f * p, COLOR_BLACK.copy(alpha = alpha))
        // Big Mustache (brown/black)
        drawP(5 * p, 4.5f * p, 4 * p, 1.5f * p, COLOR_BROWN.copy(alpha = alpha))
        // Nose (skin highlight)
        drawP(7 * p, 4 * p, 2 * p, 1.5f * p, skin)

        // 3. BODY / OVERALLS & SHIRT
        if (isJumping) {
            // Fist in air jump pose!
            // Raised arm (red sleeve + white glove)
            drawP(7 * p, 0.5f * p, 2.5f * p, 3 * p, red)
            drawP(7.5f * p, -1.5f * p, 2.5f * p, 2.5f * p, white) // Raised fist!

            // Body
            drawP(3 * p, 6 * p, 5 * p, 4 * p, blue)
            drawP(4 * p, 6.5f * p, 1 * p, 1 * p, yellow) // Button
            drawP(6.5f * p, 6.5f * p, 1 * p, 1 * p, yellow)

            // Back arm
            drawP(0.5f * p, 6.5f * p, 2.5f * p, 2.5f * p, white)

            // Legs curled
            drawP(2.5f * p, 9.5f * p, 3 * p, 2.5f * p, blue)
            drawP(6 * p, 9.5f * p, 3 * p, 2.5f * p, blue)
            // Shoes
            drawP(1.5f * p, 11f * p, 3.5f * p, 2 * p, brown)
            drawP(6f * p, 10.5f * p, 3.5f * p, 2 * p, brown)
        } else if (isWallSliding) {
            // Wall slide pose: Hand gripping wall, leaning
            drawP(3 * p, 6 * p, 5 * p, 4 * p, blue)
            drawP(4 * p, 6.5f * p, 1 * p, 1 * p, yellow)
            // Hand on wall
            drawP(8.5f * p, 5 * p, 2.5f * p, 2.5f * p, white)
            // Legs
            drawP(3 * p, 10 * p, 2.5f * p, 3 * p, blue)
            drawP(6 * p, 9.5f * p, 2.5f * p, 2.5f * p, blue)
            drawP(2.5f * p, 12f * p, 3f * p, 2 * p, brown)
            drawP(6f * p, 11f * p, 3f * p, 2 * p, brown)
        } else if (isDashing) {
            // Streamlined Dash pose
            drawP(2 * p, 5.5f * p, 7 * p, 3.5f * p, blue)
            drawP(0 * p, 6 * p, 3 * p, 2.5f * p, red)
            drawP(-1 * p, 6 * p, 2 * p, 2 * p, white)
            drawP(8 * p, 6 * p, 3 * p, 2 * p, white)
            // Legs stretched
            drawP(1 * p, 8.5f * p, 6 * p, 2.5f * p, blue)
            drawP(0 * p, 9.5f * p, 3.5f * p, 2 * p, brown)
            drawP(5 * p, 9.5f * p, 3.5f * p, 2 * p, brown)
        } else if (isWon) {
            // Victory pose: Two hands up!
            drawP(3 * p, 6 * p, 5 * p, 4 * p, blue)
            drawP(4 * p, 6.5f * p, 1 * p, 1 * p, yellow)
            drawP(6.5f * p, 6.5f * p, 1 * p, 1 * p, yellow)
            // Both hands up
            drawP(0.5f * p, 4 * p, 2.5f * p, 2.5f * p, white)
            drawP(8f * p, 4 * p, 2.5f * p, 2.5f * p, white)
            // Legs standing proud
            drawP(2.5f * p, 10 * p, 2.5f * p, 3 * p, blue)
            drawP(6 * p, 10 * p, 2.5f * p, 3 * p, blue)
            drawP(1.5f * p, 12f * p, 3.5f * p, 2 * p, brown)
            drawP(6 * p, 12f * p, 3.5f * p, 2 * p, brown)
        } else if (isRunning) {
            // Run cycle (3 frames)
            val frame = player.runFrame % 3
            drawP(3 * p, 6 * p, 5 * p, 4 * p, blue)
            drawP(4 * p, 6.5f * p, 1 * p, 1 * p, yellow)
            drawP(6.5f * p, 6.5f * p, 1 * p, 1 * p, yellow)

            when (frame) {
                0 -> {
                    // Stride A: Left leg forward, right leg back
                    drawP(1.5f * p, 9.5f * p, 3 * p, 3 * p, blue)
                    drawP(6.5f * p, 9.5f * p, 3 * p, 2.5f * p, blue)
                    drawP(0.5f * p, 12f * p, 3.5f * p, 2 * p, brown)
                    drawP(7f * p, 11f * p, 3.5f * p, 2 * p, brown)
                    // Arms
                    drawP(1 * p, 6.5f * p, 2.5f * p, 2.5f * p, white)
                    drawP(7.5f * p, 6f * p, 2.5f * p, 2.5f * p, white)
                }
                1 -> {
                    // Contact frame: Both legs under body
                    drawP(3 * p, 10 * p, 2.5f * p, 2.5f * p, blue)
                    drawP(5.5f * p, 10 * p, 2.5f * p, 2.5f * p, blue)
                    drawP(2 * p, 12.5f * p, 3.5f * p, 1.8f * p, brown)
                    drawP(5.5f * p, 12.5f * p, 3.5f * p, 1.8f * p, brown)
                    drawP(1.5f * p, 7f * p, 2.5f * p, 2.5f * p, white)
                    drawP(7f * p, 7f * p, 2.5f * p, 2.5f * p, white)
                }
                else -> {
                    // Stride B: Right leg forward, left leg back
                    drawP(6f * p, 9.5f * p, 3 * p, 3 * p, blue)
                    drawP(2f * p, 9.5f * p, 3 * p, 2.5f * p, blue)
                    drawP(6f * p, 12f * p, 3.5f * p, 2 * p, brown)
                    drawP(1f * p, 11f * p, 3.5f * p, 2 * p, brown)
                    drawP(7.5f * p, 6.5f * p, 2.5f * p, 2.5f * p, white)
                    drawP(1 * p, 6f * p, 2.5f * p, 2.5f * p, white)
                }
            }
        } else {
            // Idle standing
            drawP(3 * p, 6 * p, 5 * p, 4 * p, blue)
            drawP(4 * p, 6.5f * p, 1 * p, 1 * p, yellow)
            drawP(6.5f * p, 6.5f * p, 1 * p, 1 * p, yellow)
            // Arms by side
            drawP(1.5f * p, 6.5f * p, 2.5f * p, 2.5f * p, white)
            drawP(7f * p, 6.5f * p, 2.5f * p, 2.5f * p, white)
            // Legs standing
            drawP(2.5f * p, 10 * p, 2.5f * p, 3 * p, blue)
            drawP(6 * p, 10 * p, 2.5f * p, 3 * p, blue)
            drawP(1.5f * p, 12f * p, 3.5f * p, 2 * p, brown)
            drawP(6 * p, 12f * p, 3.5f * p, 2 * p, brown)
        }
    }

    private fun DrawScope.drawParticle(p: Particle, ts: Float) {
        val px = p.x * ts
        val py = p.y * ts
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val pSize = p.size * ts

        when (p.type) {
            ParticleType.DUST -> {
                drawCircle(color = p.color.copy(alpha = alpha * 0.7f), radius = pSize * 0.5f, center = Offset(px, py))
            }
            ParticleType.SPARKLE -> {
                val path = Path().apply {
                    moveTo(px, py - pSize)
                    lineTo(px + pSize * 0.3f, py)
                    lineTo(px, py + pSize)
                    lineTo(px - pSize * 0.3f, py)
                    close()
                }
                drawPath(path, color = p.color.copy(alpha = alpha))
            }
            ParticleType.CONFETTI -> {
                drawRect(color = p.color.copy(alpha = alpha), topLeft = Offset(px, py), size = Size(pSize, pSize * 0.7f))
            }
            ParticleType.GHOST_TRAIL -> {
                drawCircle(color = p.color.copy(alpha = alpha * 0.4f), radius = pSize, center = Offset(px, py))
            }
        }
    }

    private fun DrawScope.drawFloatingText(ft: FloatingText, ts: Float) {
        // Floating visual marker / star burst
        val px = ft.x * ts
        val py = ft.y * ts
        val alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
        drawCircle(color = ft.color.copy(alpha = alpha * 0.4f), radius = ts * 0.35f, center = Offset(px, py))
    }
}
