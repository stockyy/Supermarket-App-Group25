package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.warehouseRoutes() {
    route("/warehouse") {

        post("/deliveries") {

        }

        get("/deliveries") {

        }

        post("/picking-lists") {

        }

        get("/picking-lists/pending") {

        }

        put("/picking-lists/{id}/status") {

        }


        get("/inventory") {

        }

        put("/products/{productId}/location") {

        }

        post("/audit") {

        }
    }
}