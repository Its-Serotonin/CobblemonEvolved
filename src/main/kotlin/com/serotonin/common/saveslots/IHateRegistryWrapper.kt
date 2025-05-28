package com.serotonin.common.saveslots

import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryWrapper
import java.util.Optional
import java.util.stream.Stream

val EMPTY_WRAPPER_LOOKUP = object : RegistryWrapper.WrapperLookup {
    override fun streamAllRegistryKeys(): Stream<RegistryKey<out Registry<*>?>?>? {
        return Stream.empty()
    }

    override fun <T> getOptionalWrapper(registryRef: RegistryKey<out Registry<out T>>): Optional<RegistryWrapper.Impl<T>> {
        return Optional.empty()
    }
}