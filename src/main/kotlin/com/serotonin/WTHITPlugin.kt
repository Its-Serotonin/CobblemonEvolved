package com.serotonin


import com.serotonin.common.entities.CustomNameTagRankEntity
import com.serotonin.common.entities.CustomRankedPlayerNameEntity
import com.serotonin.common.registries.EntityRegister
import mcp.mobius.waila.api.*
import mcp.mobius.waila.config.ConfigEntry
import mcp.mobius.waila.config.WailaConfig


class WTHITPlugin : IWailaPlugin {

    override fun register(registrar: IRegistrar) {
        val suppressor = object : IEntityComponentProvider {
            override fun appendHead(
                tooltip: ITooltip,
                accessor: IEntityAccessor,
                config: IPluginConfig

            ) {

            }

            override fun appendBody(
                tooltip: ITooltip,
                accessor: IEntityAccessor,
                config: IPluginConfig
            ) {

            }

            override fun appendTail(
                tooltip: ITooltip,
                accessor: IEntityAccessor,
                config: IPluginConfig
            ) {

            }

            override fun getIcon(
                accessor: IEntityAccessor,
                config: IPluginConfig
            ): ITooltipComponent? {
                return null
            }
        }

        listOf(
            CustomNameTagRankEntity::class.java,
            CustomRankedPlayerNameEntity::class.java
        ).forEach { entityClass ->
            registrar.addComponent(suppressor, TooltipPosition.HEAD, entityClass)
            registrar.addComponent(suppressor, TooltipPosition.BODY, entityClass)
            registrar.addComponent(suppressor, TooltipPosition.TAIL, entityClass)
            registrar.addBlacklist(EntityRegister.CUSTOM_NAME_TAG_RANK_ENTITY)
            registrar.addBlacklist(EntityRegister.CUSTOM_RANKED_PLAYER_NAME_TAG_ENTITY)
        }
    }
}