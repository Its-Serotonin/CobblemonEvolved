package com.serotonin.common.networking

import com.google.gson.JsonParseException
import com.serotonin.common.saveslots.writeNbtCompat
import fr.harmex.cobbledollars.common.world.item.trading.shop.Offer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.DynamicRegistryManager
import java.math.BigInteger

object OfferSerializer : KSerializer<Offer> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Offer") {
        element<String>("itemNbt")
        element<String>("price")
    }

    lateinit var registryManager: DynamicRegistryManager // must be initialized before usage

    override fun serialize(encoder: Encoder, value: Offer) {
        val itemNbt = value.item.writeNbtCompat(registryManager)
        val itemNbtString = itemNbt.toString()
        val priceString = value.price.toString()

        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, itemNbtString)
        composite.encodeStringElement(descriptor, 1, priceString)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Offer {
        val dec = decoder.beginStructure(descriptor)
        var itemNbtString: String? = null
        var priceString: String? = null

        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> itemNbtString = dec.decodeStringElement(descriptor, 0)
                1 -> priceString = dec.decodeStringElement(descriptor, 1)
                else -> error("Unexpected index $i")
            }
        }

        dec.endStructure(descriptor)

        val nbt: NbtCompound = try {
            StringNbtReader.parse(itemNbtString!!)
        } catch (e: Exception) {
            throw JsonParseException("Failed to parse ItemStack NBT: $itemNbtString", e) as Throwable
        }

        val itemStack = ItemStack.fromNbt(nbt)
        val price = BigInteger(priceString!!)

        return Offer(itemStack, price)
    }
}