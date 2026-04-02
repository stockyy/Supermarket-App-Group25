package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.stockRoutes() {
    route("ß/stock") {

        get {

        }

        get("/low") {

        }

        post("/validate-basket") {

        }

        get("/movements") {

        }

        post("/movements") {

        }

        get("/{productId}") {

        }

        put("/{productId}/decrement") {

        }

        put("/{productId}/increment") {

        }

        post {

        }
    }
}