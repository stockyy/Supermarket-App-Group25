package com.supermarket.routes

import com.supermarket.database.StringRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.userRoutes() {
    route("/print-all-workers") {
        get {
            val userText = StringRepository.getAllWorkersString()
            call.respondText(userText, ContentType.Text.Html)
        }
    }

    route("/staff-login") {
        get {
            val html = call.application.javaClass
                .getResource("/static/views/customer/login.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }
    }




}