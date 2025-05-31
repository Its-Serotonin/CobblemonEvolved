package com.serotonin.common.networking



object ClientEloStorage {
    private val eloCache = mutableMapOf<String, Int>()
    private val listeners = mutableMapOf<String, MutableList<(Int) -> Unit>>()


    fun getElo(uuid: String): Int? {
        return eloCache[uuid]
    }

    fun setElo(uuid: String, elo: Int) {
        eloCache[uuid] = elo
        listeners[uuid]?.forEach { it(elo) }
        listeners.remove(uuid)
    }

    fun updateElo(uuid: String, elo: Int) {
        eloCache[uuid] = elo
    }

    fun onEloUpdate(uuid: String, callback: (Int) -> Unit) {
        listeners.getOrPut(uuid) { mutableListOf() }.add(callback)
    }

    fun clear() {
        eloCache.clear()
        listeners.clear()
        println("Cleared client Elo cache")
    }
}