package com.supermarket

import com.supermarket.controllers.StaffSession
import com.supermarket.controllers.UserSession
import com.supermarket.database.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.sessions.*
import io.ktor.server.auth.*
import io.ktor.server.response.respondRedirect

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
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
            cookie.maxAgeInSeconds = 604800 // The session lasts for 7 days
        }

        // Staff cookie, separate from user cookie to allow for different session data (e.g. role) and different cookie settings (e.g. no maxAge so it expires on browser close)
        cookie<StaffSession>("staff_session") {
            cookie.path = "/" // Makes the cookie work on all routes

            // Leave out maxAge bc this guarantees the browser deletes the cookie upon closing
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
                call.respondRedirect("/management/staff/login")
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
                call.respondRedirect("/management/staff/login")
            }
        }
    }
}
