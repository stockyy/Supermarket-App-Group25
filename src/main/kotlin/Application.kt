package com.supermarket

import com.supermarket.database.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Create & connect to database
    DatabaseCreation.init()
    seedDatabaseIfNeeded(false)

    configureSerialization()
    configureRouting()
}
