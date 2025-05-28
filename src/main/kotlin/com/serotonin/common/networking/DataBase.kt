package com.serotonin.common.networking


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.minecraft.server.MinecraftServer
import java.io.File
import java.util.Properties
import java.util.UUID


object ModConfig {
    var jdbcUrl: String = ""
    var username: String = ""
    var password: String = ""

    fun load(server: MinecraftServer) {
        val configDir = server.runDirectory.resolve("config").resolve("cobblemonevolved")
        val configFile = configDir.resolve("database.properties").toFile()

        if (!configFile.exists()) {
            configFile.writeText(
                """
                jdbcUrl=
                username=
                password=
                """.trimIndent()
            )
            println("Generated default database.properties. Please edit with your DB info.")
        }

        val props = Properties().apply {
            configFile.reader().use { load(it) }
        }

        jdbcUrl = props.getProperty("jdbcUrl", "")
        username = props.getProperty("username", "")
        password = props.getProperty("password", "")
    }
}



object Database {
    val dataSource: HikariDataSource by lazy {
        val config = HikariConfig().apply {
            jdbcUrl = ModConfig.jdbcUrl
            username = ModConfig.username
            password = ModConfig.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 30
            connectionTimeout = 7000
            idleTimeout = 60000
            maxLifetime = 1800000
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        }
        HikariDataSource(config)
    }
}

