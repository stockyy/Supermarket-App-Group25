package com.supermarket

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Create & connect to database
    DatabaseCreation.init()

    configureSerialization()
    configureRouting()
}
