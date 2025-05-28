package com.serotonin.common.saveslots




import com.zaxxer.hikari.HikariDataSource
import java.sql.*
import java.util.*

class SaveSlotDAOImpl(private val dataSource: HikariDataSource) : SaveSlotDAO {

    override fun saveSlot(data: PlayerSaveSlot) {
        val existingSlots = getAllSlots(data.uuid)

        val isDuplicate = existingSlots.any {
            it.slot != data.slot &&
                    it.inventoryData.contentEquals(data.inventoryData) &&
                    it.pokemonData.contentEquals(data.pokemonData) &&
                    it.pcData.contentEquals(data.pcData) &&
                    it.backpackData.contentEquals(data.backpackData) &&
                    it.trinketData.contentEquals(data.trinketData)
        }

        val allMeaningless = existingSlots.all { it.isMeaningless() }
        if (data.isMeaningless() && allMeaningless) {
            println("Skipping duplicate check: all slots are empty.")

        } else if (isDuplicate) {
            println("Duplicate save attempt: matches existing slot for ${data.uuid}")
            return
        }


        val sql = """
    INSERT INTO player_save_slots (uuid, slot, inventory_data, pokemon_data, pc_data, last_saved, screenshot_path, backpack_data, trinket_data)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT (uuid, slot)
    DO UPDATE SET inventory_data = EXCLUDED.inventory_data,
                  pokemon_data = EXCLUDED.pokemon_data,
                  pc_data = EXCLUDED.pc_data,
                  last_saved = EXCLUDED.last_saved,
                  screenshot_path = EXCLUDED.screenshot_path,
                  backpack_data = EXCLUDED.backpack_data,
                  trinket_data = EXCLUDED.trinket_data;
""".trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, data.uuid)
                stmt.setInt(2, data.slot)
                stmt.setBytes(3, data.inventoryData)
                stmt.setBytes(4, data.pokemonData)
                stmt.setBytes(5, data.pcData)
                stmt.setLong(6, data.lastSaved)
                stmt.setString(7, data.screenshotPath)
                stmt.setBytes(8, data.backpackData)
                stmt.setBytes(9, data.trinketData)

                println("Saving slot ${data.slot} for ${data.uuid}")
                println("    Inventory size: ${data.inventoryData.size} bytes")
                println("    Party size:     ${data.pokemonData.size} bytes")
                println("    PC size:        ${data.pcData.size} bytes")

                stmt.executeUpdate()
                println("Successfully saved slot ${data.slot} for ${data.uuid}")
            }
        }
    }

    override fun loadSlot(uuid: UUID, slot: Int): PlayerSaveSlot? {
        val sql = "SELECT * FROM player_save_slots WHERE uuid = ? AND slot = ?"

        fun tryLoad(): PlayerSaveSlot? {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, uuid)
                    stmt.setInt(2, slot)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            println("Found slot $slot for $uuid in database")

                            val inventory = rs.getBytes("inventory_data")
                            val party = rs.getBytes("pokemon_data")
                            val pc = rs.getBytes("pc_data")

                            println("    Inventory size: ${inventory.size} bytes")
                            println("    Party size:     ${party.size} bytes")
                            println("    PC size:        ${pc.size} bytes")

                            return PlayerSaveSlot(
                                uuid = rs.getObject("uuid", UUID::class.java),
                                slot = rs.getInt("slot"),
                                inventoryData = inventory,
                                pokemonData = party,
                                pcData = pc,
                                lastSaved = rs.getLong("last_saved"),
                                screenshotPath = rs.getString("screenshot_path"),
                                backpackData = rs.getBytes("backpack_data") ?: ByteArray(0),
                                trinketData = rs.getBytes("trinket_data") ?: ByteArray(0)
                            )
                        }
                    }
                }
            }
            return null
        }

        return tryLoad() ?: run {
            println("Retrying load for slot $slot due to initial miss")
            Thread.sleep(100)
            tryLoad().also {
                if (it == null) println("Still no save data found for $uuid in slot $slot after retry")
            }
        }
    }

    override fun deleteSlot(uuid: UUID, slot: Int) {
        val sql = "DELETE FROM player_save_slots WHERE uuid = ? AND slot = ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, uuid)
                stmt.setInt(2, slot)
                stmt.executeUpdate()
            }
        }
    }

    override fun getAllSlots(uuid: UUID): List<PlayerSaveSlot> {
        val sql = "SELECT * FROM player_save_slots WHERE uuid = ? ORDER BY slot"

        val result = mutableListOf<PlayerSaveSlot>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, uuid)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(
                            PlayerSaveSlot(
                                uuid = rs.getObject("uuid", UUID::class.java),
                                slot = rs.getInt("slot"),
                                inventoryData = rs.getBytes("inventory_data"),
                                pokemonData = rs.getBytes("pokemon_data"),
                                pcData = rs.getBytes("pc_data"),
                                lastSaved = rs.getLong("last_saved"),
                                screenshotPath = rs.getString("screenshot_path"),
                                backpackData = rs.getBytes("backpack_data") ?: ByteArray(0),
                                trinketData = rs.getBytes("trinket_data") ?: ByteArray(0)
                            )
                        )
                    }
                }
            }
        }

        return result
    }


    fun setActiveSlot(uuid: UUID, slot: Int) {
        val sql = """
        INSERT INTO player_active_slot (uuid, slot)
        VALUES (?, ?)
        ON CONFLICT (uuid)
        DO UPDATE SET slot = EXCLUDED.slot;
    """.trimIndent()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, uuid)
                stmt.setInt(2, slot)
                stmt.executeUpdate()
            }
        }
    }

    fun loadActiveSlot(uuid: UUID): Int? {
        val sql = "SELECT slot FROM player_active_slot WHERE uuid = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, uuid)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("slot")
                }
            }
        }
        return null
    }


}