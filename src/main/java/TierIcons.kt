import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.api.events.getMinimumEloForTier
import com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen
import com.serotonin.common.client.gui.drawCrispTexture
import com.serotonin.common.client.gui.effects.GuiParticleManager
import com.serotonin.common.elosystem.allTierRewards
import com.serotonin.common.elosystem.claimedTiers
import com.serotonin.common.elosystem.getTierName
import com.serotonin.common.elosystem.hasClaimedTier
import com.serotonin.common.elosystem.saveClaimedTier
import com.serotonin.common.networking.RawJsonPayload
import com.serotonin.common.registries.SoundRegistry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity

class TierIcons(
    x: Int,
    y: Int
) : ClickableWidget(x, y, 10, (10 + 3) * tierIcons.size - 3, Text.empty()) {

    companion object {
        private val BASE_TEXTURE = Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/")

        val tierIcons = allTierRewards.map { (tierId, reward) ->
            tierId to reward
        }
    }

    private var hoveredTierId: String? = null

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val itemRenderer = client.itemRenderer
        val textRenderer = client.textRenderer
        var currentY = y

        hoveredTierId = null

        tierIcons.forEach { (tierId, tierReward) ->
            val texture = tierReward.iconTexture
            val claimed = hasClaimedTier(player.uuid, tierId)
            val baseName = getTierName(getMinimumEloForTier(tierId))
            val rewards = tierReward.getRewardItems()


            drawCrispTexture(context, texture, x, currentY, 10, 10)

            if (isMouseOver(mouseX, mouseY, currentY)) {
                hoveredTierId = tierId

                if (!claimed) {
                    RenderSystem.enableDepthTest()
                    RenderSystem.enableBlend()
                    RenderSystem.defaultBlendFunc()

                    rewards.forEachIndexed { i, item ->
                        val iconX = mouseX + 5 + (i * 18)
                        val iconY = mouseY + 15
                        context.drawItem(item, iconX, iconY)
                        context.drawItemInSlot(textRenderer, item, iconX, iconY)
                    }
                    RenderSystem.disableDepthTest()
                }

                val tooltipLines = if (claimed) {
                    listOf(Text.literal(baseName), Text.literal("§aRewards already claimed!"))
                } else {
                    listOf(Text.literal(baseName), Text.literal("§aRewards:")) +
                            rewards.map {
                                val rarityColor = when (it.rarity) {
                                    Rarity.UNCOMMON -> "§a"
                                    Rarity.RARE -> "§9"
                                    Rarity.EPIC -> "§d"
                                    Rarity.COMMON, null -> "§f"
                                }
                                Text.literal(" - $rarityColor${it.name.string} x${it.count}")
                            }
                }

                context.drawTooltip(textRenderer, tooltipLines, mouseX, mouseY)
            }

            currentY += 13 // 10px icon + 3px spacing
        }

        GuiParticleManager.render(context)
        GuiParticleManager.tick()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false
        val uuid = player.uuid
        val hoveredId = hoveredTierId ?: return false

        val currentElo = (client.currentScreen as? CustomBookScreen)?.currentElo ?: 1000
        val requiredElo = getMinimumEloForTier(hoveredId)

        println("🧪 Debug Elo Check: current=$currentElo required=$requiredElo")

        return when {
            hasClaimedTier(uuid, hoveredId) -> {
                client.soundManager.play(PositionedSoundInstance.master(SoundRegistry.REWARD_ALREADY_CLAIMED, 1.0f, 0.7f))
                player.sendMessage(Text.literal("you already claimed ${prettyTierName(hoveredId)} rewards!").formatted(Formatting.RED))
                true
            }

            currentElo < requiredElo -> {
                client.soundManager.play(PositionedSoundInstance.master(SoundRegistry.REWARD_ALREADY_CLAIMED, 1.0f, 0.7f))
                player.sendMessage(Text.literal("You need a higher rank to claim ${prettyTierName(hoveredId)} rewards.").formatted(Formatting.RED))
                true
            }

            else -> {
                val reward = allTierRewards[hoveredId] ?: return false
                val rewards = reward.getRewardItems()

                rewards.forEach { player.inventory.insertStack(it.copy()) }


                val payload = buildJsonObject {
                    put("type", "claim_tier_reward")
                    put("uuid", uuid.toString())
                    put("tier", hoveredId)
                }.toString()

                ClientPlayNetworking.send(RawJsonPayload(payload))

                client.soundManager.play(PositionedSoundInstance.master(SoundRegistry.REWARD_CLAIMED, 1.0f, 0.7f))

                GuiParticleManager.spawnConfetti(mouseX.toInt(), mouseY.toInt())

                true
            }
        }
    }

    private fun isMouseOver(mouseX: Int, mouseY: Int, iconY: Int): Boolean {
        return mouseX in x until (x + 10) && mouseY in iconY until (iconY + 10)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, Text.literal("Tier reward icons"))
    }

    private fun prettyTierName(tierId: String): String {
        return tierId.replace('_', ' ').replaceFirstChar { it.uppercaseChar() } + " Tier"
    }
}