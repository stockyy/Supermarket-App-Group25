package com.supermarket.routes

import com.supermarket.database.StringRepository
import io.ktor.http.ContentType
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
}