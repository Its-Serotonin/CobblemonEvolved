package com.serotonin.common.registries

import com.serotonin.Cobblemonevolved.MOD_ID
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import java.nio.file.Files
import java.nio.file.Path

import java.nio.file.Paths

object CustomVendorRegistry {
    private val configDir = Paths.get("config/cobbledollars")
    private val vendorFile = configDir.resolve("lobby_vendor.json")
    private val bankFile = configDir.resolve("bank.json")

    fun register() {
        extractIfMissing("lobby_vendor.json", vendorFile)
        extractIfMissing("bank.json", bankFile)
    }

    private fun extractIfMissing(name: String, target: Path) {
        if (!Files.exists(target)) {
            Files.createDirectories(target.parent)
            val input = javaClass.classLoader.getResourceAsStream("config/cobbledollars/$name")
            if (input != null) {
                Files.copy(input, target)
                println("Extracted $name to ${target.toAbsolutePath()}")
            } else {
                println("Failed to extract $name — not found in resources!")
            }
        }
    }
}

object LobbyVendorDamagePrevention{
    fun preventDamage(){

        AttackEntityCallback.EVENT.register(AttackEntityCallback { player, world, hand, target, hitResult ->
            if (!world.isClient && target.commandTags.contains("lobby_vendor")) {
                //player.sendMessage(Text.literal("§cYou can't hurt the lobby vendor!"), true)
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        })
/*
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            if (!entity.world.isClient && entity.commandTags.contains("lobby_vendor")) {
                false
            } else true
        }
*/


    }
}