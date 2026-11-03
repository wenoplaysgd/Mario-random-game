package com.example

import com.example.levels.LevelRepository
import com.example.model.LevelTheme
import com.example.model.TileType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `level repository contains 6 levels`() {
        assertEquals(6, LevelRepository.totalLevels)
    }

    @Test
    fun `verify all levels have valid geometry and objectives`() {
        val allLevels = LevelRepository.getAllLevelInfo()
        assertEquals(6, allLevels.size)

        for (info in allLevels) {
            val level = LevelRepository.getLevel(info.id)
            assertNotNull(level)
            assertEquals(info.id, level.id)
            assertTrue("Map width should be >= 40", level.mapWidth >= 40)
            assertTrue("Map height should be >= 12", level.mapHeight >= 12)
            assertTrue("Level must have coins", level.coins.isNotEmpty())
            assertTrue("Level must have 3 stars", level.stars.size == 3)
            assertTrue("Level start position must be valid", level.startX > 0 && level.startY > 0)
        }
    }

    @Test
    fun `verify tile type mappings and solids`() {
        assertTrue(TileType.GROUND.isSolid)
        assertTrue(TileType.BRICK.isSolid)
        assertTrue(TileType.HARD_BLOCK.isSolid)
        assertTrue(TileType.SPRING.isSolid)
        assertFalse(TileType.EMPTY.isSolid)
        assertTrue(TileType.SPIKE.isHazard)
        assertFalse(TileType.fromId(0).isHazard)
        assertFalse(TileType.EMPTY.isHazard)
    }

    @Test
    fun `verify level theme colors are configured`() {
        for (theme in LevelTheme.entries) {
            assertNotNull(theme.displayName)
            assertNotNull(theme.brickColor)
            assertNotNull(theme.skyColorTop)
        }
    }
}
