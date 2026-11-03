package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.audio.RetroSoundManager
import com.example.data.GamePreferences
import com.example.model.*
import kotlin.math.abs
import kotlin.math.sin

class GameEngine(
    var level: LevelData,
    private val soundManager: RetroSoundManager,
    private val preferences: GamePreferences
) {
    var player = PlayerState(x = level.startX, y = level.startY)
    var currentCheckpoint: Checkpoint? = null

    var gameTime: Float = 0f
    var coinsCollected: Int = 0
    var starsCollected: Int = 0
    var isPaused: Boolean = false
    var levelCompleted: Boolean = false

    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    // Camera targets
    var cameraX: Float = level.startX
    var cameraY: Float = level.startY

    // Physics constants
    private val gravity = 27f
    private val maxFallSpeed = 16f
    private val wallSlideSpeed = 3.0f
    private val runAccel = 45f
    private val runFriction = 30f
    private val runMaxSpeed = 7.0f
    private val jumpForce = 11.2f
    private val wallJumpForceY = 10.8f
    private val wallJumpForceX = 7.5f
    private val springForce = 17.5f
    private val dashSpeed = 15.0f

    // Input States
    var inputLeft = false
    var inputRight = false
    var inputJump = false
    var inputDash = false

    private var prevJumpInput = false
    private var prevDashInput = false

    init {
        resetToStart()
    }

    fun resetToStart() {
        player = PlayerState(x = level.startX, y = level.startY)
        currentCheckpoint = null
        cameraX = level.startX
        cameraY = level.startY
        gameTime = 0f
        coinsCollected = 0
        starsCollected = 0
        levelCompleted = false
        particles.clear()
        floatingTexts.clear()

        // Reset dynamic entities in level
        for (coin in level.coins) coin.isCollected = false
        for (star in level.stars) star.isCollected = false
        for (cp in level.checkpoints) cp.isActivated = false
        for (cr in level.crumblingPlatforms) {
            cr.isSteppedOn = false
            cr.timer = 0f
            cr.isCollapsed = false
            cr.respawnTimer = 0f
            cr.shakeX = 0f
        }
    }

    fun respawnAtCheckpoint() {
        val spawnX = currentCheckpoint?.x ?: level.startX
        val spawnY = currentCheckpoint?.y ?: level.startY
        player.x = spawnX
        player.y = spawnY
        player.vx = 0f
        player.vy = 0f
        player.isDead = false
        player.respawnTimer = 0f
        player.isDashing = false
        player.isWallSliding = false
        player.isGrounded = false
        player.trail.clear()
    }

    fun update(dt: Float) {
        if (isPaused || levelCompleted) {
            // Update win celebratory particles
            if (levelCompleted) {
                updateWinCelebration(dt)
            }
            return
        }

        // Clamp delta time to avoid large physics steps
        val delta = dt.coerceIn(0.001f, 0.04f)
        gameTime += delta

        // Check if player is dead / respawning
        if (player.isDead) {
            player.respawnTimer += delta
            if (player.respawnTimer > 0.8f) {
                respawnAtCheckpoint()
            }
            return
        }

        // Handle Jump Input Buffering & Coyote Time
        val jumpJustPressed = inputJump && !prevJumpInput
        prevJumpInput = inputJump

        val dashJustPressed = inputDash && !prevDashInput
        prevDashInput = inputDash

        if (jumpJustPressed) {
            player.jumpBufferTimer = 0.12f
        } else if (player.jumpBufferTimer > 0f) {
            player.jumpBufferTimer -= delta
        }

        if (player.isGrounded) {
            player.coyoteTimer = 0.1f
        } else if (player.coyoteTimer > 0f) {
            player.coyoteTimer -= delta
        }

        // Dash Handling
        if (player.dashCooldown > 0f) player.dashCooldown -= delta

        if (dashJustPressed && player.dashCooldown <= 0f && !player.isDashing) {
            player.isDashing = true
            player.dashTimer = 0.18f
            player.dashCooldown = 0.6f
            val dashDir = if (player.facingRight) 1f else -1f
            player.vx = dashDir * dashSpeed
            player.vy = 0f
            soundManager.playDash()
            spawnDashEffect(player.x, player.y)
        }

        if (player.isDashing) {
            player.dashTimer -= delta
            // Spawn motion trail
            player.trail.add(Pair(player.x, player.y))
            if (player.trail.size > 5) player.trail.removeAt(0)

            if (player.dashTimer <= 0f) {
                player.isDashing = false
                player.vx = (if (player.facingRight) 1f else -1f) * runMaxSpeed
                player.trail.clear()
            }
        } else {
            if (player.trail.isNotEmpty()) player.trail.clear()

            // Horizontal Movement & Acceleration
            val targetMove = when {
                inputLeft && !inputRight -> -1f
                inputRight && !inputLeft -> 1f
                else -> 0f
            }

            if (targetMove != 0f) {
                player.facingRight = targetMove > 0
                val targetSpeed = targetMove * runMaxSpeed
                if (targetMove > 0) {
                    player.vx = minOf(targetSpeed, player.vx + runAccel * delta)
                } else {
                    player.vx = maxOf(targetSpeed, player.vx - runAccel * delta)
                }
            } else {
                // Apply friction
                if (player.vx > 0f) {
                    player.vx = maxOf(0f, player.vx - runFriction * delta)
                } else if (player.vx < 0f) {
                    player.vx = minOf(0f, player.vx + runFriction * delta)
                }
            }

            // Gravity
            val appliedGravity = if (player.isWallSliding && player.vy > 0f) {
                gravity * 0.35f
            } else {
                gravity
            }
            player.vy = minOf(maxFallSpeed, player.vy + appliedGravity * delta)

            // Clamp wall slide descent speed
            if (player.isWallSliding && player.vy > wallSlideSpeed) {
                player.vy = wallSlideSpeed
                // Spawn wall slide dust puffs
                if (Math.random() < 0.3) {
                    val dustX = if (player.wallSlideSide == 1) player.x + player.width else player.x
                    spawnDust(dustX, player.y + player.height * 0.8f, 1)
                }
            }
        }

        // Jump Handling
        if (player.jumpBufferTimer > 0f) {
            if (player.coyoteTimer > 0f || player.isGrounded) {
                // Standard Ground Jump
                player.vy = -jumpForce
                player.isGrounded = false
                player.coyoteTimer = 0f
                player.jumpBufferTimer = 0f
                soundManager.playJump()
                spawnDust(player.x + player.width * 0.5f, player.y + player.height, 4)
            } else if (player.isWallSliding || player.wallJumpGraceTimer > 0f) {
                // PARKOUR WALL JUMP!
                val kickDir = -player.wallSlideSide
                player.vy = -wallJumpForceY
                player.vx = kickDir * wallJumpForceX
                player.facingRight = kickDir > 0
                player.isWallSliding = false
                player.wallJumpGraceTimer = 0f
                player.jumpBufferTimer = 0f
                soundManager.playWallJump()
                spawnWallKickEffect(player.x + player.width * 0.5f, player.y + player.height * 0.5f)
                addFloatingText("WALL JUMP!", player.x, player.y - 0.5f, Color(0xFFFFEB3B))
            }
        }

        // Variable Jump Height (releasing jump early cuts upward velocity)
        if (!inputJump && player.vy < -3.0f && !player.isDashing) {
            player.vy *= 0.65f
        }

        // Update Moving Platforms
        for (mp in level.movingPlatforms) {
            val prevX = mp.x
            val prevY = mp.y
            val dx = mp.endX - mp.startX
            val dy = mp.endY - mp.startY
            val totalDist = kotlin.math.hypot(dx, dy)
            if (totalDist > 0.01f) {
                val step = (mp.speed * delta) / totalDist
                if (mp.movingToEnd) {
                    mp.x += dx * step
                    mp.y += dy * step
                    val distCurrent = kotlin.math.hypot(mp.x - mp.startX, mp.y - mp.startY)
                    if (distCurrent >= totalDist) {
                        mp.x = mp.endX
                        mp.y = mp.endY
                        mp.movingToEnd = false
                    }
                } else {
                    mp.x -= dx * step
                    mp.y -= dy * step
                    val distCurrent = kotlin.math.hypot(mp.x - mp.startX, mp.y - mp.startY)
                    if (distCurrent <= 0.05f) {
                        mp.x = mp.startX
                        mp.y = mp.startY
                        mp.movingToEnd = true
                    }
                }
            }
            mp.vx = (mp.x - prevX) / delta
            mp.vy = (mp.y - prevY) / delta
        }

        // Update Crumbling Platforms
        for (cp in level.crumblingPlatforms) {
            if (cp.isSteppedOn && !cp.isCollapsed) {
                cp.timer += delta
                cp.shakeX = sin(cp.timer * 40f) * 0.08f
                if (cp.timer >= 0.45f) {
                    cp.isCollapsed = true
                    cp.respawnTimer = 3.5f
                    spawnDust(cp.x + cp.width * 0.5f, cp.y, 6)
                }
            } else if (cp.isCollapsed) {
                cp.respawnTimer -= delta
                if (cp.respawnTimer <= 0f) {
                    cp.isCollapsed = false
                    cp.isSteppedOn = false
                    cp.timer = 0f
                    cp.shakeX = 0f
                }
            }
        }

        // Physics Integration & Tile Collision Passes
        // Step 1: X-Axis Move & Collision
        player.x += player.vx * delta
        checkCollisionsX()

        // Step 2: Y-Axis Move & Collision
        player.y += player.vy * delta
        checkCollisionsY()

        // Step 3: Check Moving Platform Carrier
        checkMovingPlatformInteraction()

        // Step 4: Check Crumbling Platform Interaction
        checkCrumblingPlatformInteraction()

        // Step 5: Check Wall Slide State
        checkWallSlideState(delta)

        // Step 6: Hazards & Bounds Check
        checkHazards()

        // Step 7: Collectibles & Triggers
        checkCollectibles()

        // Run animation cycle
        if (player.isGrounded && abs(player.vx) > 0.5f) {
            player.runAnimTimer += delta * 12f
            player.runFrame = (player.runAnimTimer.toInt()) % 3
            if (player.runFrame == 0 && Math.random() < 0.2) {
                spawnDust(player.x + player.width * 0.5f, player.y + player.height, 1)
            }
        } else {
            player.runAnimTimer = 0f
            player.runFrame = 0
        }

        // Update Particles
        val particleIter = particles.iterator()
        while (particleIter.hasNext()) {
            val p = particleIter.next()
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.life -= delta
            if (p.life <= 0f) particleIter.remove()
        }

        // Update Floating Texts
        val ftIter = floatingTexts.iterator()
        while (ftIter.hasNext()) {
            val ft = ftIter.next()
            ft.y -= delta * 0.8f
            ft.life -= delta
            if (ft.life <= 0f) ftIter.remove()
        }

        // Smooth Camera Follow
        val targetCamX = player.x + (if (player.facingRight) 1.5f else -1.5f)
        val targetCamY = player.y - 0.5f
        cameraX += (targetCamX - cameraX) * 0.12f
        cameraY += (targetCamY - cameraY) * 0.12f

        // Clamp camera to map boundaries
        cameraX = cameraX.coerceIn(4f, level.mapWidth - 4f)
        cameraY = cameraY.coerceIn(3f, level.mapHeight - 3f)
    }

    private fun checkCollisionsX() {
        val minX = player.x
        val maxX = player.x + player.width
        val minY = player.y + 0.05f
        val maxY = player.y + player.height - 0.05f

        val startTileX = minX.toInt().coerceIn(0, level.mapWidth - 1)
        val endTileX = maxX.toInt().coerceIn(0, level.mapWidth - 1)
        val startTileY = minY.toInt().coerceIn(0, level.mapHeight - 1)
        val endTileY = maxY.toInt().coerceIn(0, level.mapHeight - 1)

        for (ty in startTileY..endTileY) {
            for (tx in startTileX..endTileX) {
                val tile = TileType.fromId(level.grid[ty][tx])
                if (tile.isSolid) {
                    if (player.vx > 0) {
                        player.x = tx - player.width
                        player.vx = 0f
                    } else if (player.vx < 0) {
                        player.x = tx + 1f
                        player.vx = 0f
                    }
                }
            }
        }
    }

    private fun checkCollisionsY() {
        val minX = player.x + 0.05f
        val maxX = player.x + player.width - 0.05f
        val minY = player.y
        val maxY = player.y + player.height

        val startTileX = minX.toInt().coerceIn(0, level.mapWidth - 1)
        val endTileX = maxX.toInt().coerceIn(0, level.mapWidth - 1)
        val startTileY = minY.toInt().coerceIn(0, level.mapHeight - 1)
        val endTileY = maxY.toInt().coerceIn(0, level.mapHeight - 1)

        var hitGround = false

        for (ty in startTileY..endTileY) {
            for (tx in startTileX..endTileX) {
                val tile = TileType.fromId(level.grid[ty][tx])
                if (tile.isSolid) {
                    if (player.vy > 0) {
                        // Landing on top of tile
                        player.y = ty - player.height
                        player.vy = 0f
                        hitGround = true

                        // Check Spring Pad
                        if (tile == TileType.SPRING) {
                            player.vy = -springForce
                            hitGround = false
                            soundManager.playSpring()
                            spawnSpringEffect(tx + 0.5f, ty.toFloat())
                            addFloatingText("SPRING BOUNCE!", player.x, player.y - 0.8f, Color(0xFFFF5252))
                        }
                    } else if (player.vy < 0) {
                        // Hitting head on ceiling
                        player.y = ty + 1f
                        player.vy = 0f

                        // Question Block Hit
                        if (tile == TileType.QUESTION_BLOCK) {
                            level.grid[ty][tx] = TileType.HARD_BLOCK.id
                            coinsCollected += 1
                            soundManager.playCoin()
                            spawnSparkles(tx + 0.5f, ty.toFloat(), 6)
                            addFloatingText("+100", tx + 0.5f, ty - 0.5f, Color(0xFFFFD700))
                        }
                    }
                }
            }
        }
        player.isGrounded = hitGround
    }

    private fun checkMovingPlatformInteraction() {
        for (mp in level.movingPlatforms) {
            val playerFoot = player.y + player.height
            val withinX = (player.x + player.width > mp.x) && (player.x < mp.x + mp.width)
            val standingOnTop = withinX && (abs(playerFoot - mp.y) < 0.22f) && (player.vy >= 0f)

            if (standingOnTop) {
                player.y = mp.y - player.height
                player.vy = 0f
                player.isGrounded = true
                // Carry player with platform
                player.x += mp.vx * 0.016f
            }
        }
    }

    private fun checkCrumblingPlatformInteraction() {
        for (cp in level.crumblingPlatforms) {
            if (cp.isCollapsed) continue
            val playerFoot = player.y + player.height
            val withinX = (player.x + player.width > cp.x) && (player.x < cp.x + cp.width)
            val standingOnTop = withinX && (abs(playerFoot - cp.y) < 0.2f) && (player.vy >= 0f)

            if (standingOnTop) {
                player.y = cp.y - player.height
                player.vy = 0f
                player.isGrounded = true
                if (!cp.isSteppedOn) {
                    cp.isSteppedOn = true
                }
            }
        }
    }

    private fun checkWallSlideState(delta: Float) {
        val checkY = (player.y + player.height * 0.5f).toInt().coerceIn(0, level.mapHeight - 1)
        val leftTileX = (player.x - 0.12f).toInt()
        val rightTileX = (player.x + player.width + 0.12f).toInt()

        val touchingLeftWall = leftTileX >= 0 && TileType.fromId(level.grid[checkY][leftTileX]).isSolid
        val touchingRightWall = rightTileX < level.mapWidth && TileType.fromId(level.grid[checkY][rightTileX]).isSolid

        if (!player.isGrounded && player.vy > 0f) {
            if (touchingLeftWall && (inputLeft || player.facingRight.not())) {
                player.isWallSliding = true
                player.wallSlideSide = -1
                player.wallJumpGraceTimer = 0.15f
            } else if (touchingRightWall && (inputRight || player.facingRight)) {
                player.isWallSliding = true
                player.wallSlideSide = 1
                player.wallJumpGraceTimer = 0.15f
            } else {
                player.isWallSliding = false
            }
        } else {
            player.isWallSliding = false
        }

        if (player.wallJumpGraceTimer > 0f && !player.isWallSliding) {
            player.wallJumpGraceTimer -= delta
        }
    }

    private fun checkHazards() {
        // Fall into abyss
        if (player.y > level.mapHeight + 1) {
            triggerDeath()
            return
        }

        val checkTileX = (player.x + player.width * 0.5f).toInt().coerceIn(0, level.mapWidth - 1)
        val checkTileY = (player.y + player.height * 0.5f).toInt().coerceIn(0, level.mapHeight - 1)
        val currentTile = TileType.fromId(level.grid[checkTileY][checkTileX])

        if (currentTile.isHazard) {
            triggerDeath()
        }
    }

    private fun triggerDeath() {
        if (player.isDead) return
        player.isDead = true
        player.respawnTimer = 0f
        soundManager.playDeath()
        spawnDust(player.x + player.width * 0.5f, player.y + player.height * 0.5f, 12)
        addFloatingText("OUCH!", player.x, player.y - 0.5f, Color(0xFFFF1744))
    }

    private fun checkCollectibles() {
        // Coins
        for (coin in level.coins) {
            if (!coin.isCollected) {
                val dx = abs(coin.x + 0.5f - (player.x + player.width * 0.5f))
                val dy = abs(coin.y + 0.5f - (player.y + player.height * 0.5f))
                if (dx < 0.7f && dy < 0.8f) {
                    coin.isCollected = true
                    coinsCollected += 1
                    soundManager.playCoin()
                    spawnSparkles(coin.x + 0.5f, coin.y + 0.5f, 8)
                    addFloatingText("+100", coin.x, coin.y - 0.5f, Color(0xFFFFD700))
                }
            }
        }

        // Stars
        for (star in level.stars) {
            if (!star.isCollected) {
                val dx = abs(star.x + 0.5f - (player.x + player.width * 0.5f))
                val dy = abs(star.y + 0.5f - (player.y + player.height * 0.5f))
                if (dx < 0.8f && dy < 0.9f) {
                    star.isCollected = true
                    starsCollected += 1
                    soundManager.playCheckpoint()
                    spawnSparkles(star.x + 0.5f, star.y + 0.5f, 16)
                    addFloatingText("STAR!", star.x, star.y - 0.8f, Color(0xFFFFEB3B))
                }
            }
        }

        // Checkpoints
        for (cp in level.checkpoints) {
            if (!cp.isActivated) {
                val dx = abs(cp.x + 0.5f - (player.x + player.width * 0.5f))
                val dy = abs(cp.y + 0.5f - (player.y + player.height * 0.5f))
                if (dx < 0.8f && dy < 1.0f) {
                    cp.isActivated = true
                    currentCheckpoint = cp
                    soundManager.playCheckpoint()
                    spawnSparkles(cp.x + 0.5f, cp.y, 10)
                    addFloatingText("CHECKPOINT!", cp.x, cp.y - 0.8f, Color(0xFF64FFDA))
                }
            }
        }

        // Goal Check (Flagpole or Castle door)
        val playerTileX = (player.x + player.width * 0.5f).toInt().coerceIn(0, level.mapWidth - 1)
        val playerTileY = (player.y + player.height * 0.5f).toInt().coerceIn(0, level.mapHeight - 1)
        val tile = TileType.fromId(level.grid[playerTileY][playerTileX])

        if (!levelCompleted && (tile == TileType.GOAL_POLE || tile == TileType.CASTLE_DOOR)) {
            triggerWin()
        }
    }

    private fun triggerWin() {
        levelCompleted = true
        player.hasWon = true
        soundManager.playLevelClear()

        // Calculate stars earned
        val earnedStars = maxOf(1, minOf(3, starsCollected + (if (coinsCollected >= 5) 1 else 0)))
        preferences.saveLevelCompletion(level.id, earnedStars, gameTime, coinsCollected)

        // Spawn confetti celebration
        for (i in 0 until 40) {
            spawnConfetti(player.x + (Math.random() * 4.0 - 2.0).toFloat(), player.y - (Math.random() * 3.0).toFloat())
        }
        addFloatingText("LEVEL CLEAR!!", player.x - 1f, player.y - 1.5f, Color(0xFFFFEB3B))
    }

    private fun updateWinCelebration(delta: Float) {
        if (Math.random() < 0.15) {
            spawnConfetti(player.x + (Math.random() * 6.0 - 3.0).toFloat(), player.y - (Math.random() * 4.0).toFloat())
        }
    }

    private fun spawnDust(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.random() * 2.0 - 1.0).toFloat(),
                    vy = -(Math.random() * 1.5).toFloat(),
                    color = Color(0xFFCFD8DC),
                    life = 0.25f + (Math.random() * 0.2f).toFloat(),
                    maxLife = 0.45f,
                    size = 0.15f + (Math.random() * 0.1f).toFloat(),
                    type = ParticleType.DUST
                )
            )
        }
    }

    private fun spawnWallKickEffect(x: Float, y: Float) {
        spawnDust(x, y, 6)
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = -1f,
                color = Color(0xFFFFD54F),
                life = 0.35f,
                maxLife = 0.35f,
                size = 0.4f,
                type = ParticleType.SPARKLE
            )
        )
    }

    private fun spawnDashEffect(x: Float, y: Float) {
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = x + (Math.random() * 0.4 - 0.2).toFloat(),
                    y = y + (Math.random() * 0.6 + 0.2).toFloat(),
                    vx = (if (player.facingRight) -1f else 1f) * (2f + (Math.random() * 2f).toFloat()),
                    vy = (Math.random() * 1.0 - 0.5).toFloat(),
                    color = Color(0xFF00E5FF),
                    life = 0.3f,
                    maxLife = 0.3f,
                    size = 0.2f,
                    type = ParticleType.GHOST_TRAIL
                )
            )
        }
    }

    private fun spawnSpringEffect(x: Float, y: Float) {
        spawnDust(x, y, 8)
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.random() * 3.0 - 1.5).toFloat(),
                    vy = -(Math.random() * 3.0 + 1.0).toFloat(),
                    color = Color(0xFFFFD700),
                    life = 0.4f,
                    maxLife = 0.4f,
                    size = 0.2f,
                    type = ParticleType.SPARKLE
                )
            )
        }
    }

    private fun spawnSparkles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.random() * 3.0 - 1.5).toFloat(),
                    vy = (Math.random() * 3.0 - 1.5).toFloat(),
                    color = if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFFFFF9C4),
                    life = 0.4f + (Math.random() * 0.2f).toFloat(),
                    maxLife = 0.6f,
                    size = 0.22f,
                    type = ParticleType.SPARKLE
                )
            )
        }
    }

    private fun spawnConfetti(x: Float, y: Float) {
        val colors = listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFE040FB))
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = (Math.random() * 4.0 - 2.0).toFloat(),
                vy = (Math.random() * 2.0 - 1.0).toFloat(),
                color = colors[(Math.random() * colors.size).toInt()],
                life = 0.8f + (Math.random() * 0.4f).toFloat(),
                maxLife = 1.2f,
                size = 0.25f,
                type = ParticleType.CONFETTI
            )
        )
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Color) {
        floatingTexts.add(FloatingText(text, x, y, color))
    }
}
