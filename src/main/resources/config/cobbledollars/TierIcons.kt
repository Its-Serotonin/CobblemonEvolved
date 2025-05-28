package config.cobbledollars


import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.api.events.getMinimumEloForTier
import com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen
import com.serotonin.common.client.gui.drawCrispTexture
import com.serotonin.common.client.gui.effects.GuiParticleManager
import com.serotonin.common.elosystem.allTierRewards
import com.serotonin.common.elosystem.getTierName
import com.serotonin.common.elosystem.hasClaimedTier
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
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.item.tooltip.TooltipType
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.mapIndexed
import kotlin.collections.maxOf
import kotlin.let
import kotlin.ranges.coerceAtMost
import kotlin.ranges.until
import kotlin.text.replace
import kotlin.text.replaceFirstChar
import kotlin.text.uppercaseChar
import kotlin.to

class TierIcons(
    x: Int,
    y: Int
) : net.minecraft.client.gui.widget.ClickableWidget(x, y, 10, (10 + 3) * tierIcons.size - 3, net.minecraft.text.Text.empty()) {
    companion object {
        private val BASE_TEXTURE = net.minecraft.util.Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/")

        val tierIcons = com.serotonin.common.elosystem.allTierRewards.map { (tierId, reward) ->
            tierId to reward
        }
    }

    private var hoveredTierId: String? = null

    override fun renderWidget(context: net.minecraft.client.gui.DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val player = client.player ?: return
        val textRenderer = client.textRenderer

        hoveredTierId = null

        var tooltipLinesToRender: List<net.minecraft.text.Text>? = null
        var tooltipBoxX = 0
        var tooltipBoxY = 0
        var tooltipBoxWidth = 0
        var tooltipBoxHeight = 0
        var claimedTooltipItems: List<kotlin.Pair<net.minecraft.item.ItemStack, Int>> = kotlin.collections.emptyList()

        val tooltipContext = net.minecraft.item.Item.TooltipContext.create(client.world)
        val tooltipType = if (client.options.advancedItemTooltips) net.minecraft.item.tooltip.TooltipType.ADVANCED else net.minecraft.item.tooltip.TooltipType.BASIC

        // Step 1: Detect hover, prepare tooltip render data
        var tooltipYAnchor = net.minecraft.client.gui.widget.ClickableWidget.getY
        tierIcons.forEach { (tierId, tierReward) ->
            if (mouseX in net.minecraft.client.gui.widget.ClickableWidget.getX until (net.minecraft.client.gui.widget.ClickableWidget.getX + 10) && mouseY in tooltipYAnchor until (tooltipYAnchor + 10)) {
                val claimed = com.serotonin.common.elosystem.hasClaimedTier(player.uuid, tierId)
                val baseName = com.serotonin.common.elosystem.getTierName(
                    com.serotonin.common.api.events.getMinimumEloForTier(tierId)
                )
                val rewards = tierReward.getRewardItems()

                hoveredTierId = tierId
                tooltipBoxX = mouseX
                tooltipBoxY = mouseY
                tooltipLinesToRender = if (claimed) {
                    kotlin.collections.listOf(
                        net.minecraft.text.Text.literal(baseName),
                        net.minecraft.text.Text.literal("\u00a7aRewards already claimed!")
                    )
                } else {
                    kotlin.collections.listOf(
                        net.minecraft.text.Text.literal(baseName),
                        net.minecraft.text.Text.literal("\u00a7aRewards:")
                    )
                }
                claimedTooltipItems = if (claimed) kotlin.collections.emptyList() else rewards.mapIndexed { i, stack -> stack to i }

                val textHeight = tooltipLinesToRender!!.size * 12
                val iconHeight = ((claimedTooltipItems.size + 9) / 10) * 18
                tooltipBoxWidth = kotlin.comparisons.maxOf(
                    tooltipLinesToRender!!.maxOf { textRenderer.getWidth(it) },
                    claimedTooltipItems.size.coerceAtMost(10) * 18
                ) + 10
                tooltipBoxHeight = textHeight + iconHeight + 10
            }
            tooltipYAnchor += 13
        }

        // Step 2: Draw tooltip BELOW tier icons
        tooltipLinesToRender?.let { lines ->
            context.fill(
                tooltipBoxX - 4, tooltipBoxY - 4,
                tooltipBoxX + tooltipBoxWidth + 4, tooltipBoxY + tooltipBoxHeight + 4,
                Long.toInt()
            )

            lines.forEachIndexed { i, line ->
                context.drawText(textRenderer, line, tooltipBoxX, tooltipBoxY + i * 12, -1, true)
            }

            val textHeight = lines.size * 12
            val itemStartY = tooltipBoxY + textHeight + 2

            claimedTooltipItems.forEach { (stack, index) ->
                val row = index / 10
                val col = index % 10
                val iconX = tooltipBoxX + col * 18
                val iconY = itemStartY + row * 18
                context.drawItem(stack, iconX, iconY)
                context.drawItemInSlot(textRenderer, stack, iconX, iconY)

                if (mouseX in iconX..(iconX + 16) && mouseY in iconY..(iconY + 16)) {
                    val itemTooltip = stack.getTooltip(tooltipContext, client.player, tooltipType)
                    context.drawTooltip(textRenderer, itemTooltip, mouseX, mouseY)
                }
            }
        }

        // Step 3: Draw tier icons AFTER tooltip so they appear above
        var currentY = net.minecraft.client.gui.widget.ClickableWidget.getY
        tierIcons.forEach { (_, reward) ->
            com.serotonin.common.client.gui.drawCrispTexture(
                context,
                reward.iconTexture,
                net.minecraft.client.gui.widget.ClickableWidget.getX,
                currentY,
                10,
                10
            )
            currentY += 13
        }

        hoveredTierId = null
        com.serotonin.common.client.gui.effects.GuiParticleManager.render(context)
        com.serotonin.common.client.gui.effects.GuiParticleManager.tick()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val player = client.player ?: return false
        val uuid = player.uuid
        val hoveredId = hoveredTierId ?: return false

        val currentElo = (client.currentScreen as? com.serotonin.common.client.gui.competitivehandbook.CustomBookScreen)?.currentElo ?: 1000
        val requiredElo = com.serotonin.common.api.events.getMinimumEloForTier(hoveredId)

        return when {
            com.serotonin.common.elosystem.hasClaimedTier(uuid, hoveredId) -> {
                client.soundManager.play(net.minecraft.client.sound.PositionedSoundInstance.master(com.serotonin.common.registries.SoundRegistry.REWARD_ALREADY_CLAIMED, 1.0f, 0.7f))
                player.sendMessage(
                    net.minecraft.text.Text.literal("you already claimed ${prettyTierName(hoveredId)} rewards!").formatted(
                        net.minecraft.util.Formatting.RED))
                true
            }

            currentElo < requiredElo -> {
                client.soundManager.play(net.minecraft.client.sound.PositionedSoundInstance.master(com.serotonin.common.registries.SoundRegistry.REWARD_ALREADY_CLAIMED, 1.0f, 0.7f))
                player.sendMessage(
                    net.minecraft.text.Text.literal("You need a higher rank to claim ${prettyTierName(hoveredId)} rewards.").formatted(
                        net.minecraft.util.Formatting.RED))
                true
            }

            else -> {
                val reward = com.serotonin.common.elosystem.allTierRewards[hoveredId] ?: return false
                val rewards = reward.getRewardItems()
                rewards.forEach { player.inventory.insertStack(it.copy()) }

                val payload = kotlinx.serialization.json.buildJsonObject {
                    put("type", "claim_tier_reward")
                    put("uuid", uuid.toString())
                    put("tier", hoveredId)
                }.toString()

                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    com.serotonin.common.networking.RawJsonPayload(
                        payload
                    )
                )
                client.soundManager.play(net.minecraft.client.sound.PositionedSoundInstance.master(com.serotonin.common.registries.SoundRegistry.REWARD_CLAIMED, 1.0f, 0.7f))
                com.serotonin.common.client.gui.effects.GuiParticleManager.spawnConfetti(mouseX.toInt(), mouseY.toInt())
                true
            }
        }
    }

    private fun isMouseOver(mouseX: Int, mouseY: Int, iconY: Int): Boolean {
        return mouseX in net.minecraft.client.gui.widget.ClickableWidget.getX until (net.minecraft.client.gui.widget.ClickableWidget.getX + 10) && mouseY in iconY until (iconY + 10)
    }

    override fun appendClickableNarrations(builder: net.minecraft.client.gui.screen.narration.NarrationMessageBuilder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, net.minecraft.text.Text.literal("Tier reward icons"))
    }

    private fun prettyTierName(tierId: String): String {
        return tierId.replace('_', ' ').replaceFirstChar { it.uppercaseChar() } + " Tier"
    }
}

