package com.example.levels

import com.example.model.*

object LevelRepository {

    fun getLevel(id: Int): LevelData {
        return when (id) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            4 -> createLevel4()
            5 -> createLevel5()
            6 -> createLevel6()
            else -> createLevel1()
        }
    }

    val totalLevels: Int = 6

    fun getAllLevelInfo(): List<LevelInfo> = listOf(
        LevelInfo(1, "Green Hill Hop", "Learn the Basics & Jump!", LevelTheme.GRASSLAND, 1, 30f),
        LevelInfo(2, "Brick Alley Bounce", "Master the Wall Jump!", LevelTheme.BRICK_ALLEY, 2, 35f),
        LevelInfo(3, "Skyline Springs", "Springs & Crumbling Clouds", LevelTheme.SKY_MUSHROOM, 3, 40f),
        LevelInfo(4, "Lava Fortress", "Dodge the Molten Pit", LevelTheme.LAVA_CASTLE, 4, 45f),
        LevelInfo(5, "Frozen Peaks", "Slippery Momentum & Ice Leap", LevelTheme.FROZEN_PEAKS, 4, 40f),
        LevelInfo(6, "The Final Gauntlet", "The Ultimate Parkour Trial", LevelTheme.MASTER_GAUNTLET, 5, 50f)
    )

    data class LevelInfo(
        val id: Int,
        val title: String,
        val subtitle: String,
        val theme: LevelTheme,
        val difficulty: Int, // 1 to 5 stars
        val targetTime: Float
    )

    // Helper to generate a blank grid
    private fun createGrid(width: Int, height: Int): Array<IntArray> {
        return Array(height) { IntArray(width) { 0 } }
    }

    // Level 1: Green Hill Hop
    private fun createLevel1(): LevelData {
        val w = 55
        val h = 14
        val grid = createGrid(w, h)

        // Ground floor with a few gaps
        for (x in 0 until 14) grid[12][x] = 1
        for (x in 0 until 14) grid[13][x] = 1

        // Pit between 14..17 (hazard at bottom)
        grid[13][14] = 7
        grid[13][15] = 7
        grid[13][16] = 7

        // Island 18..24
        for (x in 18..24) {
            grid[12][x] = 1
            grid[13][x] = 1
        }

        // Stepping stones 21..24 elevated
        grid[10][21] = 2
        grid[10][22] = 3 // Question block
        grid[10][23] = 2

        // Pit 25..29 (moving platform here)
        for (x in 25..29) grid[13][x] = 7

        // Island 30..37
        for (x in 30..37) {
            grid[12][x] = 1
            grid[13][x] = 1
        }
        // High wall at 37
        for (y in 7..11) grid[y][37] = 2

        // Spring at 34 to jump over wall
        grid[11][34] = 8

        // High ground 38..45
        for (x in 38..45) {
            grid[8][x] = 1
            for (y in 9..13) grid[y][x] = 4 // stone fill
        }

        // Final floor 46..54
        for (x in 46..54) {
            grid[12][x] = 1
            grid[13][x] = 1
        }

        // Flagpole at 49, Castle at 52..54
        for (y in 4..11) grid[y][49] = 11
        grid[11][52] = 12 // Castle door
        for (y in 9..11) grid[y][53] = 4
        for (y in 9..11) grid[y][54] = 4

        // Checkpoint at 24
        grid[11][24] = 10

        val coins = listOf(
            DynamicCoin(1, 5f, 10f),
            DynamicCoin(2, 7f, 10f),
            DynamicCoin(3, 9f, 9f),
            DynamicCoin(4, 22f, 8f),
            DynamicCoin(5, 27f, 8f),
            DynamicCoin(6, 34f, 6f),
            DynamicCoin(7, 40f, 6f),
            DynamicCoin(8, 42f, 6f)
        )

        val stars = listOf(
            DynamicStar(1, 22f, 7f), // Above question block
            DynamicStar(2, 27f, 6f), // In the air above moving platform
            DynamicStar(3, 49f, 3.5f) // At top of flagpole
        )

        val movingPlatforms = listOf(
            MovingPlatform(1, x = 25f, y = 10f, width = 2f, startX = 25f, startY = 10f, endX = 29f, endY = 10f, speed = 1.8f)
        )

        val crumblingPlatforms = listOf(
            CrumblingPlatform(1, x = 15f, y = 9f, width = 1.5f)
        )

        return LevelData(
            id = 1,
            title = "Green Hill Hop",
            subtitle = "Learn the Basics & Jump!",
            theme = LevelTheme.GRASSLAND,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2.5f,
            startY = 10f,
            coins = coins,
            stars = stars,
            movingPlatforms = movingPlatforms,
            crumblingPlatforms = crumblingPlatforms,
            checkpoints = listOf(Checkpoint(24f, 11f)),
            parTimeSeconds = 25f
        )
    }

    // Level 2: Brick Alley Bounce (Wall jump focus)
    private fun createLevel2(): LevelData {
        val w = 50
        val h = 16
        val grid = createGrid(w, h)

        // Starting floor
        for (x in 0..7) {
            grid[14][x] = 1
            grid[15][x] = 1
        }

        // First Wall Shaft: Wall A at x=8 (height 5..14), Wall B at x=12 (height 5..14)
        for (y in 6..14) grid[y][8] = 2
        for (y in 6..14) grid[y][12] = 2
        // Spikes on ground between walls
        for (x in 9..11) grid[15][x] = 6

        // High rooftop at x=12..18
        for (x in 12..18) grid[6][x] = 2
        for (y in 7..15) grid[y][18] = 2

        // Deep drop after 18 to x=22
        for (x in 19..22) grid[15][x] = 6 // Spikes!

        // Second Wall Shaft: Wall at 22 (height 4..14) and Wall at 26 (height 4..14)
        for (y in 4..14) grid[y][22] = 2
        for (y in 4..14) grid[y][26] = 2
        for (x in 23..25) grid[15][x] = 6

        // Checkpoint rooftop at x=26..32
        for (x in 26..32) grid[4][x] = 2
        grid[3][28] = 10 // Checkpoint flag

        // Tricky Wall slide drop at 32..36
        for (y in 4..14) grid[y][32] = 2
        for (y in 7..14) grid[y][36] = 2
        for (x in 33..35) grid[15][x] = 6

        // Final rooftop 36..48
        for (x in 36..48) {
            grid[7][x] = 2
            for (y in 8..15) grid[y][x] = 4
        }

        // Flagpole at 44, Castle at 46..48
        for (y in 1..6) grid[y][44] = 11
        grid[6][47] = 12 // Door

        val coins = listOf(
            DynamicCoin(1, 4f, 12f),
            DynamicCoin(2, 10f, 10f),
            DynamicCoin(3, 10f, 7f),
            DynamicCoin(4, 15f, 4f),
            DynamicCoin(5, 24f, 10f),
            DynamicCoin(6, 24f, 6f),
            DynamicCoin(7, 34f, 9f),
            DynamicCoin(8, 40f, 5f)
        )

        val stars = listOf(
            DynamicStar(1, 10f, 5f), // Top of first wall jump shaft
            DynamicStar(2, 24f, 3f), // Top of second shaft
            DynamicStar(3, 44f, 1f)  // Peak of flagpole
        )

        return LevelData(
            id = 2,
            title = "Brick Alley Bounce",
            subtitle = "Master the Wall Jump!",
            theme = LevelTheme.BRICK_ALLEY,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2f,
            startY = 12f,
            coins = coins,
            stars = stars,
            movingPlatforms = emptyList(),
            crumblingPlatforms = listOf(
                CrumblingPlatform(1, x = 14f, y = 4f, width = 1.5f),
                CrumblingPlatform(2, x = 38f, y = 5f, width = 1.5f)
            ),
            checkpoints = listOf(Checkpoint(28f, 3f)),
            parTimeSeconds = 30f
        )
    }

    // Level 3: Skyline Springs (High altitudes, springs, crumbling clouds)
    private fun createLevel3(): LevelData {
        val w = 55
        val h = 15
        val grid = createGrid(w, h)

        // Starting cloud
        for (x in 0..6) {
            grid[11][x] = 1
            grid[12][x] = 4
        }

        // Spring 1 at x=5
        grid[10][5] = 8

        // Floating island 1 at x=10..13, y=6
        for (x in 10..13) grid[6][x] = 2

        // Spring 2 at x=13, y=6
        grid[5][13] = 8

        // Floating ledge at x=18..21, y=3
        for (x in 18..21) grid[3][x] = 2

        // Mid-air drop down to island 24..28, y=10
        for (x in 24..28) {
            grid[10][x] = 1
            grid[11][x] = 4
        }
        grid[9][25] = 10 // Checkpoint

        // Spring at 27
        grid[9][27] = 8

        // Floating sequence 32..35, y=7
        for (x in 32..35) grid[7][x] = 2

        // Spring at 35, y=7
        grid[6][35] = 8

        // Castle cloud 44..53, y=8
        for (x in 44..53) {
            grid[8][x] = 1
            for (y in 9..14) grid[y][x] = 4
        }

        // Flagpole at 48, Castle door at 51
        for (y in 2..7) grid[y][48] = 11
        grid[7][51] = 12

        // Hazards at absolute bottom y=14
        for (x in 0 until w) {
            if (grid[14][x] == 0) grid[14][x] = 7 // Fall into bottom abyss
        }

        val coins = listOf(
            DynamicCoin(1, 3f, 9f),
            DynamicCoin(2, 8f, 4f),
            DynamicCoin(3, 11f, 4f),
            DynamicCoin(4, 15f, 2f),
            DynamicCoin(5, 20f, 1.5f),
            DynamicCoin(6, 30f, 6f),
            DynamicCoin(7, 33f, 5f),
            DynamicCoin(8, 40f, 4f)
        )

        val stars = listOf(
            DynamicStar(1, 20f, 1f),
            DynamicStar(2, 39f, 3f),
            DynamicStar(3, 48f, 1.5f)
        )

        val crumblingPlatforms = listOf(
            CrumblingPlatform(1, x = 15f, y = 5f, width = 1.5f),
            CrumblingPlatform(2, x = 22f, y = 7f, width = 1.5f),
            CrumblingPlatform(3, x = 29f, y = 9f, width = 1.5f),
            CrumblingPlatform(4, x = 38f, y = 6f, width = 1.8f),
            CrumblingPlatform(5, x = 41f, y = 7f, width = 1.5f)
        )

        val movingPlatforms = listOf(
            MovingPlatform(1, x = 7f, y = 8f, width = 2f, startX = 7f, startY = 8f, endX = 7f, endY = 5f, speed = 1.5f),
            MovingPlatform(2, x = 36f, y = 5f, width = 2f, startX = 36f, startY = 5f, endX = 40f, endY = 5f, speed = 2.0f)
        )

        return LevelData(
            id = 3,
            title = "Skyline Springs",
            subtitle = "Springs & Crumbling Clouds",
            theme = LevelTheme.SKY_MUSHROOM,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2f,
            startY = 9f,
            coins = coins,
            stars = stars,
            movingPlatforms = movingPlatforms,
            crumblingPlatforms = crumblingPlatforms,
            checkpoints = listOf(Checkpoint(25f, 9f)),
            parTimeSeconds = 35f
        )
    }

    // Level 4: Lava Fortress (Fire pits, moving platforms, wall jump over spikes)
    private fun createLevel4(): LevelData {
        val w = 55
        val h = 15
        val grid = createGrid(w, h)

        // Fill bottom with Lava
        for (x in 0 until w) {
            grid[14][x] = 7
        }

        // Start platform 0..6
        for (x in 0..6) {
            grid[12][x] = 4
            grid[13][x] = 4
        }

        // Stepping stone 9..11
        for (x in 9..11) grid[11][x] = 4

        // Spikes on stone at 10
        grid[10][10] = 6

        // High ledge 14..16
        for (x in 14..16) grid[9][x] = 4

        // Fortress wall jump chimney 19 (x=19, y=4..13) and 23 (x=23, y=4..13)
        for (y in 4..13) grid[y][19] = 2
        for (y in 4..13) grid[y][23] = 2
        grid[13][20] = 7
        grid[13][21] = 7
        grid[13][22] = 7

        // Mid fortress roof 23..29, y=4
        for (x in 23..29) grid[4][x] = 4
        grid[3][26] = 10 // Checkpoint

        // Drop down to moving platforms over giant lava lake 30..42
        // Island at 43..53
        for (x in 43..53) {
            grid[11][x] = 4
            grid[12][x] = 4
            grid[13][x] = 4
        }

        // Final flag at 48, Castle door at 51
        for (y in 4..10) grid[y][48] = 11
        grid[10][51] = 12

        val coins = listOf(
            DynamicCoin(1, 4f, 10f),
            DynamicCoin(2, 9f, 9f),
            DynamicCoin(3, 15f, 7f),
            DynamicCoin(4, 21f, 9f),
            DynamicCoin(5, 21f, 6f),
            DynamicCoin(6, 32f, 8f),
            DynamicCoin(7, 37f, 8f),
            DynamicCoin(8, 45f, 9f)
        )

        val stars = listOf(
            DynamicStar(1, 15f, 6f),
            DynamicStar(2, 21f, 2.5f), // Peak of chimney
            DynamicStar(3, 48f, 3f)
        )

        val movingPlatforms = listOf(
            MovingPlatform(1, x = 6f, y = 11f, width = 2f, startX = 6f, startY = 11f, endX = 9f, endY = 11f, speed = 1.5f),
            MovingPlatform(2, x = 30f, y = 10f, width = 2f, startX = 30f, startY = 10f, endX = 35f, endY = 10f, speed = 2.2f),
            MovingPlatform(3, x = 37f, y = 9f, width = 2f, startX = 37f, startY = 9f, endX = 42f, endY = 9f, speed = 2.0f)
        )

        val crumblingPlatforms = listOf(
            CrumblingPlatform(1, x = 17f, y = 10f, width = 1.5f),
            CrumblingPlatform(2, x = 33f, y = 7f, width = 1.5f)
        )

        return LevelData(
            id = 4,
            title = "Lava Fortress",
            subtitle = "Dodge the Molten Pit",
            theme = LevelTheme.LAVA_CASTLE,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2f,
            startY = 10f,
            coins = coins,
            stars = stars,
            movingPlatforms = movingPlatforms,
            crumblingPlatforms = crumblingPlatforms,
            checkpoints = listOf(Checkpoint(26f, 3f)),
            parTimeSeconds = 38f
        )
    }

    // Level 5: Frozen Peaks (Ice physics, speed slides, icy wall leaps)
    private fun createLevel5(): LevelData {
        val w = 55
        val h = 15
        val grid = createGrid(w, h)

        // Starting icy ledge
        for (x in 0..7) {
            grid[12][x] = 5 // Ice block
            grid[13][x] = 4
        }

        // Ice slope & gap
        for (x in 10..15) {
            grid[12][x] = 5
            grid[13][x] = 4
        }
        grid[11][13] = 6 // Spike on ice

        // Ice pillars for wall leaps: Pillar at 18 (y=6..13), Pillar at 22 (y=6..13)
        for (y in 6..13) grid[y][18] = 5
        for (y in 6..13) grid[y][22] = 5
        for (x in 19..21) grid[14][x] = 6 // Spikes below

        // Checkpoint ledge 22..27, y=6
        for (x in 22..27) grid[6][x] = 5
        grid[5][25] = 10 // Checkpoint

        // Long jump slide with Spring at 27
        grid[5][27] = 8

        // Series of icy steps 32, 36, 40
        for (x in 31..33) grid[8][x] = 5
        for (x in 35..37) grid[7][x] = 5
        for (x in 39..41) grid[6][x] = 5

        // Final peak 44..54
        for (x in 44..54) {
            grid[10][x] = 5
            for (y in 11..14) grid[y][x] = 4
        }

        // Flag at 48, Castle door at 52
        for (y in 3..9) grid[y][48] = 11
        grid[9][52] = 12

        // Abyss at bottom
        for (x in 0 until w) {
            if (grid[14][x] == 0) grid[14][x] = 7
        }

        val coins = listOf(
            DynamicCoin(1, 4f, 10f),
            DynamicCoin(2, 11f, 10f),
            DynamicCoin(3, 20f, 10f),
            DynamicCoin(4, 20f, 7f),
            DynamicCoin(5, 29f, 4f),
            DynamicCoin(6, 32f, 6f),
            DynamicCoin(7, 36f, 5f),
            DynamicCoin(8, 40f, 4f)
        )

        val stars = listOf(
            DynamicStar(1, 20f, 4.5f),
            DynamicStar(2, 36f, 3f),
            DynamicStar(3, 48f, 2f)
        )

        val movingPlatforms = listOf(
            MovingPlatform(1, x = 15f, y = 10f, width = 2f, startX = 15f, startY = 10f, endX = 17f, endY = 10f, speed = 2.0f),
            MovingPlatform(2, x = 28f, y = 8f, width = 2f, startX = 28f, startY = 8f, endX = 30f, endY = 8f, speed = 2.4f)
        )

        val crumblingPlatforms = listOf(
            CrumblingPlatform(1, x = 8f, y = 11f, width = 1.5f),
            CrumblingPlatform(2, x = 42f, y = 8f, width = 1.5f)
        )

        return LevelData(
            id = 5,
            title = "Frozen Peaks",
            subtitle = "Slippery Momentum & Ice Leap",
            theme = LevelTheme.FROZEN_PEAKS,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2f,
            startY = 10f,
            coins = coins,
            stars = stars,
            movingPlatforms = movingPlatforms,
            crumblingPlatforms = crumblingPlatforms,
            checkpoints = listOf(Checkpoint(25f, 5f)),
            parTimeSeconds = 35f
        )
    }

    // Level 6: The Final Gauntlet (The ultimate parkour test!)
    private fun createLevel6(): LevelData {
        val w = 65
        val h = 16
        val grid = createGrid(w, h)

        // Bottom is pure hazard
        for (x in 0 until w) grid[15][x] = 7

        // Start platform
        for (x in 0..5) {
            grid[13][x] = 2
            grid[14][x] = 4
        }

        // Section 1: Crumbling run over spikes
        // Crumbling blocks handled dynamically, spikes at 6..12
        for (x in 6..12) grid[14][x] = 6

        // Section 2: Tall wall jump shaft (x=13, y=4..13) and (x=17, y=4..13)
        for (y in 4..13) grid[y][13] = 2
        for (y in 4..13) grid[y][17] = 2
        for (x in 14..16) grid[14][x] = 6

        // Ledge with Spring at 17, y=4
        grid[3][17] = 8

        // High suspended island with Checkpoint 22..26, y=2
        for (x in 22..26) grid[2][x] = 2
        grid[1][24] = 10 // Checkpoint 1

        // Drop down into moving platform maze 28..42
        // Second Checkpoint at 44, y=10
        for (x in 43..46) {
            grid[10][x] = 2
            grid[11][x] = 4
        }
        grid[9][44] = 10 // Checkpoint 2

        // Section 3: Final Wall Climb & Spikes 48 (y=3..12), 52 (y=3..12)
        for (y in 3..12) grid[y][48] = 2
        for (y in 3..12) grid[y][52] = 2
        for (x in 49..51) grid[14][x] = 6

        // Final Castle rooftop 54..64, y=5
        for (x in 54..64) {
            grid[5][x] = 2
            for (y in 6..14) grid[y][x] = 4
        }

        // Glorious Flagpole at 59, Castle Gate at 62
        for (y in 0..4) grid[y][59] = 11
        grid[4][62] = 12

        val coins = listOf(
            DynamicCoin(1, 3f, 11f),
            DynamicCoin(2, 8f, 10f),
            DynamicCoin(3, 15f, 9f),
            DynamicCoin(4, 15f, 5f),
            DynamicCoin(5, 20f, 1f),
            DynamicCoin(6, 32f, 6f),
            DynamicCoin(7, 38f, 6f),
            DynamicCoin(8, 44f, 8f),
            DynamicCoin(9, 50f, 7f),
            DynamicCoin(10, 50f, 3f)
        )

        val stars = listOf(
            DynamicStar(1, 15f, 2f), // Peak of first wall jump
            DynamicStar(2, 35f, 4f), // Above moving platform hazard
            DynamicStar(3, 59f, 0f)  // Very top of final flagpole
        )

        val movingPlatforms = listOf(
            MovingPlatform(1, x = 28f, y = 7f, width = 2f, startX = 28f, startY = 7f, endX = 33f, endY = 7f, speed = 2.5f),
            MovingPlatform(2, x = 35f, y = 9f, width = 2f, startX = 35f, startY = 9f, endX = 40f, endY = 9f, speed = 2.5f),
            MovingPlatform(3, x = 46f, y = 8f, width = 1.8f, startX = 46f, startY = 8f, endX = 46f, endY = 4f, speed = 1.8f)
        )

        val crumblingPlatforms = listOf(
            CrumblingPlatform(1, x = 6f, y = 12f, width = 1.2f),
            CrumblingPlatform(2, x = 8.5f, y = 12f, width = 1.2f),
            CrumblingPlatform(3, x = 11f, y = 12f, width = 1.2f),
            CrumblingPlatform(4, x = 19f, y = 3f, width = 1.5f),
            CrumblingPlatform(5, x = 53f, y = 4f, width = 1.5f)
        )

        return LevelData(
            id = 6,
            title = "The Final Gauntlet",
            subtitle = "The Ultimate Parkour Trial",
            theme = LevelTheme.MASTER_GAUNTLET,
            mapWidth = w,
            mapHeight = h,
            grid = grid,
            startX = 2f,
            startY = 11f,
            coins = coins,
            stars = stars,
            movingPlatforms = movingPlatforms,
            crumblingPlatforms = crumblingPlatforms,
            checkpoints = listOf(Checkpoint(24f, 1f), Checkpoint(44f, 9f)),
            parTimeSeconds = 45f
        )
    }
}
