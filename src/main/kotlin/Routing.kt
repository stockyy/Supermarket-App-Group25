package com.supermarket

import com.supermarket.database.OffsaleSummary
import com.supermarket.database.Product
import com.supermarket.database.ProductRepository
import com.supermarket.database.UserRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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

    route("/product/{id}") {
        get {
            val productId = call.parameters["id"]?.toIntOrNull()

            if (productId == null) {
                call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                return@get
            }

            val product = ProductRepository.getProductById(productId)

            if (product != null) {
                call.respond(HttpStatusCode.OK, product)
            }
            else {
                call.respondText("Product not found!", status = HttpStatusCode.NotFound)
            }
        }
    }

    route("/offsale/{id}") {
        // SHOULD BE PUT BECAUSE WE ARE MODIFYING DATA
        // GET GOT TESTING
        get {
            val productId = call.parameters["id"]?.toIntOrNull()

            if (productId == null) {
                call.respondText( "Invalid Product ID format", status=HttpStatusCode.BadRequest )
                return@get
            }

            val productBefore = ProductRepository.getProductById(productId)
            if (productBefore == null) {
                call.respond(HttpStatusCode.OK, "Product not found!")
                return@get
            }

            // Arbitrary UserId until login system is working
            val success = ProductRepository.createOffsaleLog(productId, userId = 6, potentialOffsale = false, managerReview = false)

            if (!success) {
                call.respond("Failed to update database")
                return@get
            }
            else {
                val productAfter = ProductRepository.getProductById(productId)
                val summary = OffsaleSummary(
                    productName = productBefore.name,
                    quantityBefore = productBefore.stockLevel,
                    quantityAfter = productAfter?.stockLevel ?: 67676767, //Only get stock level if not null, else 67676767
                    status = "Successfully marked offsale"
                )
                call.respond(HttpStatusCode.OK, summary)
                return@get
            }


        }
    }

    route("/print-offsale-logs") {
        get {
            val offsaleLogText = ProductRepository.getAllOffsaleLogsString()
            call.respondText(offsaleLogText, ContentType.Text.Plain)
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
