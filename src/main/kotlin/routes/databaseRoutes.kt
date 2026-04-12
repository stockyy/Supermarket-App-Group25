package com.supermarket.routes

import com.supermarket.database.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.*

fun Route.testingRoutes() {
    put("/seed-db") {
        try {
            refreshDatabase()
            call.respondText(
                "SUCCESS: Database wiped and re-seeded with data from the productData JSON file", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respondText("JSON seeding failed: ${e.message}", status = HttpStatusCode.BadRequest)

        }
    }

    get("/verify-database") {
        val testData = transaction {
            // Fetch the first 10 products and format them into a readable string
            Product.selectAll().limit(10).map {
                "Name: ${it[Product.name]} | Price: £${it[Product.price]} | Location: ${it[Product.location]}"
            }
        }

        if (testData.isEmpty()) {
            call.respondText("The database is empty! Seeding must have failed.")
        } else {
            // This will print the list directly to your web browser
            call.respond(testData)
        }
    }

    get("/print-all-users") {
        val userText = StringRepository.getAllUsersString()
        call.respondText(userText, ContentType.Text.Html)
    }

    get("/print-all-orders") {
        val orderText = StringRepository.getAllOrdersString()
        call.respondText(orderText, ContentType.Text.Html)
    }

    get("/print-all-carts") {
        val cartText = StringRepository.getAllCartsString()
        call.respondText(cartText, ContentType.Text.Html)
    }

    get("/view-all-products") {
        val productHtml = HtmlRepository.getAllProductsHtml()
        call.respondText(productHtml, ContentType.Text.Html)
    }
}