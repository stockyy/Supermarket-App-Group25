package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.customerRoutes() {
    route("/customers") {

        post("/register") {
            // registerCustomer
        }

        get("/login") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/login.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/register") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/register.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/forgotPassword") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/forgotPassword.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
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