package com.example.model

import androidx.compose.ui.graphics.Color

enum class TileType(val id: Int, val isSolid: Boolean, val isHazard: Boolean) {
    EMPTY(0, false, false),
    GROUND(1, true, false),
    BRICK(2, true, false),
    QUESTION_BLOCK(3, true, false),
    HARD_BLOCK(4, true, false),
    ICE_BLOCK(5, true, false),
    SPIKE(6, false, true),
    LAVA(7, false, true),
    SPRING(8, true, false),
    PLATFORM_THIN(9, false, false), // Custom handled one-way
    CHECKPOINT(10, false, false),
    GOAL_POLE(11, false, false),
    CASTLE_DOOR(12, false, false);

    companion object {
        private val map = entries.associateBy { it.id }
        fun fromId(id: Int): TileType = map[id] ?: EMPTY
    }
}

enum class LevelTheme(
    val displayName: String,
    val skyColorTop: Color,
    val skyColorBottom: Color,
    val hillColor: Color,
    val cloudColor: Color,
    val groundTopColor: Color,
    val groundBodyColor: Color,
    val brickColor: Color
) {
    GRASSLAND(
        displayName = "Grassland Heights",
        skyColorTop = Color(0xFF64B5F6),
        skyColorBottom = Color(0xFFBBDEFB),
        hillColor = Color(0xFF43A047),
        cloudColor = Color(0xFFFFFFFF),
        groundTopColor = Color(0xFF4CAF50),
        groundBodyColor = Color(0xFF8D6E63),
        brickColor = Color(0xFFD32F2F)
    ),
    BRICK_ALLEY(
        displayName = "Brick Alley",
        skyColorTop = Color(0xFF3949AB),
        skyColorBottom = Color(0xFF7986CB),
        hillColor = Color(0xFF283593),
        cloudColor = Color(0xFFE8EAF6),
        groundTopColor = Color(0xFF5C6BC0),
        groundBodyColor = Color(0xFF3F51B5),
        brickColor = Color(0xFFE53935)
    ),
    SKY_MUSHROOM(
        displayName = "Skyline Springs",
        skyColorTop = Color(0xFFF06292),
        skyColorBottom = Color(0xFFFFCC80),
        hillColor = Color(0xFFBA68C8),
        cloudColor = Color(0xFFFFF9C4),
        groundTopColor = Color(0xFFAB47BC),
        groundBodyColor = Color(0xFF7B1FA2),
        brickColor = Color(0xFFFF7043)
    ),
    LAVA_CASTLE(
        displayName = "Lava Fortress",
        skyColorTop = Color(0xFF212121),
        skyColorBottom = Color(0xFFBF360C),
        hillColor = Color(0xFF37474F),
        cloudColor = Color(0xFFFF8A65),
        groundTopColor = Color(0xFF455A64),
        groundBodyColor = Color(0xFF263238),
        brickColor = Color(0xFFD84315)
    ),
    FROZEN_PEAKS(
        displayName = "Frozen Peaks",
        skyColorTop = Color(0xFF00ACC1),
        skyColorBottom = Color(0xFFE0F7FA),
        hillColor = Color(0xFF00838F),
        cloudColor = Color(0xFFFFFFFF),
        groundTopColor = Color(0xFF80DEEA),
        groundBodyColor = Color(0xFF006064),
        brickColor = Color(0xFF26A69A)
    ),
    MASTER_GAUNTLET(
        displayName = "The Final Gauntlet",
        skyColorTop = Color(0xFF4A148C),
        skyColorBottom = Color(0xFFE040FB),
        hillColor = Color(0xFF311B92),
        cloudColor = Color(0xFFF3E5F5),
        groundTopColor = Color(0xFFAB47BC),
        groundBodyColor = Color(0xFF4A148C),
        brickColor = Color(0xFFFFD600)
    )
}

data class DynamicCoin(
    val id: Int,
    var x: Float,
    var y: Float,
    var isCollected: Boolean = false,
    var animFrame: Float = 0f
)

data class DynamicStar(
    val id: Int,
    var x: Float,
    var y: Float,
    var isCollected: Boolean = false
)

data class MovingPlatform(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float = 0.5f,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val speed: Float = 2.0f,
    var movingToEnd: Boolean = true,
    var vx: Float = 0f,
    var vy: Float = 0f
)

data class CrumblingPlatform(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float = 1.0f,
    val height: Float = 0.5f,
    var isSteppedOn: Boolean = false,
    var timer: Float = 0f,
    var isCollapsed: Boolean = false,
    var respawnTimer: Float = 0f,
    var shakeX: Float = 0f
)

enum class ParticleType {
    DUST, SPARKLE, CONFETTI, GHOST_TRAIL
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val type: ParticleType = ParticleType.DUST
)

data class FloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var life: Float = 1.0f,
    val maxLife: Float = 1.0f
)

data class Checkpoint(
    val x: Float,
    val y: Float,
    var isActivated: Boolean = false
)

data class PlayerState(
    var x: Float = 2f,
    var y: Float = 2f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val width: Float = 0.75f,
    val height: Float = 0.95f,
    var isGrounded: Boolean = false,
    var isWallSliding: Boolean = false,
    var wallSlideSide: Int = 0, // -1 for left wall, 1 for right wall
    var facingRight: Boolean = true,
    var isDashing: Boolean = false,
    var dashTimer: Float = 0f,
    var dashCooldown: Float = 0f,
    var isDead: Boolean = false,
    var respawnTimer: Float = 0f,
    var hasWon: Boolean = false,
    var winTimer: Float = 0f,
    var runAnimTimer: Float = 0f,
    var runFrame: Int = 0,
    var wallJumpGraceTimer: Float = 0f,
    var coyoteTimer: Float = 0f,
    var jumpBufferTimer: Float = 0f,
    val trail: MutableList<Pair<Float, Float>> = mutableListOf()
)

data class LevelData(
    val id: Int,
    val title: String,
    val subtitle: String,
    val theme: LevelTheme,
    val mapWidth: Int,
    val mapHeight: Int,
    val grid: Array<IntArray>,
    val startX: Float,
    val startY: Float,
    val coins: List<DynamicCoin>,
    val stars: List<DynamicStar>,
    val movingPlatforms: List<MovingPlatform>,
    val crumblingPlatforms: List<CrumblingPlatform>,
    val checkpoints: List<Checkpoint>,
    val parTimeSeconds: Float = 30.0f
)

data class LevelProgress(
    val levelId: Int,
    val isUnlocked: Boolean = false,
    val starsEarned: Int = 0,
    val bestTimeSeconds: Float = 0f,
    val coinsCollected: Int = 0
)
