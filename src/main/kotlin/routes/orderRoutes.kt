package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    route("/orders") {

        route("/basket") {

            get {

            }

            post {

            }

            put("/{itemId}") {

            }

            delete("/{itemId}") {

            }

            delete {

            }
        }


        post {

        }

        get {

        }

        get("/delivery-windows") {

        }

        get("/{id}") {

        }

        put("/{id}/status") {

        }

        put("/{id}/cancel") {

        }

        post("/{id}/substitutions") {

        }

        get("/{id}/substitutions") {

        }

        put("/{id}/substitutions/{subId}") {

        }
    }
}