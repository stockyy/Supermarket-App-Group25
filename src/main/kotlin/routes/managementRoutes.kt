package com.supermarket.routes

import io.ktor.http.*
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

            route("/create") {
                get {
                    val html = call.application.javaClass
                        .getResource("/static/views/management/create.html")
                        ?.readText()

                    if (html != null) {
                        call.respondText(html, ContentType.Text.Html)
                    } else {
                        call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                    }
                }
            }

            route("/login") {
                get {
                    val html = call.application.javaClass
                        .getResource("/static/views/management/login.html")
                        ?.readText()

                    if (html != null) {
                        call.respondText(html, ContentType.Text.Html)
                    } else {
                        call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                    }
                }
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