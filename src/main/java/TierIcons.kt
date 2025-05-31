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
        var rewardBoxHovered = false

        var tooltipLinesToRender: List<Text>? = null
        var tooltipBoxX = 0
        var tooltipBoxY = 0
        var tooltipBoxWidth = 0
        var tooltipBoxHeight = 0
        var claimedTooltipItems: List<Pair<ItemStack, Int>> = emptyList()

        val tooltipContext = Item.TooltipContext.create(client.world)
        val tooltipType = if (client.options.advancedItemTooltips) TooltipType.ADVANCED else TooltipType.BASIC

        tierIcons.forEach { (tierId, tierReward) ->
            val texture = tierReward.iconTexture
            val claimed = hasClaimedTier(player.uuid, tierId)
            val baseName = getTierName(getMinimumEloForTier(tierId))
            val rewards = tierReward.getRewardItems()

            drawCrispTexture(context, texture, x, currentY, 10, 10)

            val itemsPerRow = 7
            val iconSpacing = 18
            val textLines = if (claimed) {
                listOf(Text.literal(baseName), Text.literal("§aRewards already claimed!"))
            } else {
                listOf(Text.literal(baseName), Text.literal("§aRewards:"))
            }

            val textHeight = textLines.size * 12
            val iconHeight = if (claimed) 0 else ((rewards.size + itemsPerRow - 1) / itemsPerRow) * iconSpacing
            val tooltipWidth = if (claimed) {
                textLines.maxOf { textRenderer.getWidth(it) } + 8
            } else {
                maxOf(
                    textLines.maxOf { textRenderer.getWidth(it) },
                    rewards.size.coerceAtMost(itemsPerRow) * iconSpacing
                ) + 7
            }
            val tooltipHeight = textHeight + iconHeight + if (claimed) 4 else 7

            val tooltipX = mouseX + 12
            val tooltipY = mouseY

            val isInsideIcon = isMouseOver(mouseX, mouseY, currentY)
            val isInsideTooltip =
                hoveredTierId == tierId &&
                        mouseX in tooltipX..(tooltipX + tooltipWidth) &&
                        mouseY in tooltipY..(tooltipY + tooltipHeight)

            if (isInsideIcon || isInsideTooltip) {
                hoveredTierId = tierId
                rewardBoxHovered = true

                tooltipLinesToRender = textLines
                tooltipBoxX = tooltipX
                tooltipBoxY = tooltipY
                tooltipBoxWidth = tooltipWidth
                tooltipBoxHeight = tooltipHeight

                if (!claimed) {
                    claimedTooltipItems = rewards.mapIndexed { i, stack -> stack to i }
                }
            }

            currentY += 13
        }


        tooltipLinesToRender?.let { lines ->
            context.fill(
                tooltipBoxX - 4, tooltipBoxY - 4,
                tooltipBoxX + tooltipBoxWidth + 4, tooltipBoxY + tooltipBoxHeight + 4,
                0xF0100010.toInt()
            )

            lines.forEachIndexed { i, line ->
                context.drawText(textRenderer, line, tooltipBoxX, tooltipBoxY + i * 12, -1, true)
            }

            val textHeight = lines.size * 12
            val itemStartY = tooltipBoxY + textHeight + 2
            val iconSpacing = 18
            val itemsPerRow = 7

            claimedTooltipItems.forEach { (stack, index) ->
                val row = index / itemsPerRow
                val col = index % itemsPerRow
                val iconX = tooltipBoxX + col * iconSpacing
                val iconY = itemStartY + row * iconSpacing
                context.drawItem(stack, iconX, iconY)
                context.drawItemInSlot(textRenderer, stack, iconX, iconY)

                if (mouseX in iconX..(iconX + 16) && mouseY in iconY..(iconY + 16)) {
                    val itemTooltip = stack.getTooltip(tooltipContext, client.player, tooltipType)
                    context.drawTooltip(textRenderer, itemTooltip, mouseX, mouseY)
                }
            }
        }

        if (!rewardBoxHovered) {
            hoveredTierId = null
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

        println("Debug Elo Check: current=$currentElo required=$requiredElo")

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