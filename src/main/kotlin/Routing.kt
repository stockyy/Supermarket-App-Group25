package com.supermarket

import com.supermarket.database.ProductRepository
import com.supermarket.database.UserRepository
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")

        productRouting()
        userRouting()
    }
}

fun Route.productRouting() {
    route("/print-all-products") {
        get {
            val productText = ProductRepository.getAllProductsString()
            call.respondText(productText, ContentType.Text.Plain)
        }
    }
}

fun Route.userRouting() {
    route("/print-all-workers") {
        get {
            val userText = UserRepository.getAllWorkersString()
            call.respondText(userText, ContentType.Text.Plain)
        }
    }
}
