package com.serotonin.common.saveslots

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.*

object SaveSlotBackupManager {
    private val backupRoot = File("shared/saveslot_backups")
    private const val CURRENT_VERSION = 1

    data class BackupSlotData(
        val inventory: ByteArray,
        val party: ByteArray,
        val pc: ByteArray,
        val backpack: ByteArray = ByteArray(0),  // ← must be defined
        val trinkets: ByteArray = ByteArray(0),  // ← must be defined
        val timestamp: Long
    ) {
        override fun equals(other: Any?): Boolean {
            return other is BackupSlotData &&
                    inventory.contentEquals(other.inventory) &&
                    party.contentEquals(other.party) &&
                    pc.contentEquals(other.pc)
        }

        override fun hashCode(): Int {
            var result = inventory.contentHashCode()
            result = 31 * result + party.contentHashCode()
            result = 31 * result + pc.contentHashCode()
            return result
        }
    }

    fun backupSlot(data: PlayerSaveSlot) {
        try {
            val playerDir = File(backupRoot, data.uuid.toString())
            if (!playerDir.exists()) playerDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val filename = "slot${data.slot}_$timestamp.dat"
            val file = File(playerDir, filename)

            FileOutputStream(file).use { out ->
                out.write(encodeBackup(data))
            }

            println("Created backup for slot ${data.slot} of ${data.uuid}: ${file.absolutePath}")
            cleanupOldBackups(data.uuid, data.slot)
        } catch (e: Exception) {
            println("Failed to create backup for ${data.uuid}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun loadLatestBackup(uuid: UUID, slot: Int): BackupSlotData? {
        val playerDir = File(backupRoot, uuid.toString())
        if (!playerDir.exists()) return null

        val files = playerDir.listFiles { f ->
            f.name.startsWith("slot$slot") && f.name.endsWith(".dat")
        } ?: return null

        val latest = files.mapNotNull { file ->
            try {
                val data = decodeBackup(file.readBytes())
                data to file
            } catch (e: Exception) {
                println("Failed to decode backup file ${file.name}: ${e.message}")
                null
            }
        }.maxByOrNull { it.first.timestamp }

        if (latest == null) return null

        val (data, file) = latest

        return try {
            FileInputStream(file).use {
                decodeBackup(it.readBytes())
            }
        } catch (e: Exception) {
            println("Failed to load backup from ${file.name}: ${e.message}. Deleting it.")
            file.delete()
            null
        }
    }

    private fun encodeBackup(data: PlayerSaveSlot): ByteArray {
        val output = ByteArrayOutputStream()
        val tag = NbtCompound().apply {
            putInt("version", CURRENT_VERSION)
            putByteArray("inventory", data.inventoryData)
            putByteArray("party", data.pokemonData)
            putByteArray("pc", data.pcData)
            putByteArray("backpack", data.backpackData)
            putByteArray("trinkets", data.trinketData)
            putLong("timestamp", data.lastSaved)
        }
        NbtIo.writeCompressed(tag, output)
        return output.toByteArray()
    }

    private fun decodeBackup(bytes: ByteArray): BackupSlotData {
        val tag = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtSizeTracker.ofUnlimitedBytes())
        val version = tag.getInt("version")
        if (version != CURRENT_VERSION) {
            println("Loading backup with version $version (current: $CURRENT_VERSION)")
            // In future: support different decode logic
        }
        return BackupSlotData(
            inventory = tag.getSafeByteArray("inventory"),
            party = tag.getSafeByteArray("party"),
            pc = tag.getSafeByteArray("pc"),
            backpack = tag.getSafeByteArray("backpack"),
            trinkets = tag.getSafeByteArray("trinkets"),
            timestamp = if (tag.contains("timestamp")) tag.getLong("timestamp") else System.currentTimeMillis()
        )
    }
    fun cleanupOldBackups(uuid: UUID, slot: Int, maxBackups: Int = 5): Int {
        val playerDir = File(backupRoot, uuid.toString())
        if (!playerDir.exists()) return 0

        val backups = playerDir.listFiles { f ->
            f.name.startsWith("slot$slot") && f.name.endsWith(".dat")
        }?.sortedByDescending { it.lastModified() } ?: return 0

        if (backups.size <= maxBackups) return 0

        val toDelete = backups.drop(maxBackups)
        toDelete.forEach {
            println("Found ${backups.size} backups for slot $slot of $uuid")
            it.delete()
        }

        return toDelete.size
    }

    fun cleanupInactivePlayers(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
        if (!backupRoot.exists()) return

        backupRoot.listFiles()?.forEach { playerDir ->
            if (!playerDir.isDirectory) return@forEach

            val recent = playerDir.listFiles()?.any { it.lastModified() > cutoff } ?: false
            if (!recent) {
                println("Deleting old backup folder for inactive player: ${playerDir.name}")
                playerDir.deleteRecursively()
            }
        }
    }

    fun deleteAllBackups(uuid: UUID, slot: Int) {
        val dir = File(backupRoot, uuid.toString())
        if (dir.exists()) {
            dir.listFiles()?.filter { it.name.startsWith("slot$slot") }?.forEach {
                println("Deleting backup: ${it.name}")
                it.delete()
            }
        }
    }


}

fun NbtCompound.getSafeByteArray(key: String): ByteArray {
    return (this[key] as? net.minecraft.nbt.NbtByteArray)?.byteArray ?: ByteArray(0)
}

