package com.serotonin.common.client.gui.competitivehandbook

import com.mojang.blaze3d.systems.RenderSystem
import com.serotonin.common.api.events.EloManager
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_HEIGHT
import com.serotonin.common.client.gui.competitivehandbook.CompetitiveHandbookGUIConstants.BASE_WIDTH
import com.serotonin.common.client.gui.competitivehandbook.widgets.Chains
import com.serotonin.common.client.gui.competitivehandbook.widgets.CompetitiveHandbookTextGUI
import com.serotonin.common.client.gui.competitivehandbook.widgets.FriendlyBattleButtons
import com.serotonin.common.client.gui.competitivehandbook.widgets.FriendlyBattleStatusTextGUI
import com.serotonin.common.client.gui.competitivehandbook.widgets.TierIcons
import com.serotonin.common.client.gui.competitivehandbook.widgets.TierIcons.Companion.tierIcons
import com.serotonin.common.client.gui.competitivehandbook.widgets.TournamentInfoAnimationWidget
import com.serotonin.common.client.gui.competitivehandbook.widgets.TournamentInfoTextGUI
import com.serotonin.common.client.gui.competitivehandbook.widgets.TournamentSignupButton
import com.serotonin.common.client.gui.drawCrispTexture
import com.serotonin.common.client.gui.effects.GuiParticleManager
import com.serotonin.common.elosystem.getTierName
import com.serotonin.common.elosystem.initializeTierRewards
import com.serotonin.common.networking.ClientEloStorage
import com.serotonin.common.networking.RawJsonPayload
import com.serotonin.common.networking.TournamentSignupPayload
import com.serotonin.common.networking.setFriendlyBattle

import com.serotonin.common.registries.FriendlyBattleManager
import com.serotonin.common.registries.SoundRegistry.COMPETITIVE_GUI_CLOSE
import com.serotonin.common.registries.SoundRegistry.COMPETITIVE_GUI_OPEN
import com.serotonin.common.tourneys.TournamentManagerClient
import com.serotonin.common.tourneys.TournamentManagerClient.clearSignupCache
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11
import java.util.UUID
import kotlin.concurrent.thread


class CustomBookScreen : Screen(Text.of("Tier Rewards & Tournaments")) {

    companion object {
        private val screenBackground =
            Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/competitivehandbook_base.png")

        private val backgroundMask =
            Identifier.of("cobblemonevolved", "textures/gui/competitivehandbook/background_mask.png")
    }

    private var isFriendly: Boolean = false
    var currentElo: Int = 1000

    override fun shouldPause(): Boolean = false


    override fun init() {
        initializeTierRewards()
        super.init()

        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        client.soundManager.play(
            PositionedSoundInstance.master(
                COMPETITIVE_GUI_OPEN,
                1.0f,
                1.0f
            )
        )

        val json = buildJsonObject {
            put("type", "get_tournament_signup_status")
        }.toString()

        ClientPlayNetworking.send(RawJsonPayload(json))


        isFriendly = FriendlyBattleManager.getCachedSetting(player.uuid)

         currentElo = EloManager.playerElos[player.uuid] ?: 1000

        val centerX = (width - BASE_WIDTH) / 2
        val centerY = (height - BASE_HEIGHT) / 2

        val anchorX = (width - BASE_WIDTH) / 2 + 20
        val anchorY = (height - BASE_HEIGHT) / 2 + 30

        val greenButton = FriendlyBattleButtons(
            x = centerX + 5,
            y = centerY + 32,
            width = 8,
            height = 8,
            isGreen = true,
            getCurrentState = { FriendlyBattleManager.getCachedSetting(player.uuid) },
            onToggle = { newState ->

                val jsonObject = buildJsonObject {
                    put("type", "toggle_friendly_battle")
                    put("value", newState)
                    put("silent", true)
                }

                ClientPlayNetworking.send(RawJsonPayload(jsonObject.toString()))
            }

        )

        val redButton = FriendlyBattleButtons(
            x = centerX + 16,
            y = centerY + 32,
            width = 8,
            height = 8,
            isGreen = false,
            getCurrentState = {


                FriendlyBattleManager.getCachedSetting(player.uuid) },
            onToggle = { newState ->

                val jsonObject = buildJsonObject {
                    put("type", "toggle_friendly_battle")
                    put("value", newState)
                    put("silent", true)
                }

                ClientPlayNetworking.send(RawJsonPayload(jsonObject.toString()))
            }

        )
        val animation = TournamentInfoAnimationWidget(centerX + 261, centerY + 44)

        val tierIconsWidget = TierIcons(centerX + 91, centerY + 40)

        val backgroundWidget = BackgroundTexture(
            texture = screenBackground,
            x = (width - BASE_WIDTH) / 2,
            y = (height - BASE_HEIGHT) / 2,
            width = BASE_WIDTH,
            height = BASE_HEIGHT
        )





        val signupButton = TournamentSignupButton(
            x = centerX + 265,
            y = centerY + 26,
            width = 80,
            height = 20,
            getCurrentState = { TournamentManagerClient.isSignedUpCached() },
            onToggle = { newState ->
                ClientPlayNetworking.send(TournamentSignupPayload(newState))


                thread(start = true) {
                    Thread.sleep(250)
                    val refreshJson = buildJsonObject {
                        put("type", "get_tournament_signup_status")
                    }.toString()
                    ClientPlayNetworking.send(RawJsonPayload(refreshJson))
                }
            },
            isTournamentActive = { TournamentManagerClient.hasCachedTournament() }
        )

        addDrawable(backgroundWidget)
        addDrawableChild(greenButton)
        addDrawableChild(redButton)
        addDrawable(animation)
        addDrawable(Chains(centerX + 272, centerY + 28, 67, 19))
        addDrawableChild(signupButton)
        addDrawableChild(tierIconsWidget)

        requestEloUpdate()
        requestStatsUpdate()

    }


    private fun getPlayerElo(uuid: UUID): Int {

        return ClientEloStorage.getElo(uuid.toString()) ?: 1000
    }

    private fun getTierForPlayer(elo: Int): String {

        return getTierName(elo)
    }


    private fun requestEloUpdate() {
        val jsonObject = buildJsonObject {
            put("type", "get_elo")
        }


        ClientPlayNetworking.send(RawJsonPayload(jsonObject.toString()))
    }


    private fun requestStatsUpdate() {
        val jsonObject = buildJsonObject {
            put("type", "get_player_stats")
        }
        ClientPlayNetworking.send(RawJsonPayload(jsonObject.toString()))
    }


    private fun refreshFriendlyStatus() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        isFriendly = FriendlyBattleManager.getCachedSetting(player.uuid)

    }


    private fun toggleFriendly(uuid: UUID, newState: Boolean) {
        isFriendly = newState
        thread(name = "toggle-friendly-$uuid") {
            try {
                setFriendlyBattle(uuid, newState)
                FriendlyBattleManager.cacheSetting(uuid, newState)
            } catch (e: Exception) {
                println("Failed to update DB for friendly battle: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun requestFriendlyBattleStatus() {
        val jsonObject = buildJsonObject {
            put("type", "get_friendly_battle")
        }


        ClientPlayNetworking.send(RawJsonPayload(jsonObject.toString()))
    }


    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {

        RenderSystem.disableDepthTest()
        super.render(context, mouseX, mouseY, delta)



        renderPlayerInfo(context)

        GuiParticleManager.tick()
        GuiParticleManager.render(context)


    }

    private fun renderPlayerInfo(context: DrawContext) {


        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val username = player.gameProfile.name
        val currentElo = getPlayerElo(player.uuid)
        val tier = getTierForPlayer(currentElo)


        val rankText = "$tier: §c§l$currentElo§r"
        val nameText = "$username§r"

        var scale = 0.75f
        val renderer = client.textRenderer

        val centerX = (width - BASE_WIDTH) / 2
        val centerY = (height - BASE_HEIGHT) / 2
        val textX = centerX + BASE_WIDTH - 5
        val textY = centerY + 17

        val matrices = context.matrices
        matrices.push()

        matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        matrices.scale(scale, scale, 1.0f)

        context.drawTextWithShadow(
            renderer,
            Text.of(nameText),
            -renderer.getWidth(nameText),
            0,
            0xFFFFFF
        )

        context.drawTextWithShadow(
            renderer,
            Text.of(rankText),
            -renderer.getWidth(rankText),
            -10,
            0xFFFFFF
        )

        matrices.pop()


        matrices.push()
        val textGuiX = (width - BASE_WIDTH) / 2 + BASE_WIDTH - 343
        val textGuiY = (height - BASE_HEIGHT) / 2 + 53

        matrices.translate(textGuiX.toDouble(), textGuiY.toDouble(), 0.0)
        matrices.scale(scale, scale, 1.0f)

        CompetitiveHandbookTextGUI(
          0,0
        ).render(context)

        matrices.pop()


        scale = 0.60f
        matrices.push()

        val infoGuiX = centerX + 159
        val infoGuiY = centerY + 77
        matrices.translate(infoGuiX.toDouble(), infoGuiY.toDouble(), 0.0)
        matrices.scale(scale, scale, 1.0f)

        TournamentInfoTextGUI(
            0,
             0
        ).render(context)

        matrices.pop()


        val statusText = FriendlyBattleStatusTextGUI(
            x = centerX + 5,
            y = centerY + 12
        )
        statusText.render(context)

    }





    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {

        val inventoryKey = client?.options?.inventoryKey
        if (inventoryKey?.matchesKey(keyCode, scanCode) == true) {
           // MinecraftClient.getInstance().setScreen(null)
            this.close()
            return true
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }



    override fun close(){
        super.close()
        client?.soundManager?.play(
            PositionedSoundInstance.master(
                COMPETITIVE_GUI_CLOSE,
                1.0f,
                1.0f
            )
        )

        clearSignupCache()

    }


    /*private fun drawBackgroundTexture(context: DrawContext) {
        val centerX = (width - BASE_WIDTH) / 2
        val centerY = (height - BASE_HEIGHT) / 2

        RenderSystem.setShaderTexture(0, screenBackground)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        RenderSystem.disableDepthTest()

        drawCrispTexture(
            context,
            screenBackground,
            centerX,
            centerY,
            BASE_WIDTH,
            BASE_HEIGHT
        )
    }*/

    private fun drawMask(context: DrawContext) {
        val centerX = (width - BASE_WIDTH) / 2
        val centerY = (height - BASE_HEIGHT) / 2


        RenderSystem.setShaderTexture(0, screenBackground)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        drawCrispTexture(
            context,
            backgroundMask,
            centerX,
            centerY,
            BASE_WIDTH,
            BASE_HEIGHT
        )

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }


    fun forceEloRefresh(newElo: Int) {
        val uuid = MinecraftClient.getInstance().player?.uuid ?: return
        ClientEloStorage.setElo(uuid.toString(), newElo)

    }
}