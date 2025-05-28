package  com.serotonin.common.networking


import com.cobblemon.mod.common.api.net.NetworkPacket
import fr.harmex.cobbledollars.common.world.item.trading.shop.Offer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.math.BigInteger
import java.util.*

@Serializable
data class SyncBankWithOffersPacket(
    @Contextual val merchantUUID: UUID,
    @Contextual val offers: List<Offer>,
    @Contextual override val id: Identifier
) : NetworkPacket<SyncBankWithOffersPacket> {

    companion object {
        val PACKET_ID = Identifier.of("cobblemonevolved", "sync_bank_offers")

        val CODEC: PacketCodec<RegistryByteBuf, SyncBankWithOffersPacket> =
            PacketCodec.of(
                { packet, buf ->
                    buf.writeUuid(packet.merchantUUID)
                    buf.writeVarInt(packet.offers.size)
                    for (offer in packet.offers) {
                        val nbt = offer.item.writeNbt(NbtCompound())
                        buf.writeNbt(nbt)
                        buf.writeBytes(offer.price.toByteArray())
                    }
                },
                { buf ->
                    val uuid = buf.readUuid()
                    val count = buf.readVarInt()
                    val offers = mutableListOf<Offer>()
                    repeat(count) {
                        val nbt = buf.readNbt() ?: NbtCompound()
                        val stack = ItemStack.fromNbt(nbt)
                        val priceBytes = ByteArray(16) // assumes fixed-size
                        buf.readBytes(priceBytes)
                        val price = BigInteger(priceBytes)
                        offers.add(Offer(stack, price))
                    }
                    SyncBankWithOffersPacket(uuid, offers, PACKET_ID)
                }
            )
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = CustomPayload.Id(PACKET_ID)

    override fun encode(buffer: RegistryByteBuf) {
        // if not using codec, manually encode here
    }
}