package com.supermarket

import com.supermarket.controllers.StaffSession
import com.supermarket.controllers.UserSession
import com.supermarket.database.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.response.respondRedirect
import io.ktor.util.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    // Create & connect to database
    DatabaseCreation.init()

    configureSerialization()

    // would normally be in a .env file, it is not right now for ease of testing
    val secretSignKey = hex("68656c6c6f20776f726c64206d7920736563726574206b657920313233343536")

    // Plugins (Sessions, Auth) must be installed BEFORE Routing
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 604800
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }

        cookie<StaffSession>("staff_session") {
            cookie.path = "/"
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }

    install(Authentication) {
        // Worker Auth
        session<StaffSession>("worker-auth") {
            validate { session ->
                // only allow workers in
                if (session.role == UserRole.WORKER.name) session else null
            }
            challenge {
                // If they fail the check, boot them to the login screen
                call.respondRedirect("/management/login")
            }
        }

        // Management auth
        session<StaffSession>("manager-auth") {
            validate { session ->
                // Allow Managers, Analysts, and Drivers in
                if (session.role in listOf(UserRole.MANAGER.name, UserRole.ANALYST.name, UserRole.DRIVER.name)) {
                    session
                } else {
                    null
                }
            }
            // If failed then go back to login page
            challenge {
                call.respondRedirect("/management/login")
            }
        }
    }

    // Routing depends on Authentication and Sessions being installed
    configureRouting()
}
