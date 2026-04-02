package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.managementRoutes() {
    route("/management") {

        get("/dashboard") {

        }

        get("/reports/sales") {

        }

        get("/reports/orders") {

        }

        get("/audit-log") {

        }


        route("/staff") {

            get {

            }

            post {

            }

            put("/{id}") {

            }

            delete("/{id}") {

            }
        }
    }
}