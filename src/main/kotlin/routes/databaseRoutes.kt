package com.supermarket.routes

import com.supermarket.database.Product
import com.supermarket.database.refreshDatabase
import com.supermarket.database.refreshDatabaseJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.*

fun Route.testingRoutes() {
    put("/json-seed") {
        try {
            refreshDatabaseJson()
            call.respondText(
                "SUCCESS: Database wiped and re-seeded with data from the productData JSON file", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respondText("JSON seeding failed: ${e.message}", status = HttpStatusCode.BadRequest)

        }
    }

    put("/seed-db-random") {
        try {
            refreshDatabase()
            call.respondText("SUCCESS: Database wiped, rebuilt, and re-seeded with fresh data", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            call.respondText("ERROR: ${e.message}", status = HttpStatusCode.InternalServerError)
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


}