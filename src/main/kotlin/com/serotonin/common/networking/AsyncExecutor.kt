package com.serotonin.common.networking

import java.util.concurrent.Executors

object AsyncExecutor {
    val disconnectExecutor = Executors.newSingleThreadExecutor()
}