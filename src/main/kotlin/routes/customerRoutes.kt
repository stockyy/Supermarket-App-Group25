package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.customerRoutes() {
    route("/customers") {

        post("/register") {
            // registerCustomer
        }

        post("/login") {
            // loginCustomer
        }

        post("/logout") {
            // logoutCustomer
        }

        get("/session") {
            // validateSession
        }

        get {
            // getAllCustomers
        }

        get("/{id}") {

        }

        put("/{id}") {

        }

        put("/{id}/password") {

        }

        delete("/{id}") {

        }
    }
}