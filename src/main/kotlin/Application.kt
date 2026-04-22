package com.supermarket

import com.supermarket.controllers.UserSession
import com.supermarket.database.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Create & connect to database
    DatabaseCreation.init()

    configureSerialization()
    configureRouting()

    // Tell Ktor to turn on Sessions
    install(Sessions) {
        // Tell it to use a Cookie to store the session, and that the session data is stored in a UserSession object
        cookie<UserSession>("user_session") {
            cookie.path = "/" // Makes the cookie work on all pages
            cookie.maxAgeInSeconds = 604800 // The session lasts for 7 days (7 * 24 * 60 * 60)
        }
    }
}
