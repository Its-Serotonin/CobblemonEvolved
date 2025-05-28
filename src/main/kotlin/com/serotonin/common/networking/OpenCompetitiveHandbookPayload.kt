package com.serotonin.common.networking

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data object OpenCompetitiveHandbookPayload : CustomPayload {
    val ID = CustomPayload.id<OpenCompetitiveHandbookPayload>("cobblemonevolved_open_handbook")
    val CODEC = PacketCodec.unit<PacketByteBuf, OpenCompetitiveHandbookPayload>(OpenCompetitiveHandbookPayload)

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}