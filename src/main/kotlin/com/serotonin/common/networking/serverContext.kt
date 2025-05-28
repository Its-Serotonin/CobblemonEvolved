package com.serotonin.common.networking

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer

object ServerContext {
    var server: MinecraftServer? = null


    fun registerServerLifecycleHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            println("Server started - storing reference")
            ServerContext.server = server
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            println("Server stopped - clearing reference")
            server = null
        }
    }
}
