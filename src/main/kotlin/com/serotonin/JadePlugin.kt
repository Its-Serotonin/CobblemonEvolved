package com.serotonin


import com.serotonin.common.entities.CustomNameTagRankEntity
import com.serotonin.common.entities.CustomRankedPlayerNameEntity
import com.serotonin.common.registries.EntityRegister
import net.minecraft.util.Identifier
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig


@WailaPlugin
class JadePlugin : IWailaPlugin {

    override fun register(registration: IWailaCommonRegistration) {
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        val suppressor = object : IComponentProvider<EntityAccessor> {
            override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
            }

            override fun getUid(): Identifier {
                return Identifier.of("cobblemonevolved", "suppress_tooltip")
            }
        }

        registration.registerEntityComponent(suppressor, CustomNameTagRankEntity::class.java)
        registration.registerEntityComponent(suppressor, CustomRankedPlayerNameEntity::class.java)
        registration.registerEntityIcon(suppressor, CustomNameTagRankEntity::class.java)
        registration.registerEntityIcon(suppressor, CustomRankedPlayerNameEntity::class.java)

        registration.hideTarget(EntityRegister.CUSTOM_NAME_TAG_RANK_ENTITY)
        registration.hideTarget(EntityRegister.CUSTOM_RANKED_PLAYER_NAME_TAG_ENTITY)
    }
}